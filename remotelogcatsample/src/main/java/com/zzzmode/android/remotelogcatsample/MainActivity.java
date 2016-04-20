package com.zzzmode.android.remotelogcatsample;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.zzzmode.android.remotelogcat.LogcatRunner;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    
    private boolean logRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            LogcatRunner.getInstance()
                    .config(LogcatRunner.LogConfig.builder()
                            .setWsCanReceiveMsg(false)
                            .write2File(true))
                    .with(getApplicationContext())
                    .start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startLog() {
        if (logRunning) {
            return;
        }
        logRunning = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                Random random = new Random();
                int i = 0;
                while (logRunning) {
                    if (random.nextBoolean()) {
                        Log.e("testlog", "run --> " + i);
                    } else {
                        Log.w("testlog", "run --> " + i);

                    }
//                    test();test();test();test();test();test();test();
//                    test();test();test();test();test();test();test();
                    SystemClock.sleep(random.nextInt(5000) + 100);
                    i++;
                }
            }
        }).start();

    }

    private static void test(){
        try {
            throw new RuntimeException("----");
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startLog();
    }

    @Override
    protected void onStop() {
        super.onStop();
        logRunning = false;
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogcatRunner.getInstance().stop();

    }
}
