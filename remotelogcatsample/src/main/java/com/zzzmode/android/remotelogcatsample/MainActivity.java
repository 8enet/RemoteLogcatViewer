package com.zzzmode.android.remotelogcatsample;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.zzzmode.android.remotelogcat.LogcatRunner;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private boolean logRunning=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LogcatRunner.getInstance().bind(11220).start();
    }

    private void startLog(){
        if(logRunning){
            return;
        }
        logRunning=true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Random random=new Random();
                int i=0;
                while (logRunning){
                    if(random.nextBoolean()){
                        Log.e("testlog", "run --> "+i);
                    }else {
                        Log.w("testlog", "run --> "+i);
                    }
                    SystemClock.sleep(random.nextInt(3000)+100);
                    i++;
                }
            }
        }).start();

    }


    @Override
    protected void onStart() {
        super.onStart();
        startLog();
    }

    @Override
    protected void onStop() {
        super.onStop();
        logRunning=false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        LogcatRunner.getInstance().stop();

    }
}
