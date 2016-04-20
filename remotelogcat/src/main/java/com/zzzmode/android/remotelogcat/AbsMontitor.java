package com.zzzmode.android.remotelogcat;

import android.content.Context;

import org.json.JSONObject;

/**
 * Created by zl on 16/4/20.
 */
public abstract class AbsMontitor implements IMontitor {

    public AbsMontitor(){

    }

    protected OnNotifyObserver notifyObserver;
    protected Context context;

    public void attachServer(Context context,OnNotifyObserver observer){
        this.context=context;
        this.notifyObserver=observer;
    }


    protected void notify(JSONObject jsonObject){
        if(notifyObserver != null){
            notifyObserver.onChange(jsonObject);
        }
    }

}
