[中文文档](https://www.jianshu.com/p/d2a8aa249134)
# FastHook
Android ART Hook
# Supports
- Android 9.0（API 28）
- Android 8.1 (API 27)
- Android 8.0 (API 26)
- Android 7.1 (API 25)
- Android 7.0 (API 24)
- Android 6.0 (API 23)
- Android 5.1 (API 22)
- Android 5.0 (API 21)
# ABI
- Thumb2
- Arm64
# Usage
```
/**
 *
 *@param hookInfoClassName class name of HookInfo
 *@param hookInfoClassLoader class loader of HookInfo，if null，that is FastHook class loader
 *@param targetClassLoader class loader of TargetMethod，if null，that is FastHook class loader
 *@param hookClassLoader class loader of HookMethod，if null，that is FastHook class loader
 *@param forwardClassLoader class loader of ForwardMethod，if null，that is FastHook class loader
 *@param jitInline if false, diable jit inline, otherwise allow jit inline
 *
 */
public static void doHook(String hookInfoClassName, ClassLoader hookInfoClassLoader, ClassLoader targetClassLoader, ClassLoader hookClassLoader, ClassLoader forwardClassLoader, boolean jitInline)
```
call dohook to hook,for example
```
//hook and target at different classloader
FastHookManger.doHook("hookInfoClassName",pluginsClassloader,null,pluginsClassloader,pluginsClassloader,false);

//hook and target at the same classloader
FastHookManger.doHook("hookInfoClassName",null,null,null,null,false);
```
