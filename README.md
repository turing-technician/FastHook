# FastHook
一种高效稳定、简洁易用的Android Hook框架，**实际项目验证，拥有远超其他同类框架的优异稳定性**。
实现原理，请参考
- [**FastHook——一种高效稳定、简洁易用的Android Hook框架**](https://blog.csdn.net/TuringTechnician/article/details/88613555)
- [**FastHook——巧妙利用动态代理实现非侵入式AOP**](https://blog.csdn.net/TuringTechnician/article/details/88925234)
# 使用
提供两种使用方式：**配置方式**、**Callback方式**
- **配置方式**：代码取master分支
- **callback方式**：代码取callback分支

## 配置方式
提供一个Hook信息类，可以是任意类，但是必须定义有**static String[] HOOK_ITEMS**；例如
HookInfo.java
```
private static String[] mHookItem = {
            "mode",
            "targetClassName","targetMethodName","targetParamSig",
            "hookClassName","hookMethodName","hookParamSig",
            "forwardClassName","forwardMethodName","forwardParamSig"
};
public static String[][] HOOK_ITEMS = {
            mHookItem
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
- **hookInfoClassName**：HookInfo类名
- **hookInfoClassLoader**：HookInfo类所在的ClassLoader，如果为null，代表当前ClassLoader
- **targetClassLoader**：Target方法所在的ClassLoader，如果为null，代表当前ClassLoader
- **hookClassLoader**：Hook方法所在的ClassLoader，如果为null，代表当前ClassLoader
- **forwardClassLoader**：Forward方法所在的ClassLoader，如果为null，代表当前ClassLoader
- **jitInline**：是否内联，false，禁止内联；true，允许内联

**调用**
- 插件式Hook：需要提供插件的ClassLoader,建议在attachBaseContext方法里调用。
```
//插件式Hook，需要提供插件的ClassLoader
FastHookManger.doHook("hookInfoClassName",pluginsClassloader,null,pluginsClassloader,pluginsClassloader,false);
```
- 内置Hook：都位于当前ClassLoader建议在attachBaseContext方法里调用。
```
//内置Hook，都位于当前ClassLoader
FastHookManger.doHook("hookInfoClassName",null,null,null,null,false);
```
- 其他形式 Hook：需要体供插件的ClassLoader和apk的ClassLoader，建议在handleBindApplication方法里合适的地方调用，一般在加载apk后，调用attachBaseContext前。
```
//Root Hook，需要体供插件的ClassLoader和apk的ClassLoader
FastHookManger.doHook("hookInfoClassName",pluginsClassloader,apkClassLoader,pluginsClassloader,pluginsClassloader,false);
```
## Callback方式
**实现FastHookCallback接口**
```
public interface FastHookCallback {
    void beforeHookedMethod(FastHookParam param);
    void afterHookedMethod(FastHookParam param);
}
```
- **beforeHookedMethod**：原方法调用前调用
- **afterHookedMethod**：原方法调用后调用
```
public class FastHookParam {
    public Object receiver;
    public Object[] args;
    public Object result;
    public boolean replace;
}
```
- **receiver**：this对象，static方法则为null
- **args**：方法参数
- **result**：方法返回值
- **replace**：是否替换方法，如果为true，则不会调用原方法，并以result返回

**接口**
```
/**
 *
 *@param className 目标方法类名
 *@param classLoader 目标方法所在ClassLoader，如果为null，代表当前ClassLoader
 *@param methodName 目标方法方法名
 *@param methodSig 目标方法参数签名，不包括返回类型
 *@param callback hook回调方法
 *@param mode hook模式
 *@param jitInline 是否内联，false，禁止内联；true，允许内联
 *
 */
 FastHookManager.doHook(String className, ClassLoader classLoader, String methodName, String methodSig, FastHookCallback callback, int mode, boolean jitInline)
 ```
- **className**：目标方法类名
- **classLoader**：目标方法所在ClassLoader，如果为null，代表当前ClassLoader
- **methodName**：目标方法方法名
- **methodSig**：目标方法参数签名，不包括返回类型
- **callback**：hook回调方法
- **mode**：hook模式，FastHookManager.MODE_REWRITE和FastHookManager.MODE_REPLACE
- **jitInline**：是否内联，false，禁止内联；true，允许内联

**调用**
```
FastHookManager.doHook(className,classLoader, methodName, methodSig, new FastHookCallback() {
            @Override
            public void beforeHookedMethod(FastHookParam param) {
                
            }

            @Override
            public void afterHookedMethod(FastHookParam param) {

            }
        },FastHookManager.MODE_REWRITE,false);
```

# 支持Android版本
- Android 9.0 (API 28)
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
1. **[VirtualFastHook](https://github.com/turing-technician/VirtualFastHook)**：结合VirtualApp的免root Hook工具。
2. **[Enhanced_dlfunctions](https://github.com/turing-technician/Enhanced_dlfunctions)**：增强版本的dl函数，支持获取所有符号，包括.dynsym段和.symtab段
