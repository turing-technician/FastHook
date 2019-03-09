package pers.turing.technician.fasthookapp.hook;

public class HookInfo2 {
    public static String MODE_REWRITE = "1";
    public static String MODE_RELACE = "2";
    public static String MODE = MODE_RELACE;
    private static String CLASS = "pers.turing.technician.fasthookapp.hook.Test";
    private static String HOOK_CLASS = "pers.turing.technician.fasthookapp.hook.HookMethodInfo";

    private static String[] mHookItem = {
            MODE,
            CLASS,"<init>","Ljava/lang/String;",
            HOOK_CLASS,"hookConstrctorTest","Ljava/lang/Object;Ljava/lang/String;",
            HOOK_CLASS,"forwardConstrctorTest","Ljava/lang/Object;Ljava/lang/String;"
    };

    private static String[] mHookItem2 = {
            MODE,
            CLASS,"testDirect","Ljava/lang/String;",
            HOOK_CLASS,"hookDirectTest","Ljava/lang/Object;Ljava/lang/String;",
            HOOK_CLASS,"forwardDirectTest","Ljava/lang/Object;Ljava/lang/String;"
    };

    private static String[] mHookItem3 = {
            MODE,
            CLASS,"testVirtual","Ljava/lang/String;",
            HOOK_CLASS,"hookVirtualTest","Ljava/lang/Object;Ljava/lang/String;",
            HOOK_CLASS,"forwardVirtualTest","Ljava/lang/Object;Ljava/lang/String;"
    };

    private static String[] mHookItem4 = {
            MODE,
            CLASS,"testStatic","Ljava/lang/String;",
            HOOK_CLASS,"hookStaticTest",";Ljava/lang/String;",
            HOOK_CLASS,"forwardStaticTest","Ljava/lang/String;"
    };

    private static String[] mHookItem5 = {
            MODE,
            CLASS,"testNativeDirect","Ljava/lang/String;",
            HOOK_CLASS,"hookNativeDirectTest","Ljava/lang/Object;Ljava/lang/String;",
            HOOK_CLASS,"forwardNativeDirectTest","Ljava/lang/Object;Ljava/lang/String;"
    };

    private static String[] mHookItem6 = {
            MODE,
            CLASS,"testNativeVirtual","Ljava/lang/String;",
            HOOK_CLASS,"hookNativeVirtualTest","Ljava/lang/Object;Ljava/lang/String;",
            HOOK_CLASS,"forwardNativeVirtualTest","Ljava/lang/Object;Ljava/lang/String;"
    };

    private static String[] mHookItem7 = {
            MODE,
            CLASS,"testNativeStatic","Ljava/lang/String;",
            HOOK_CLASS,"hookNativeStaticTest","Ljava/lang/String;",
            HOOK_CLASS,"forwardNativeStaticTest","Ljava/lang/String;"
    };

    public static String[][] HOOK_ITEMS = {
            mHookItem,
            mHookItem2,
            mHookItem3,
            mHookItem4,
            mHookItem5,
            mHookItem6,
            mHookItem7
    };
}
