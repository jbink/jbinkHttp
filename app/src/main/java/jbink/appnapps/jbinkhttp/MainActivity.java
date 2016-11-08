package jbink.appnapps.jbinkhttp;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.orhanobut.logger.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import jbink.appnapps.httplibrary.HttpHelper;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    private class testApi extends AsyncTask<Void, Void, String>{
        List<NameValuePair> param = new ArrayList<NameValuePair>();

        public testApi(List<NameValuePair> _param){
            param = _param;
        }

        @Override
        protected String doInBackground(Void... params) {
            String response = null;
            try {
                HttpHelper helper = new HttpHelper("ddd");
                helper.setParams(param);//post parameter
                helper.setReadTimeout(30);//30초의 timeOut
                response = helper.sendPost();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Logger.json(result);//com.orhanobut:Logger  Library를 이용한 json 로그 찍기
        }
    }

}
