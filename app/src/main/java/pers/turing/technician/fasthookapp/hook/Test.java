package pers.turing.technician.fasthookapp.hook;

import android.util.Log;

import pers.turing.technician.fasthookapp.MainApplication;

public class Test {

    public String mMessage;

    static {
        System.loadLibrary("native-lib");
    }

    public Test(String message) {
        Log.e(MainApplication.TAG,"invoke ConstrctorTest");
        mMessage = message;
    }

    public String testDirectMethod(String message) {
        return testDirect(message);
    }

    private String testDirect(String message) {
        Log.e(MainApplication.TAG,"invoke DirectTest");
        return message;
    }

    public String testVirtual(String message) {
        Log.e(MainApplication.TAG,"invoke VirtualTest");
        return message;
    }

    public static String testStatic(String message) {
        Log.e(MainApplication.TAG,"invoke StaticTest");
        return message;
    }

    public String testNativeDirectMethod(String message) {
        return testNativeDirect(message);
    }

    private native String testNativeDirect(String message);

    public String testNativeVirtualMethod(String message) {
        return testNativeVirtual(message);
    }

    public native String testNativeVirtual(String message);

    public String testNativeStaticMethod(String message) {
        return Test.testNativeStatic(message);
    }

    public static native String testNativeStatic(String message);
}
