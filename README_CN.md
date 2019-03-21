# FastHook
一种高效稳定、简洁易用的Android Hook框架，**实际项目验证，拥有远超其他同类框架的优异稳定性**
实现原理，请看**[FastHook](https://www.jianshu.com/p/d2a8aa249134)**
# 使用
提供一个Hook信息类，可以是任意类，但是必须定义有**static String[] HOOK_ITEMS**；例如
HookInfo.java
```
private static String[] mHookItem = {
            "mode",
            "targetClassName","targetMethodName","targetParamSig",
            "hookClassName","hookMethodName","hookParamSig",
            "forwardClassName","forwardMethodName","forwardParamSig"
};
```
mode:提供两种hook模式："1":**Inline Hook(性能高效)**；"2":**EntryPoint替换模式(弥补Inline Hook在一些情况下不能Hook的缺陷)**

**接口**
```
/**
 *
 *@param hookInfoClassName HookInfo类名
 *@param hookInfoClassLoader HookInfo类所在的ClassLoader，如果为null，代表当前ClassLoader
 *@param targetClassLoader Target方法所在的ClassLoader，如果为null，代表当前ClassLoader
 *@param hookClassLoader Hook方法所在的ClassLoader，如果为null，代表当前ClassLoader
 *@param forwardClassLoader Forward方法所在的ClassLoader，如果为null，代表当前ClassLoader
 *@param jitInline 是否内联，false，禁止内联；true，允许内联
 *
 */
public static void doHook(String hookInfoClassName, ClassLoader hookInfoClassLoader, ClassLoader targetClassLoader, ClassLoader hookClassLoader, ClassLoader forwardClassLoader, boolean jitInline)
```

**调用**
1. 插件式Hook：需要提供插件的ClassLoader,建议在attachBaseContext方法里调用。
```
//插件式Hook，需要提供插件的ClassLoader
FastHookManger.doHook("hookInfoClassName",pluginsClassloader,null,pluginsClassloader,pluginsClassloader,false);
```
2. 内置Hook：都位于当前ClassLoader建议在attachBaseContext方法里调用。
```
//内置Hook，都位于当前ClassLoader
FastHookManger.doHook("hookInfoClassName",null,null,null,null,false);
```
3. 其他形式 Hook：需要体供插件的ClassLoader和apk的ClassLoader，建议在handleBindApplication方法里合适的地方调用，一般在加载apk后，调用attachBaseContext前。
```
//Root Hook，需要体供插件的ClassLoader和apk的ClassLoader
FastHookManger.doHook("hookInfoClassName",pluginsClassloader,apkClassLoader,pluginsClassloader,pluginsClassloader,false);
```
# 支持Android版本
- Android 9.0（API 28）
- Android 8.1 (API 27)
- Android 8.0 (API 26)
- Android 7.1 (API 25)
- Android 7.0 (API 24)
- Android 6.0 (API 23)
- Android 5.1 (API 22)
- Android 5.0 (API 21)
# 支持架构
- Thumb2
- Arm64
# 参考
**[VirtualFastHook](https://github.com/turing-technician/VirtualFastHook)**：结合VirtualApp的免root Hook工具
**[Enhanced_dlfunctions](https://github.com/turing-technician/Enhanced_dlfunctions)**：增强版本的dl函数，支持获取所有符号，包括.dynsym段和.symtab段
