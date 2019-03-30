package pers.turing.technician.fasthook;

public interface FastHookCallback {
    void beforeHookedMethod(FastHookParam param);
    void afterHookedMethod(FastHookParam param);
}
