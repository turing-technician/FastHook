package pers.turing.technician.fasthookapp.hook;

import android.util.Log;

import pers.turing.technician.fasthookapp.MainApplication;

public class HookMethodInfo {

    public static void hookConstrctorTest(Object thiz, String message) {
        Log.e(MainApplication.TAG,"hook ConstrctorTest");
        message = "hook param-"+message;
        forwardConstrctorTest(thiz,message);
    }

    public native static void forwardConstrctorTest(Object thiz,String message);

    public static String hookDirectTest(Object thiz, String message) {
        Log.e(MainApplication.TAG,"hook DirectTest");
        message = "hook param-"+message;
        String result = forwardDirectTest(thiz,message);
        result = message+"-hook result";
        return result;
    }

    public native static String forwardDirectTest(Object thiz,String message);

    public static String hookVirtualTest(Object thiz,String message) {
        Log.e(MainApplication.TAG,"hook VirtualTest");
        message = "hook param-"+message;
        String result = forwardVirtualTest(thiz,message);
        result = message+"-hook result";
        return result;
    }

    public native static String forwardVirtualTest(Object thiz,String message);

    public static String hookStaticTest(String message) {
        Log.e(MainApplication.TAG,"hook StaticTest");
        message = "hook param-"+message;
        String result = forwardStaticTest(message);
        result = message+"-hook result";
        return result;
    }

    public native static String forwardStaticTest(String message);

    public static String hookNativeDirectTest(Object thiz,String message) {
        Log.e(MainApplication.TAG,"hook NativeDirectTest");
        message = "hook param-"+message;
        String result = forwardNativeDirectTest(thiz,message);
        result = message+"-hook result";
        return result;
    }

    public native static String forwardNativeDirectTest(Object thiz, String message);

    public static String hookNativeVirtualTest(Object thiz,String message) {
        Log.e(MainApplication.TAG,"hook NativeVirtualTest");
        message = "hook param-"+message;
        String result = forwardNativeVirtualTest(thiz,message);
        result = message+"]hook result";
        return result;
    }

    public native static String forwardNativeVirtualTest(Object thiz,String message);

    public static String hookNativeStaticTest(String message) {
        Log.e(MainApplication.TAG,"hook NativeStaticTest");
        message = "hook param-"+message;
        String result = forwardNativeStaticTest(message);
        result = message+"-hook result";
        return result;
    }

    public native static String forwardNativeStaticTest(String message);
}
