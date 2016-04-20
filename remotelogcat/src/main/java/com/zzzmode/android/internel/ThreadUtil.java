package com.zzzmode.android.internel;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by zl on 16/4/20.
 */
public class ThreadUtil {

    private static final Executor sExecutor= Executors.newCachedThreadPool();

    public static Executor getExecutor(){
        return sExecutor;
    }
}
