package pers.turing.technician.fasthookapp;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import pers.turing.technician.fasthook.FastHookManager;

public class MainApplication extends Application {

    public final static String TAG = "FastHookManager";
    public static boolean mTest = false;
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
    }
}
