package com.zzzmode.android.remotelogcat;

import android.content.Context;

import org.json.JSONObject;

/**
 * Created by zl on 16/4/20.
 */
public interface IMontitor extends Runnable{

    int getInterval();

    void start();

    void stop();

    interface OnNotifyObserver{
        void onChange(JSONObject jsonObject);
    }
}
