package pers.turing.technician.fasthook;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.StringBuilder;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class FastHookManager {
    private static final String TAG = "FastHookManager";
    private static final boolean DEBUG = BuildConfig.DEBUG;

    private final static int ANDROID_P = 28;
    private final static int ANDROID_O_MR1 = 27;
    private final static int ANDROID_O = 26;
    private final static int ANDROID_N_MR1 = 25;
    private final static int ANDROID_N = 24;
    private final static int ANDROID_M = 23;
    private final static int ANDROID_L_MR1 = 22;
    private final static int ANDROID_L = 21;

    public final static int MODE_REWRITE = 1;
    public final static int MODE_REPLACE = 2;

    private final static int TYPE_RECORD_REWRITE = 1;
    private final static int TYPE_RECORD_REWRITE_HEAD = 2;
    private final static int TYPE_RECORD_REWRITE_TAIL = 3;
    private final static int TYPE_RECORD_REPLACE = 4;

    private final static int JIT_NONE = 0;
    private final static int JIT_COMPILE = 1;
    private final static int JIT_COMPILING = 2;
    private final static int JIT_COMPILINGORFAILED = 3;

    private final static int MESSAGE_RETRY = 1;

    private final static int RETRY_TIME_WAIT = 1000;
    private final static int RETRY_LIMIT = 1;

    private final static int CORE_REG_ARGS_SIZE = 4;
    private final static int FP_REG_ARGS_SIZE = 16;
    private final static int TYPE_SP = 0;
    private final static int TYPE_CORE_REG_ARGS = 1;
    private final static int TYPE_FP_REG_ARGES = 2;

    private final static String HOOK_LIB = "fasthook";

    private final static String CONSTRUCTOR = "<init>";

    private static int mProxyCount;
    private final static String PROXY_CLASS_NAME = "pers.turing.technician.fasthook.proxy";
    private final static String GENERATEPROXY = "generateProxy";

    private static HashMap<Member,HookRecord> mHookMap;
    private static HashMap<Long,ArrayList<HookRecord>> mQuickTrampolineMap;
    private static HashMap<Member,HookInfo> mHookInfoMap;
    private static Handler mHandler;
    private static long mThread;

    static{
        System.loadLibrary(HOOK_LIB);
        mHookMap = new HashMap<Member,HookRecord>();
        mQuickTrampolineMap = new HashMap<Long,ArrayList<HookRecord>>();
        mHookInfoMap = new HashMap<Member,HookInfo>();
        mHandler = new HookHandler();
        init(Build.VERSION.SDK_INT);
        if(Build.VERSION.SDK_INT >= ANDROID_P) {
            disableHiddenApiCheck();
        }
        Logd("Init");
    }

    public static void doHook(String className, ClassLoader classLoader, String methodName, String methodSig, FastHookCallback callback, int mode, boolean jitInline) {
        if(className == null || methodName == null || methodSig == null || callback == null) {
            throwException(new FastHookException("invalid param"));
            return;
        }

        Class[] paramType = getParamType(methodSig,classLoader);

        try {
            Class targetClass = null;
            if(classLoader != null) {
                targetClass = Class.forName(className,true,classLoader);
            }else {
                targetClass = Class.forName(className);
            }

            if(targetClass == null) {
                throwException(new FastHookException("targetClass is null!"));
                return;
            }

            Member targetMethod = null;
            if(methodName.equals(CONSTRUCTOR)) {
                targetMethod = targetClass.getDeclaredConstructor(paramType);
            }else {
                targetMethod = targetClass.getDeclaredMethod(methodName,paramType);
            }

            if(targetMethod == null) {
                throwException(new FastHookException("targetMethod is null!"));
                return;
            }

            HookRecord hookRecord = mHookMap.get(targetMethod);
            if(hookRecord != null) {
                throwException(new FastHookException("already hook"));
                return;
            }

            Member hookMethod = getHookHandle(targetMethod);
            if(hookMethod == null) {
                throwException(new FastHookException("hookMethod is null!"));
                return;
            }

            Member forwardMethod = generateForwardMethod(targetMethod,FastHookManager.class.getClassLoader(),paramType);
            if(forwardMethod == null) {
                throwException(new FastHookException("forwardMethod is null!"));
                return;
            }

            doHook(targetMethod,hookMethod,forwardMethod,paramType,callback,mode,0);

            if(!jitInline  && Build.VERSION.SDK_INT >= ANDROID_N) {
                disableJITInline();
            }
        }catch (Exception e) {
            throwException(e);
        }
    }

    private static void doHook(Member targetMethod, Member hookMethod, Member forwardMethod, Class[] paramType, FastHookCallback callback, int mode, int retryCount) {
        boolean isStatic = false;
        if(isStaticMethod(targetMethod)) {
            isStatic = true;
        }

        boolean isNative = false;
        if(isNativeMethod(targetMethod)) {
            mode = MODE_REPLACE;
            isNative = true;
            Logd("do replace hook for native method");
        }

        if(mode == MODE_REPLACE && Build.VERSION.SDK_INT == ANDROID_L && !isNative) {
            mode = MODE_REWRITE;
        }

        switch (mode) {
            case MODE_REWRITE:
                long entryPoint = getMethodEntryPoint(targetMethod);
                Logd("EntryPoint:0x"+Long.toHexString(entryPoint));

                ArrayList<HookRecord> quickTrampolineList = mQuickTrampolineMap.get(Long.valueOf(entryPoint));
                if(quickTrampolineList != null) {
                    int i = 0;
                    for(HookRecord record:quickTrampolineList) {
                        Logd("record["+i+"]:"+record);
                    }

                    HookRecord tailRecord = quickTrampolineList.get(quickTrampolineList.size() - 1);
                    HookRecord prevRecord = quickTrampolineList.get(quickTrampolineList.size() - 2);
                    long quickOriginalTrampoline =  tailRecord.mQuickOriginalTrampoline;
                    long prevQuickHookTrampoline = prevRecord.mQuickHookTrampoline;
                    HookRecord targetRecord = new HookRecord(TYPE_RECORD_REWRITE, targetMethod, hookMethod, forwardMethod, 0, 0, 1, quickTrampolineList);

                    doPartRewriteHook(targetMethod,hookMethod,forwardMethod,quickOriginalTrampoline,prevQuickHookTrampoline,targetRecord);
                    Logd("QuickHookTrampoline:0x"+Long.toHexString(targetRecord.mQuickHookTrampoline)+" QuickTargetTrampoline:0x"+Long.toHexString(targetRecord.mQuickTargetTrampoline));

                    quickTrampolineList.add(quickTrampolineList.size() - 1,targetRecord);
                    targetRecord.index = quickTrampolineList.size() - 2;
                    tailRecord.index = quickTrampolineList.size() - 1;
                    mHookMap.put(targetMethod,targetRecord);

                    HookInfo hookInfo = new HookInfo(forwardMethod,callback,paramType,isStatic);
                    mHookInfoMap.put(targetMethod,hookInfo);
                }else {
                    if(Build.VERSION.SDK_INT < ANDROID_N) {
                        boolean success = isCompiled(targetMethod);
                        if(success) {
                            success = doRewriteHookCheck(targetMethod);
                        }

                        if(success) {
                            doFullRewriteHookInternal(targetMethod,hookMethod,forwardMethod);

                            HookInfo hookInfo = new HookInfo(forwardMethod,callback,paramType,isStatic);
                            mHookInfoMap.put(targetMethod,hookInfo);
                        }else {
                            if(Build.VERSION.SDK_INT == ANDROID_L) {
                                throwException(new FastHookException("hook failed!"));
                                return;
                            }
                            doReplaceHookInternal(targetMethod,hookMethod,forwardMethod,isNative);

                            HookInfo hookInfo = new HookInfo(forwardMethod,callback,paramType,isStatic);
                            mHookInfoMap.put(targetMethod,hookInfo);
                        }
                    }else {
                        int jitState = checkJitState(targetMethod);
                        Logd("jitState:"+jitState);

                        switch (jitState) {
                            case JIT_COMPILING:
                            case JIT_COMPILINGORFAILED:
                                if (retryCount < RETRY_LIMIT) {
                                    int newCount = retryCount + 1;
                                    sendRetryMessage(targetMethod,hookMethod,forwardMethod,paramType,callback,mode,newCount);
                                    break;
                                }
                            case JIT_NONE:
                            case JIT_COMPILE:
                                boolean success = true;
                                boolean needCompile = !isCompiled(targetMethod);

                                if(jitState == JIT_NONE && needCompile) {
                                    success = compileMethod(targetMethod);

                                    if(success) {
                                        success = doRewriteHookCheck(targetMethod);
                                    }
                                }else if(jitState == JIT_COMPILE) {
                                    success = doRewriteHookCheck(targetMethod);
                                }else if(jitState == JIT_COMPILINGORFAILED) {
                                    success = false;
                                }

                                if(success) {
                                    doFullRewriteHookInternal(targetMethod,hookMethod,forwardMethod);

                                    HookInfo hookInfo = new HookInfo(forwardMethod,callback,paramType,isStatic);
                                    mHookInfoMap.put(targetMethod,hookInfo);
                                }else {
                                    if(Build.VERSION.SDK_INT == ANDROID_L) {
                                        throwException(new FastHookException("hook failed!"));
                                        return;
                                    }
                                    if(Build.VERSION.SDK_INT >= ANDROID_O && BuildConfig.DEBUG) {
                                        setNativeMethod(targetMethod);
                                        Logd("set target method to native on debug mode");
                                    }
                                    doReplaceHookInternal(targetMethod,hookMethod,forwardMethod,isNative);

                                    HookInfo hookInfo = new HookInfo(forwardMethod,callback,paramType,isStatic);
                                    mHookInfoMap.put(targetMethod,hookInfo);
                                }
                                break;
                        }
                    }
                }
                break;
            case MODE_REPLACE:
                if(Build.VERSION.SDK_INT < ANDROID_N) {
                    doReplaceHookInternal(targetMethod,hookMethod,forwardMethod,isNative);
                    HookInfo hookInfo = new HookInfo(forwardMethod,callback,paramType,isStatic);
                    mHookInfoMap.put(targetMethod,hookInfo);
                }else {
                    int jitState = checkJitState(targetMethod);
                    Logd("jitState:"+jitState);

                    switch (jitState) {
                        case JIT_COMPILING:
                        case JIT_COMPILINGORFAILED:
                            if (retryCount < RETRY_LIMIT) {
                                int newCount = retryCount + 1;
                                sendRetryMessage(targetMethod,hookMethod,forwardMethod,paramType,callback,mode,newCount);
                                break;
                            }
                        case JIT_NONE:
                        case JIT_COMPILE:
                            if(Build.VERSION.SDK_INT >= ANDROID_O && BuildConfig.DEBUG) {
                                setNativeMethod(targetMethod);
                                Logd("set target method to native on debug mode");
                            }
                            doReplaceHookInternal(targetMethod,hookMethod,forwardMethod,isNative);
                            HookInfo hookInfo = new HookInfo(forwardMethod,callback,paramType,isStatic);
                            mHookInfoMap.put(targetMethod,hookInfo);
                            break;
                    }
                }
                break;
        }
    }

    private static FastHookParam hookHandle(long targetArtMethod, long sp) {
        Member targetMethod = getReflectedMethod(targetArtMethod);

        HookInfo hookInfo = mHookInfoMap.get(targetMethod);
        FastHookCallback callback = hookInfo.mCallback;
        FastHookParam param = parseParam(sp,hookInfo.mParamType,hookInfo.mIsStatic);

        callback.beforeHookedMethod(param);
        if(param.replace) {
            return param;
        }

        Method forwardMethod = (Method) hookInfo.mForwardMethod;
        forwardMethod.setAccessible(true);

        try {
            param.result = forwardMethod.invoke(param.receiver,param.args);
            callback.afterHookedMethod(param);
        }catch (Exception e) {
            throwException(new FastHookException(e));
        }

        return param;
    }

    private static FastHookParam hookHandle(int targetArtMethod, int sp, int r11) {
        Member targetMethod = getReflectedMethod(targetArtMethod);

        HookInfo hookInfo = mHookInfoMap.get(targetMethod);
        FastHookCallback callback = hookInfo.mCallback;
        FastHookParam param = parseParam(sp,r11,hookInfo.mParamType,hookInfo.mIsStatic);

        callback.beforeHookedMethod(param);
        if(param.replace) {
            return param;
        }

        Method forwardMethod = (Method) hookInfo.mForwardMethod;
        forwardMethod.setAccessible(true);

        try {
            param.result = forwardMethod.invoke(param.receiver,param.args);
            callback.afterHookedMethod(param);
        }catch (Exception e) {
            throwException(new FastHookException(e));
        }

        return param;
    }

    private static Object hookHandleObject(int targetArtMethod, int sp, int r11) {
        FastHookParam param = hookHandle(targetArtMethod,sp,r11);
        return param.result;
    }

    private static boolean hookHandleBoolean(int targetArtMethod, int sp, int r11) {
        FastHookParam param = hookHandle(targetArtMethod,sp,r11);
        if(param.result != null && param.result instanceof Boolean) {
            return ((Boolean) param.result).booleanValue();
        }
        return false;
    }

    private static byte hookHandleByte(int targetArtMethod, int sp, int r11) {
        FastHookParam param = hookHandle(targetArtMethod,sp,r11);
        if(param.result != null && param.result instanceof Byte) {
            return ((Byte) param.result).byteValue();
        }
        return 0;
    }

    private static char hookHandleChar(int targetArtMethod, int sp, int r11) {
        FastHookParam param = hookHandle(targetArtMethod,sp,r11);
        if(param.result != null && param.result instanceof Character) {
            return ((Character) param.result).charValue();
        }
        return 0;
    }

    private static short hookHandleShort(int targetArtMethod, int sp, int r11) {
        FastHookParam param = hookHandle(targetArtMethod,sp,r11);
        if(param.result != null && param.result instanceof Short) {
            return ((Short) param.result).shortValue();
        }
        return 0;
    }

    private static int hookHandleInt(int targetArtMethod, int sp, int r11) {
        FastHookParam param = hookHandle(targetArtMethod,sp,r11);
        if(param.result != null && param.result instanceof Integer) {
            return ((Integer) param.result).intValue();
        }
        return 0;
    }

    private static long hookHandleLong(int targetArtMethod, int sp, int r11) {
        FastHookParam param = hookHandle(targetArtMethod,sp,r11);
        if(param.result != null && param.result instanceof Long) {
            return ((Long) param.result).longValue();
        }
        return 0;
    }

    private static float hookHandleFloat(int targetArtMethod, int sp, int r11) {
        FastHookParam param = hookHandle(targetArtMethod,sp,r11);
        if(param.result != null && param.result instanceof Float) {
            return ((Float) param.result).floatValue();
        }
        return 0;
    }

    private static double hookHandleDouble(int targetArtMethod, int sp, int r11) {
        FastHookParam param = hookHandle(targetArtMethod,sp,r11);
        if(param.result != null && param.result instanceof Double) {
            return ((Double) param.result).doubleValue();
        }
        return 0;
    }

    private static void hookHandleVoid(int targetArtMethod, int sp, int r11) {
        hookHandle(targetArtMethod,sp,r11);
        return;
    }

    private static Object hookHandleObject(long targetArtMethod, long sp) {
        FastHookParam param = hookHandle(targetArtMethod,sp);
        return param.result;
    }

    private static boolean hookHandleBoolean(long targetArtMethod, long sp) {
        FastHookParam param = hookHandle(targetArtMethod,sp);
        if(param.result != null && param.result instanceof Boolean) {
            return ((Boolean) param.result).booleanValue();
        }
        return false;
    }

    private static byte hookHandleByte(long targetArtMethod, long sp) {
        FastHookParam param = hookHandle(targetArtMethod,sp);
        if(param.result != null && param.result instanceof Byte) {
            return ((Byte) param.result).byteValue();
        }
        return 0;
    }

    private static char hookHandleChar(long targetArtMethod, long sp) {
        FastHookParam param = hookHandle(targetArtMethod,sp);
        if(param.result != null && param.result instanceof Character) {
            return ((Character) param.result).charValue();
        }
        return 0;
    }

    private static short hookHandleShort(long targetArtMethod, long sp) {
        FastHookParam param = hookHandle(targetArtMethod,sp);
        if(param.result != null && param.result instanceof Short) {
            return ((Short) param.result).shortValue();
        }
        return 0;
    }

    private static int hookHandleInt(long targetArtMethod, long sp) {
        FastHookParam param = hookHandle(targetArtMethod,sp);
        if(param.result != null && param.result instanceof Integer) {
            return ((Integer) param.result).intValue();
        }
        return 0;
    }

    private static long hookHandleLong(long targetArtMethod, long sp) {
        FastHookParam param = hookHandle(targetArtMethod,sp);
        if(param.result != null && param.result instanceof Long) {
            return ((Long) param.result).longValue();
        }
        return 0;
    }

    private static float hookHandleFloat(long targetArtMethod, long sp) {
        FastHookParam param = hookHandle(targetArtMethod,sp);
        if(param.result != null && param.result instanceof Float) {
            return ((Float) param.result).floatValue();
        }
        return 0;
    }

    private static double hookHandleDouble(long targetArtMethod, long sp) {
        FastHookParam param = hookHandle(targetArtMethod,sp);
        if(param.result != null && param.result instanceof Double) {
            return ((Double) param.result).doubleValue();
        }
        return 0;
    }

    private static void hookHandleVoid(long targetArtMethod, long sp) {
        hookHandle(targetArtMethod,sp);
        return;
    }

    private static Member getHookHandle(Member targetMethod) {
        if(targetMethod instanceof Constructor) {
            return getMethod("hookHandleVoid");
        }else if(targetMethod instanceof Method) {
            Class returnType = ((Method)targetMethod).getReturnType();
            if(returnType == boolean.class) {
                return getMethod("hookHandleBoolean");
            }else if(returnType == byte.class) {
                return getMethod("hookHandleByte");
            }else if(returnType == char.class) {
                return getMethod("hookHandleChar");
            }else if(returnType == short.class) {
                return getMethod("hookHandleShort");
            }else if(returnType == int.class) {
                return getMethod("hookHandleInt");
            }else if(returnType == long.class) {
                return getMethod("hookHandleLong");
            }else if(returnType == float.class) {
                return getMethod("hookHandleFloat");
            }else if(returnType == double.class) {
                return getMethod("hookHandleDouble");
            }else if(returnType == void.class) {
                return getMethod("hookHandleVoid");
            }else {
                return getMethod("hookHandleObject");
            }
        }

        return null;
    }

    private static Member getMethod(String methodName) {
        Member method = null;

        try {
            if(is32bit()) {
                method = FastHookManager.class.getDeclaredMethod(methodName,int.class,int.class,int.class);
            }else {
                method = FastHookManager.class.getDeclaredMethod(methodName,long.class,long.class);
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        return method;
    }

    private static Class[] getParamType(String paramSig, ClassLoader targetClassLoader) {
        Class[] params = null;

        int index = 0;
        ArrayList<Class> paramsArray = new ArrayList<Class>();
        while(index < paramSig.length()) {
            switch (paramSig.charAt(index)) {
                case 'B': // byte
                    paramsArray.add(byte.class);
                    break;
                case 'C': // char
                    paramsArray.add(char.class);
                    break;
                case 'D': // double
                    paramsArray.add(double.class);
                    break;
                case 'F': // float
                    paramsArray.add(float.class);
                    break;
                case 'I': // int
                    paramsArray.add(int.class);
                    break;
                case 'J': // long
                    paramsArray.add(long.class);
                    break;
                case 'S': // short
                    paramsArray.add(short.class);
                    break;
                case 'Z': // boolean
                    paramsArray.add(boolean.class);
                    break;
                case 'L':
                    try {
                        String objectClass = getObjectClass(index,paramSig);

                        if(targetClassLoader != null) {
                            paramsArray.add(Class.forName(objectClass,true,targetClassLoader));
                        }else {
                            paramsArray.add(Class.forName(objectClass));
                        }

                        index += objectClass.length() + 1;
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
                case '[':
                    try {
                        String arrayClass = getArrayClass(index,paramSig);

                        if(targetClassLoader != null) {
                            paramsArray.add(Class.forName(arrayClass,true,targetClassLoader));
                        }else {
                            paramsArray.add(Class.forName(arrayClass));
                        }

                        index += arrayClass.length() - 1;
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                    break;
            }
            index++;
        }

        if(!paramsArray.isEmpty()) {
            params = new Class[paramsArray.size()];
            for(int i = 0; i < paramsArray.size(); i++) {
                params[i] = paramsArray.get(i);
            }
        }

        return params;
    }

    private static String getObjectClass(int index, String paramSig) {
        String objectClass = null;

        String subParam = paramSig.substring(index + 1);
        objectClass = subParam.split(";")[0].replace('/','.');

        return objectClass;
    }

    private static String getArrayClass(int index, String paramSig) {
        int count = 0;
        StringBuilder arrayClassBuilder = new StringBuilder("");

        while(paramSig.charAt(index) == '[') {
            count++;
            index++;
            arrayClassBuilder.append('[');
        }

        if(paramSig.charAt(index) == 'L') {
            String subParam = paramSig.substring(index);
            arrayClassBuilder.append(subParam.split(";")[0].replace('/','.'));
            arrayClassBuilder.append(";");
        }else {
            arrayClassBuilder.append(paramSig.charAt(index));
        }

        return arrayClassBuilder.toString();
    }

    private static FastHookParam parseParam(long sp, Class[] paramType, boolean isStatic) {
        FastHookParam param = new FastHookParam();

        int offset = 0;
        List<Object> args = new ArrayList<Object>();

        if(!isStatic) {
            param.receiver = getObjectParam(sp,offset);
            offset += 4;
        }

        if(paramType == null)
            return param;

        for(Class type : paramType) {
            if(type.equals(boolean.class)) {
                boolean b = getBooleanParam(sp,offset);
                args.add(new Boolean(b));
                offset += 4;
            }else if(type.equals(byte.class)) {
                byte b2 = getByteParam(sp,offset);
                args.add(new Byte(b2));
                offset += 4;
            }else if(type.equals(char.class)) {
                char c = getCharParam(sp,offset);
                args.add(new Character(c));
                offset += 4;
            }else if(type.equals(short.class)) {
                short s = getShortParam(sp,offset);
                args.add(new Short(s));
                offset += 4;
            }else if(type.equals(int.class)) {
                int i = getIntParam(sp,offset);
                args.add(new Integer(i));
                offset += 4;
            }else if(type.equals(long.class)) {
                long l = getLongParam(sp,offset);
                args.add(new Long(l));
                offset += 8;
            }else if(type.equals(float.class)) {
                float f = getFloatParam(sp,offset);
                args.add(new Float(f));
                offset += 4;
            }else if(type.equals(double.class)) {
                double d = getDoubleParam(sp,offset);
                args.add(new Double(d));
                offset += 8;
            }else if(type.equals(void.class)) {

            }else {
                Object obj = getObjectParam(sp,offset);
                args.add(obj);
                offset += 4;
            }
        }

        if(!args.isEmpty()) {
            param.args = args.toArray(new Object[args.size()]);
        }

        return param;
    }

    private static FastHookParam parseParam(int sp, int r11, Class[] paramType, boolean isStatic) {
        FastHookParam param = new FastHookParam();

        int offset = 4;
        int gpr_index = 1;
        int fpr_index = 0;
        int fpr_double_index = 0;
        List<Object> args = new ArrayList<Object>();

        if(!isStatic) {
            param.receiver = getObjectParam(r11,gpr_index * 4, TYPE_CORE_REG_ARGS);
            gpr_index += 1;
            offset += 4;
        }

        if(paramType == null)
            return param;

        for(Class type : paramType) {
            if(type.equals(boolean.class)) {
                if(gpr_index < CORE_REG_ARGS_SIZE) {
                    boolean b = getBooleanParam(r11,gpr_index * 4,TYPE_CORE_REG_ARGS);
                    args.add(new Boolean(b));
                    gpr_index += 1;
                }else {
                    boolean b = getBooleanParam(sp,offset,TYPE_SP);
                    args.add(new Boolean(b));
                }
                offset += 4;
            }else if(type.equals(byte.class)) {
                if(gpr_index < CORE_REG_ARGS_SIZE) {
                    byte b2 = getByteParam(r11,gpr_index * 4,TYPE_CORE_REG_ARGS);
                    args.add(new Byte(b2));
                    gpr_index += 1;
                }else {
                    byte b2 = getByteParam(sp,offset,TYPE_SP);
                    args.add(new Byte(b2));
                }
                offset += 4;
            }else if(type.equals(char.class)) {
                if(gpr_index < CORE_REG_ARGS_SIZE) {
                    char c = getCharParam(r11,gpr_index * 4,TYPE_CORE_REG_ARGS);
                    args.add(new Character(c));
                    gpr_index += 1;
                }else {
                    char c = getCharParam(sp,offset,TYPE_SP);
                    args.add(new Character(c));
                }
                offset += 4;
            }else if(type.equals(short.class)) {
                if(gpr_index < CORE_REG_ARGS_SIZE) {
                    short s = getShortParam(r11,gpr_index * 4,TYPE_CORE_REG_ARGS);
                    args.add(new Short(s));
                    gpr_index += 1;
                }else {
                    short s = getShortParam(sp,offset,TYPE_SP);
                    args.add(new Short(s));
                }
                offset += 4;
            }else if(type.equals(int.class)) {
                if(gpr_index < CORE_REG_ARGS_SIZE) {
                    int i = getIntParam(r11,gpr_index * 4,TYPE_CORE_REG_ARGS);
                    args.add(new Integer(i));
                    gpr_index += 1;
                }else {
                    int i = getIntParam(sp,offset,TYPE_SP);
                    args.add(new Integer(i));
                }
                offset += 4;
            }else if(type.equals(long.class)) {
                if(gpr_index < CORE_REG_ARGS_SIZE) {
                    if(gpr_index == 1) {
                        gpr_index += 1;
                    }
                    long l = getLongParam(r11,gpr_index * 4,TYPE_CORE_REG_ARGS);
                    args.add(new Long(l));
                    gpr_index += 2;
                }else {
                    long l = getLongParam(sp,offset,TYPE_SP);
                    args.add(new Long(l));
                }
                offset += 8;
            }else if(type.equals(float.class)) {
                if(fpr_index % 2 == 0) {
                    fpr_index = fpr_double_index > fpr_index ? fpr_double_index : fpr_index;
                }
                if(fpr_index < FP_REG_ARGS_SIZE) {
                    float f = getFloatParam(r11,fpr_index * 4, TYPE_FP_REG_ARGES);
                    args.add(new Float(f));
                    fpr_index += 1;
                }else {
                    float f = getFloatParam(sp,offset,TYPE_SP);
                    args.add(new Float(f));
                }
                offset += 4;
            }else if(type.equals(double.class)) {
                int fpr_index_temp = fpr_index % 2 == 0 ? fpr_index : fpr_index + 1;
                fpr_double_index = fpr_double_index > fpr_index_temp ? fpr_double_index : fpr_index_temp;

                if(fpr_double_index < FP_REG_ARGS_SIZE) {
                    double d = getDoubleParam(r11,fpr_double_index * 4, TYPE_FP_REG_ARGES);
                    args.add(new Double(d));
                    fpr_double_index += 2;
                }else {
                    double d = getDoubleParam(sp,offset,TYPE_SP);
                    args.add(new Double(d));
                }
                offset += 8;
            }else if(type.equals(void.class)) {

            }else {
                if(gpr_index < CORE_REG_ARGS_SIZE) {
                    Object obj = getObjectParam(r11,gpr_index * 4,TYPE_CORE_REG_ARGS);
                    args.add(obj);
                    gpr_index += 1;
                }else {
                    Object obj = getObjectParam(sp,offset,TYPE_SP);
                    args.add(obj);
                }
                offset += 4;
            }
        }

        if(!args.isEmpty()) {
            param.args = args.toArray(new Object[args.size()]);
        }

        return param;
    }

    private static Member generateForwardMethod(Member targetMethod, ClassLoader classLoader, Class[] paramType) {
        Member forwardMethod = null;

        try {
            Method generateProxy = Proxy.class.getDeclaredMethod(GENERATEPROXY,String.class,Class[].class,ClassLoader.class,Method[].class,Class[][].class);
            generateProxy.setAccessible(true);

            boolean needRecover = false;
            Method[] methods = new Method[1];

            if(targetMethod instanceof Constructor) {
                Method fakeMethod = constructorToMethod(targetMethod);
                methods[0] = fakeMethod;
                needRecover = true;
            }else if(targetMethod instanceof Method){
                methods[0] = (Method) targetMethod;
            }

            Class forwardClass = (Class)generateProxy.invoke(null,PROXY_CLASS_NAME + mProxyCount,null,classLoader,methods,null);
            poseAsObject(forwardClass,getThread());
            forwardMethod = forwardClass.getDeclaredMethod(methods[0].getName(),paramType);

            if(needRecover) {
                methodToConstructor(targetMethod);
                methodToConstructor(forwardMethod);
            }else {
                setDirectMethod(forwardMethod);
            }

            mProxyCount++;
        }catch (Exception e) {
            e.printStackTrace();
        }

        return forwardMethod;
    }

    private static long getThread() {
        if(mThread != 0)
            return mThread;

        try {
            Field nativePeer = Thread.class.getDeclaredField("nativePeer");
            nativePeer.setAccessible(true);
            mThread = (long) nativePeer.get(Thread.currentThread());
        }catch(Exception e) {
            e.printStackTrace();
        }

        return mThread;
    }

    private static void doFullRewriteHookInternal(Member targetMethod, Member hookMethod, Member forwardMethod) {
        ArrayList<HookRecord> newQuickTrampolineList = new ArrayList<HookRecord>();

        HookRecord headRecord = new HookRecord(TYPE_RECORD_REWRITE_HEAD, 0, 0, newQuickTrampolineList);
        HookRecord targetRecord = new HookRecord(TYPE_RECORD_REWRITE, targetMethod, hookMethod, forwardMethod, 0, 0, 1, newQuickTrampolineList);
        HookRecord tailRecord = new HookRecord(TYPE_RECORD_REWRITE_TAIL, 0, 2, newQuickTrampolineList);

        doFullRewriteHook(targetMethod, hookMethod, forwardMethod, headRecord, targetRecord, tailRecord);
        Logd("JumpTrampoline:0x"+Long.toHexString(headRecord.mJumpTrampoline)+" QuickHookTrampoline:0x"+Long.toHexString(targetRecord.mQuickHookTrampoline)+" QuickTargetTrampoline:0x"+Long.toHexString(targetRecord.mQuickTargetTrampoline)+" QuickOriginalTrampoline:0x"+Long.toHexString(tailRecord.mQuickOriginalTrampoline));

        newQuickTrampolineList.add(headRecord);
        newQuickTrampolineList.add(targetRecord);
        newQuickTrampolineList.add(tailRecord);

        mQuickTrampolineMap.put(Long.valueOf(getMethodEntryPoint(targetMethod)),newQuickTrampolineList);
        mHookMap.put(targetMethod,targetRecord);
    }

    private static void doReplaceHookInternal(Member targetMethod, Member hookMethod, Member forwardMethod, boolean isNative) {
        HookRecord targetRecord = new HookRecord(TYPE_RECORD_REPLACE,targetMethod,hookMethod,forwardMethod,0,0);

        doReplaceHook(targetMethod,hookMethod,forwardMethod,isNative,targetRecord);
        Logd("HookTrampoline:0x"+Long.toHexString(targetRecord.mHookTrampoline)+" TargetTrampoline:0x"+Long.toHexString(targetRecord.mTargetTrampoline));

        mHookMap.put(targetMethod,targetRecord);
    }

    private static void sendRetryMessage(Member targetMethod, Member hookMethod, Member forwardMethod, Class[] paramType, FastHookCallback callback, int mode, int retryCount) {
        HookMessage hookMessage = new HookMessage(targetMethod, hookMethod, forwardMethod, paramType, callback, mode, retryCount++);
        Message message = mHandler.obtainMessage(MESSAGE_RETRY);
        message.obj = hookMessage;

        mHandler.sendMessageDelayed(message, RETRY_TIME_WAIT);
    }

    private static class HookInfo {
        public Member mForwardMethod;
        public FastHookCallback mCallback;
        public Class[] mParamType;
        public boolean mIsStatic;

        public HookInfo(Member forwardMethod, FastHookCallback callback, Class[] paramType, boolean isStatic) {
            this.mForwardMethod = forwardMethod;
            this.mCallback = callback;
            this.mParamType = paramType;
            this.mIsStatic = isStatic;
        }
    }

    private static class HookRecord {
        public int mType;
        public Member mTargetMethod;
        public Member mHookMethod;
        public Member mForwardMethod;
        public long mJumpTrampoline;
        public long mQuickHookTrampoline;
        public long mQuickOriginalTrampoline;
        public long mQuickTargetTrampoline;
        public long mHookTrampoline;
        public long mTargetTrampoline;
        public int index;
        public ArrayList<HookRecord> mQuickTrampolineList;

        public HookRecord(int type, Member targetMethod, Member hookMethod, Member forwardMethod, long quickHookTrampoline, long quickTargetTrampoline, int index, ArrayList<HookRecord> quickTrampolineList) {
            this.mType = type;
            this.mTargetMethod = targetMethod;
            this.mHookMethod = hookMethod;
            this.mForwardMethod = forwardMethod;
            this.mQuickHookTrampoline = quickHookTrampoline;
            this.mQuickTargetTrampoline = quickTargetTrampoline;
            this.index = index;
            this.mQuickTrampolineList = quickTrampolineList;
        }

        public HookRecord(int type, long quickTrampoline, int index, ArrayList<HookRecord> quickTrampolineList) {
            this.mType = type;
            if(type == TYPE_RECORD_REWRITE_HEAD) {
                this.mQuickOriginalTrampoline = quickTrampoline;
            }else if(type == TYPE_RECORD_REWRITE_TAIL) {
                this.mJumpTrampoline = quickTrampoline;
            }
            this.index = index;
            this.mQuickTrampolineList = quickTrampolineList;
        }

        public HookRecord(int type, Member targetMethod, Member hookMethod, Member forwardMethod, long hookTrampoline, long targetTrampoline) {
            this.mType = type;
            this.mTargetMethod = targetMethod;
            this.mHookMethod = hookMethod;
            this.mForwardMethod = forwardMethod;
            this.mHookTrampoline = hookTrampoline;
            this.mTargetTrampoline = targetTrampoline;
        }

        public String toString() {
            StringBuilder sb = new StringBuilder("");
            switch(mType) {
                case TYPE_RECORD_REWRITE_HEAD:
                    sb.append("HEAD Jump:"+Long.toHexString(mJumpTrampoline)+" index:"+index);
                    break;
                case TYPE_RECORD_REWRITE:
                    sb.append("RECORD target:"+mTargetMethod.getName()+" hook:"+mHookMethod.getName()+" forward:"+mForwardMethod.getName()+" hook trampoline:0x"+Long.toHexString(mQuickHookTrampoline)+" target trampoline:0x"+Long.toHexString(mQuickTargetTrampoline)+" index:"+index);
                    break;
                case TYPE_RECORD_REWRITE_TAIL:
                    sb.append("TAIL original:"+Long.toHexString(mQuickOriginalTrampoline)+" index:"+index);
                    break;
            }
            return sb.toString();
        }
    }

    private static class HookHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_RETRY:
                    HookMessage hookMessage = (HookMessage)msg.obj;
                    doHook(hookMessage.mTargetMethod,hookMessage.mHookMethod,hookMessage.mForwardMethod,hookMessage.mParamType,hookMessage.mCallback,hookMessage.mMode,hookMessage.mRetryCount);
                    break;
            }
        }
    }

    private static class HookMessage {
        public Member mTargetMethod;
        public Member mHookMethod;
        public Member mForwardMethod;
        public Class[] mParamType;
        public FastHookCallback mCallback;
        public int mMode;
        public int mRetryCount;

        HookMessage(Member targetMethod, Member hookMethod, Member forwardMethod, Class[] paramType, FastHookCallback callback, int mode, int retryCount) {
            this.mTargetMethod = targetMethod;
            this.mHookMethod = hookMethod;
            this.mForwardMethod = forwardMethod;
            this.mParamType = paramType;
            this.mCallback = callback;
            this.mMode = mode;
            this.mRetryCount = retryCount;
        }
    }

    private static void throwException(Exception exception) {
        try {
            throw exception;
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void Logd(String message) {
        if(DEBUG) {
            Log.d(TAG,message);
        }
    }

    private static void Loge(String message) {
        Log.d(TAG,message);
    }

    public native static void disableHiddenApiCheck();
    private native static void init(int version);
    private native static boolean is32bit();
    private native static Member getReflectedMethod(long artMethod);
    private native static Member getReflectedMethod(int artMethod);
    private native static boolean getBooleanParam(long sp, int offset);
    private native static boolean getBooleanParam(int address, int offset, int type);
    private native static byte getByteParam(long address, int offset);
    private native static byte getByteParam(int address, int offset, int type);
    private native static char getCharParam(long address, int offset);
    private native static char getCharParam(int address, int offset, int type);
    private native static short getShortParam(long address, int offset);
    private native static short getShortParam(int address, int offset, int type);
    private native static int getIntParam(long address, int offset);
    private native static int getIntParam(int address, int offset, int type);
    private native static long getLongParam(long address, int offset);
    private native static long getLongParam(int address, int offset, int type);
    private native static float getFloatParam(long address, int offset);
    private native static float getFloatParam(int address, int offset, int type);
    private native static double getDoubleParam(long address, int offset);
    private native static double getDoubleParam(int address, int offset, int type);
    private native static Object getObjectParam(long address, int offset);
    private native static Object getObjectParam(int address, int offset, int type);
    private native static void poseAsObject(Class targetClass, long thread);
    private native static Method constructorToMethod(Member method);
    private native static void methodToConstructor(Member method);
    private native static void disableJITInline();
    private native static long getMethodEntryPoint(Member method);
    private native static boolean compileMethod(Member method);
    private native static boolean isCompiled(Member method);
    private native static boolean doRewriteHookCheck(Member method);
    private native static boolean isNativeMethod(Member method);
    private native static boolean isStaticMethod(Member method);
    private native static void setNativeMethod(Member method);
    private native static void setDirectMethod(Member method);
    private native static int checkJitState(Member method);
    private native static int doFullRewriteHook(Member targetMethod, Member hookMethod, Member forwardMethod, HookRecord headRecord, HookRecord targetRecord, HookRecord tailRecord);
    private native static int doPartRewriteHook(Member targetMethod, Member hookMethod, Member forwardMethod, long quickOriginalTrampoline, long prevQuickHookTrampoline, HookRecord targetRecord);
    private native static int doReplaceHook(Member targetMethod, Member hookMethod, Member forwardMethod, boolean isNative, HookRecord targetRecord);
}