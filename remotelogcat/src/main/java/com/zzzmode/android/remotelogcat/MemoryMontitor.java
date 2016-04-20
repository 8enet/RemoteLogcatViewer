package com.zzzmode.android.remotelogcat;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;
import android.os.Process;

import org.json.JSONObject;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by zl on 16/4/20.
 */
class MemoryMontitor extends AbsMontitor{

    private ScheduledExecutorService mExecutorService;
    private ActivityManager am;

    public MemoryMontitor(){
    }

    @Override
    public void attachServer(Context context, OnNotifyObserver observer) {
        super.attachServer(context, observer);

        if(context != null) {
            am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            mExecutorService = Executors.newScheduledThreadPool(1);
        }
    }

    @Override
    public int getInterval() {
        return 2;
    }

    @Override
    public void start() {
        mExecutorService.scheduleWithFixedDelay(this,0,getInterval(), TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        try {
            mExecutorService.shutdownNow();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = am.getRunningAppProcesses();
        if(runningAppProcesses != null){
            final int myuid=Process.myUid();
            int[] pids = new int[1];
            for (ActivityManager.RunningAppProcessInfo info:runningAppProcesses){
                try {
                    if(info.uid == myuid){
                        pids[0]=info.pid;
                        Debug.MemoryInfo[] memoryInfo = am.getProcessMemoryInfo(pids);
                        preData(info.processName,memoryInfo[0].dalvikPrivateDirty);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void preData(String processName,int size){
        try {
            JSONObject jsonObject =new JSONObject();
            jsonObject.put("key","memory");

            JSONObject data=new JSONObject();
            data.put("process_name",processName);
            data.put("memory_size",size);

            jsonObject.put("data",data);

            notify(jsonObject);
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
