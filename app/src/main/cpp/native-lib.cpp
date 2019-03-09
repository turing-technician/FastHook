#include <jni.h>
#include <string>
#include <android/log.h>

#define LOG_TAG "FastHookManager"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

extern "C" JNIEXPORT jstring

JNICALL
Java_pers_turing_technician_fasthookapp_hook_Test_testNativeDirect(
        JNIEnv *env,
        jobject thiz,jstring message) {
    LOGI("invoke NativeDirectTest");
    const char *fromjava = env->GetStringUTFChars(message, 0);
    int len=strlen(fromjava);
    char str[len];
    strcpy (str,fromjava);
    return env->NewStringUTF(str);
}

extern "C" JNIEXPORT jstring

JNICALL
Java_pers_turing_technician_fasthookapp_hook_Test_testNativeVirtual(
        JNIEnv *env,
        jobject thiz,jstring message) {
    LOGI("invoke NativeVirtualTest");
    const char *fromjava = env->GetStringUTFChars(message, 0);
    int len=strlen(fromjava);
    char str[len];
    strcpy (str,fromjava);
    return env->NewStringUTF(str);
}

extern "C" JNIEXPORT jstring

JNICALL
Java_pers_turing_technician_fasthookapp_hook_Test_testNativeStatic(
        JNIEnv *env,jobject thiz,jstring message) {
    LOGI("invoke NativeStaticTest");
    const char *fromjava = env->GetStringUTFChars(message, 0);
    int len=strlen(fromjava);
    char str[len];
    strcpy (str,fromjava);
    return env->NewStringUTF(str);
}
