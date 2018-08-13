package com.zzzmode.android.remotelogcatsample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import android.widget.TextView;
import com.zzzmode.android.remotelogcat.LogcatRunner;

import java.io.IOException;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    
    private boolean logRunning = false;
    private TextView textIP;
    private WifiReceiver wifiReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textIP = findViewById(R.id.textIP);

        wifiReceiver = new WifiReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(wifiReceiver, filter);

        try {
            LogcatRunner.getInstance()
                    .config(LogcatRunner.LogConfig.builder()
                            .setWsCanReceiveMsg(false)
                            .write2File(true))
                    .with(getApplicationContext(), false)
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
        unregisterReceiver(wifiReceiver);
        LogcatRunner.getInstance().stop();
        super.onDestroy();

    }


    public class WifiReceiver extends BroadcastReceiver {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(action)
                || WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(action)) {


                WifiManager wm = (WifiManager) context.getApplicationContext().getSystemService(context.WIFI_SERVICE);
                int ipAddressInt = wm.getConnectionInfo().getIpAddress();
                String ipAddress = String.format("%d.%d.%d.%d", (ipAddressInt & 0xff), (ipAddressInt >> 8 & 0xff), (ipAddressInt >> 16 & 0xff), (ipAddressInt >> 24 & 0xff));

                StringBuilder sbf = new StringBuilder();
                sbf.append("ws://")
                    .append(ipAddress)
                    .append(":")
                    .append(LogcatRunner.getInstance().getPort())
                    .append(LogcatRunner.getInstance().getWsPrefix());
                textIP.setText(sbf);

                Log.i("testlog", "" + sbf);
            }
        }
    }
}
