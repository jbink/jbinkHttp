package jbink.appnapps.httplibrary;

import android.os.Build;
import android.os.Build.VERSION;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.NameValuePair;

public class HttpHelper {
    private final URL mTargetUrl;
    private ProgressMultipartCallback mUploadProgress;
    private ProgressCallback mProgessCallback;
    private List<NameValuePair> mParams = null;
    private Map<String, File> mFileParams = null;
    private int mReadTimeout = 20000;

    static {
    }

    private static String encodeString(List<NameValuePair> params)
            throws UnsupportedEncodingException {
        StringBuilder sb = new StringBuilder(256);
        Iterator<NameValuePair> iterator = params.iterator();
        while (iterator.hasNext()) {
            NameValuePair element = (NameValuePair) iterator.next();
            String value = element.getValue();
            if (value == null) {
                value = "";
            }
            sb.append(URLEncoder.encode(element.getName(), Charset.defaultCharset().displayName()) + "=" + URLEncoder.encode(value, Charset.defaultCharset().displayName()));
            if (iterator.hasNext()) {
                sb.append("&");
            }
        }
        return sb.toString();
    }

    public static String getEncodedParam(List<NameValuePair> params)
            throws UnsupportedEncodingException {
        return "?" + encodeString(params);
    }

    public HttpHelper(String targetUrl)
            throws MalformedURLException {
        this.mTargetUrl = new URL(targetUrl);
    }

    private HttpURLConnection getConnection(URL targetUrl)
            throws IOException {
        HttpURLConnection conn = null;
        if (targetUrl.getProtocol().toLowerCase(Locale.getDefault()).equals("https")) {
            SslUtils.trustAllHosts();

            conn = (HttpsURLConnection) targetUrl.openConnection();
            ((HttpsURLConnection) conn).setHostnameVerifier(SslUtils.DO_NOT_VERIFY);
        } else {
            conn = (HttpURLConnection) targetUrl.openConnection();
        }
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(this.mReadTimeout);
        conn.setUseCaches(false);
        return conn;
    }

    public static boolean isUTF8(byte[] sequence) {
        for (int i = 0; i < sequence.length; i++) {
            byte b = sequence[i];
            if ((b >> 6 & 0x3) == 2) {
                return false;
            }
            byte test = b;
            int numberBytesInChar = 0;
            while ((test & 0x80) > 0) {
                test = (byte) (test << 1);
                numberBytesInChar++;
            }
            if (numberBytesInChar > 1) {
                for (int j = 1; j < numberBytesInChar; j++) {
                    if (i + j >= sequence.length) {
                        return false;
                    }
                    if ((sequence[(i + j)] >> 6 & 0x3) != 2) {
                        return false;
                    }
                }
                i += numberBytesInChar - 1;
            }
        }
        return true;
    }

    public byte[] getStreamDataBytes(HttpURLConnection conn)
            throws IOException {
        InputStream is = conn.getInputStream();

        ByteBuffer buffer = ByteBuffer.allocate(2048);
        ReadableByteChannel inChannel = Channels.newChannel(is);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        WritableByteChannel outChannel = Channels.newChannel(os);

        long contentSize = conn.getContentLength();
        long totalRead = 0L;
        int amountRead = 0;
        try {
            while ((amountRead = inChannel.read(buffer)) != -1) {
                buffer.flip();
                totalRead += amountRead;
                if (this.mProgessCallback != null) {
                    this.mProgessCallback.setProgress((int) (100L * totalRead / contentSize));
                }
                while (buffer.hasRemaining()) {
                    outChannel.write(buffer);
                }
                buffer.clear();
            }
        } finally {
            closeQuietly(inChannel);
            closeQuietly(outChannel);
            closeQuietly(is);
        }
        try {
            return os.toByteArray();
        } finally {
            closeQuietly(os);
        }
    }

    private String getStreamData(HttpURLConnection conn)
            throws IOException {
        if (conn == null) {
            return null;
        }
        byte[] data = getStreamDataBytes(conn);
        if (data == null) {
            return null;
        }
        if (isUTF8(data)) {
            return new String(data);
        }
        try {
            return new String(data, "EUC-KR");
        } catch (UnsupportedEncodingException e) {
        }
        return new String(data);
    }

    public String sendGet()
            throws UnsupportedEncodingException, IOException {
        HttpURLConnection connection = null;
        try {
            connection = sendGetMessage();
            return getStreamData(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public InputStream sendGetByte()
            throws IOException {
        return sendGetMessage().getInputStream();
    }

    private HttpURLConnection sendGetMessage()
            throws MalformedURLException, IOException {
        String paramString = null;
        if (this.mParams != null) {
            paramString = getEncodedParam(this.mParams);
        } else {
            paramString = "";
        }
        return getConnection(new URL(this.mTargetUrl.toExternalForm() + paramString));
    }

    public void setParams(List<NameValuePair> params) {
        this.mParams = params;
    }

    public void setFileParams(Map<String, File> fileParams) {
        this.mFileParams = fileParams;
    }

    public void setMultipartProgressCallback(ProgressMultipartCallback progress) {
        this.mUploadProgress = progress;
    }

    public String sendPost()
            throws MalformedURLException, IOException {
        HttpURLConnection connection = null;
        try {
            connection = sendPostMessage();
            return getStreamData(connection);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public HttpURLConnection sendPostConnection()
            throws MalformedURLException, IOException {
        return sendPostMessage();
    }

    public InputStream sendPostByte()
            throws MalformedURLException, IOException {
        return sendPostMessage().getInputStream();
    }

    private HttpURLConnection sendPostMessage()
            throws IOException {
        HttpURLConnection conn = getConnection(this.mTargetUrl);

        conn.setDoInput(true);
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");

        DataOutputStream dos = null;
        BufferedInputStream inputStream = null;
        try {
            if ((this.mFileParams != null) && (this.mFileParams.size() > 0)) {
                conn.setChunkedStreamingMode(2048);

                String twoHyphens = "--";
                String boundary = "*****";
                String lineEnd = "\r\n";

                conn.setRequestProperty("Content-Type", "multipart/form-data; charset=UTF-8; boundary=*****");

                dos = new DataOutputStream(conn.getOutputStream());
                StringBuilder contentBuiler;
                if ((this.mParams != null) && (this.mParams.size() > 0)) {
                    for (NameValuePair pair : this.mParams) {
                        contentBuiler = new StringBuilder();

                        contentBuiler.append("--*****\r\n");
                        contentBuiler.append("Content-Disposition: form-data; name=\"" + pair.getName() + "\"");
                        contentBuiler.append("\r\n");
                        contentBuiler.append("\r\n");
                        contentBuiler.append(pair.getValue());
                        contentBuiler.append("\r\n");

                        dos.write(contentBuiler.toString().getBytes("UTF-8"));
                    }
                }
                int cnt = 0;
                for (Object param : this.mFileParams.entrySet()) {
                    File file = (File) ((Map.Entry) param).getValue();
                    String fileName = file.getName();


                    contentBuiler = new StringBuilder();

                    contentBuiler.append("--*****\r\n");
                    contentBuiler.append("Content-Disposition: form-data; name=\"" + (String) ((Map.Entry) param).getKey() + "\"; filename=\"" + fileName + "\"");
                    contentBuiler.append("\r\n");
                    contentBuiler.append("Content-Type: " + getContentType(fileName));
                    contentBuiler.append("\r\n");
                    contentBuiler.append("\r\n");

                    dos.write(contentBuiler.toString().getBytes("UTF-8"));

                    inputStream = new BufferedInputStream(new FileInputStream(file));

                    byte[] buf = new byte[2048];

                    int readByteCounter = 0;
                    int totalReadByteCounter = 0;
                    while ((readByteCounter = inputStream.read(buf)) != -1) {
                        dos.write(buf, 0, readByteCounter);
                        totalReadByteCounter += readByteCounter;
                        if (this.mUploadProgress != null) {
                            this.mUploadProgress.setProgress(cnt + 1L, this.mFileParams.size(), totalReadByteCounter, file.length());
                        }
                    }
                    dos.writeBytes("\r\n");


                    cnt++;
                }
                dos.writeBytes("--*****--\r\n");
            } else {
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(encodeString(this.mParams));
            }
            dos.flush();
        } finally {
            closeQuietly(inputStream);
            closeQuietly(dos);
        }
        return conn;
    }

    private static void disableConnectionReuseIfNecessary() {
        if (Build.VERSION.SDK_INT < 8) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    public static String getContentType(String fileName) {
        String extension = fileName.substring(fileName.lastIndexOf("."));
        String mimeTypeMap = MimeTypeMap.getFileExtensionFromUrl(extension);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mimeTypeMap);
        if (TextUtils.isEmpty(mimeType)) {
            mimeType = "application/octet-stream";
        }
        return mimeType;
    }

    public void setReadTimeout(int value) {
        this.mReadTimeout = value;
    }


    public void closeQuietly(final Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (final IOException ioe) {
            // ignore
        }
    }
}
