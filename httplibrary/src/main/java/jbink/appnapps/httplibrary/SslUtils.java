package jbink.appnapps.httplibrary;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
/**
 * Created by user on 2016-11-08.
 */
public class SslUtils {
    static void trustAllHosts()
    {
        TrustManager[] trustAllCerts = { new X509TrustManager()
        {
            public void checkClientTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException
            {}

            public void checkServerTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException
            {}

            public X509Certificate[] getAcceptedIssuers()
            {
                return new X509Certificate[0];
            }
        } };
        try
        {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    static final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier()
    {
        public boolean verify(String arg0, SSLSession arg1)
        {
            return true;
        }
    };
}

