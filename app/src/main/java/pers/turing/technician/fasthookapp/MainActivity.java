package pers.turing.technician.fasthookapp;

import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

import pers.turing.technician.fasthook.FastHookCallback;
import pers.turing.technician.fasthook.FastHookManager;
import pers.turing.technician.fasthook.FastHookParam;
import pers.turing.technician.fasthookapp.hook.Test;

public class MainActivity extends AppCompatActivity {

    TextView mConstructorText;
    TextView mDirectText;
    TextView mVirtualText;
    TextView mStaticText;
    TextView mNativeDirectText;
    TextView mNativeVirtualText;
    TextView mNativeStaticText;
    TextView mSystemText;
    TextView mHiddenText;

    String mConstructorTextString;
    String mDirectTextString;
    String mVirtualTextString;
    String mStaticTextString;
    String mNativeDirectTextString;
    String mNativeVirtualTextString;
    String mNativeStaticTextString;
    String mSystemTextString;
    String mHiddenTextString;

    Button mRewriteMode;
    Button mReplaceMode;

    Context mContext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mContext = this;

        mConstructorText = findViewById(R.id.constructor_test);
        mDirectText = findViewById(R.id.direct_test);
        mVirtualText = findViewById(R.id.virtual_test);
        mStaticText = findViewById(R.id.static_test);
        mNativeDirectText = findViewById(R.id.native_direct_test);
        mNativeVirtualText = findViewById(R.id.native_virtual_test);
        mNativeStaticText = findViewById(R.id.native_static_test);
        mSystemText = findViewById(R.id.system_test);
        mHiddenText = findViewById(R.id.hidden_test);

        mConstructorTextString = mConstructorText.getText().toString();
        mDirectTextString = mDirectText.getText().toString();
        mVirtualTextString = mVirtualText.getText().toString();
        mStaticTextString = mStaticText.getText().toString();
        mNativeDirectTextString = mNativeDirectText.getText().toString();
        mNativeVirtualTextString = mNativeVirtualText.getText().toString();
        mNativeStaticTextString = mNativeStaticText.getText().toString();
        mSystemTextString = mSystemText.getText().toString();
        mHiddenTextString = mHiddenText.getText().toString();

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            mHiddenText.setVisibility(View.VISIBLE);
        }else {
            mHiddenText.setVisibility(View.GONE);
        }

        mRewriteMode = findViewById(R.id.button_rewrite);
        mReplaceMode = findViewById(R.id.button_replace);

        mRewriteMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!MainApplication.mTest) {
                    MainApplication.mTest = true;
                    doHook(FastHookManager.MODE_REWRITE);
                    test(String.valueOf(FastHookManager.MODE_REWRITE));
                }else {
                    Toast toast = Toast.makeText(mContext,"already hook,please restart app to test!",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });

        mReplaceMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!MainApplication.mTest) {
                    MainApplication.mTest = true;
                    doHook(FastHookManager.MODE_REPLACE);
                    test(String.valueOf(FastHookManager.MODE_REPLACE));
                }else {
                    Toast toast = Toast.makeText(mContext,"already hook,please restart app to test!",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
    }

    public void doHook(int mode) {
        String className = "pers.turing.technician.fasthookapp.hook.Test";

        FastHookManager.doHook(className,null, "<init>", "Ljava/lang/String;", new FastHookCallback() {
            @Override
            public void beforeHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"beforeHookedMethod ConstrctorTest");
                param.args[0] = "hook param-"+param.args[0];
            }

            @Override
            public void afterHookedMethod(FastHookParam param) {

            }
        },mode,false);

        FastHookManager.doHook(className, null, "testDirect", "Ljava/lang/String;", new FastHookCallback() {
            @Override
            public void beforeHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"beforeHookedMethod DirectTest");
                param.args[0] = "hook param-"+param.args[0];
            }

            @Override
            public void afterHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"afterHookedMethod DirectTest");
                param.result = param.result+"-hook result";
            }
        },mode,false);

        FastHookManager.doHook(className, null, "testVirtual", "Ljava/lang/String;", new FastHookCallback() {
            @Override
            public void beforeHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"beforeHookedMethod VirtualTest");
                param.args[0] = "hook param-"+param.args[0];
            }

            @Override
            public void afterHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"afterHookedMethod VirtualTest");
                param.result = param.result+"-hook result";
            }
        },mode,false);

        FastHookManager.doHook(className, null, "testStatic", "Ljava/lang/String;", new FastHookCallback() {
            @Override
            public void beforeHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"beforeHookedMethod StaticTest");
                param.args[0] = "hook param-"+param.args[0];
            }

            @Override
            public void afterHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"afterHookedMethod StaticTest");
                param.result = param.result+"-hook result";
            }
        },mode,false);

        FastHookManager.doHook(className, null, "testNativeDirect", "Ljava/lang/String;", new FastHookCallback() {
            @Override
            public void beforeHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"beforeHookedMethod NativeDirectTest");
                param.args[0] = "hook param-"+param.args[0];
            }

            @Override
            public void afterHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"afterHookedMethod NativeDirectTest");
                param.result = param.result+"-hook result";
            }
        },mode,false);

        FastHookManager.doHook(className, null, "testNativeVirtual", "Ljava/lang/String;", new FastHookCallback() {
            @Override
            public void beforeHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"beforeHookedMethod NativeVirtualTest");
                param.args[0] = "hook param-"+param.args[0];
            }

            @Override
            public void afterHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"afterHookedMethod NativeVirtualTest");
                param.result = param.result+"-hook result";
            }
        },mode,false);

        FastHookManager.doHook(className, null, "testNativeStatic", "Ljava/lang/String;", new FastHookCallback() {
            @Override
            public void beforeHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"beforeHookedMethod NativeStaticTest");
                param.args[0] = "hook param-"+param.args[0];
            }

            @Override
            public void afterHookedMethod(FastHookParam param) {
                Log.e(MainApplication.TAG,"afterHookedMethod NativeStaticTest");
                param.result = param.result+"-hook result";
            }
        },mode,false);

        FastHookManager.doHook("android.widget.TextView", null, "setText", "Ljava/lang/CharSequence;", new FastHookCallback() {
            @Override
            public void beforeHookedMethod(FastHookParam param) {
                CharSequence charSequence = (CharSequence) param.args[0];
                if(charSequence.toString().contains("Test SystemMethod") && !charSequence.toString().contains("hook param")) {
                    Log.e(MainApplication.TAG,"beforeHookedMethod SystemTest");
                    param.args[0] = "hook param-"+charSequence;
                }
            }

            @Override
            public void afterHookedMethod(FastHookParam param) {

            }
        },mode,false);
    }

    public void reset() {
        mConstructorText.setText(mConstructorTextString);
        mDirectText.setText(mDirectTextString);
        mVirtualText.setText(mVirtualTextString);
        mStaticText.setText(mStaticTextString);
        mNativeDirectText.setText(mNativeDirectTextString);
        mNativeVirtualText.setText(mNativeVirtualTextString);
        mNativeStaticText.setText(mNativeStaticTextString);
    }

    public void test(String mode) {
        reset();

        Test test = new Test(mConstructorText.getText().toString());
        mConstructorText.setText(mode + test.mMessage);

        if(test.mMessage.contains("hook param")) {
            mConstructorText.setTextColor(Color.GREEN);
        }else {
            mConstructorText.setTextColor(Color.RED);
        }

        String directMessage = mDirectText.getText().toString();
        directMessage = test.testDirectMethod(directMessage);
        mDirectText.setText(mode + directMessage);
        if(directMessage.contains("hook param") && directMessage.contains("hook result")) {
            mDirectText.setTextColor(Color.GREEN);
        }else {
            mDirectText.setTextColor(Color.RED);
        }

        String virtualMessage = mVirtualText.getText().toString();
        virtualMessage = test.testVirtual(virtualMessage);
        mVirtualText.setText(mode + virtualMessage);
        if(virtualMessage.contains("hook param") && virtualMessage.contains("hook result")) {
            mVirtualText.setTextColor(Color.GREEN);
        }else {
            mVirtualText.setTextColor(Color.RED);
        }

        String staticMessage = mStaticText.getText().toString();
        staticMessage = Test.testStatic(staticMessage);
        mStaticText.setText(mode + staticMessage);
        if(staticMessage.contains("hook param") && staticMessage.contains("hook result")) {
            mStaticText.setTextColor(Color.GREEN);
        }else {
            mStaticText.setTextColor(Color.RED);
        }

        String nativeDirectMessage = mNativeDirectText.getText().toString();
        nativeDirectMessage = test.testNativeDirectMethod(nativeDirectMessage);
        mNativeDirectText.setText(mode + nativeDirectMessage);
        if(nativeDirectMessage.contains("hook param") && nativeDirectMessage.contains("hook result")) {
            mNativeDirectText.setTextColor(Color.GREEN);
        }else {
            mNativeDirectText.setTextColor(Color.RED);
        }

        String nativeVirtualMessage = mNativeVirtualText.getText().toString();
        nativeVirtualMessage = test.testNativeVirtualMethod(nativeVirtualMessage);
        mNativeVirtualText.setText(mode + nativeVirtualMessage);
        if(nativeVirtualMessage.contains("hook param") && nativeVirtualMessage.contains("hook result")) {
            mNativeVirtualText.setTextColor(Color.GREEN);
        }else {
            mNativeVirtualText.setTextColor(Color.RED);
        }

        String nativeStaticMessage = mNativeStaticText.getText().toString();
        nativeStaticMessage = test.testNativeStaticMethod(nativeStaticMessage);
        mNativeStaticText.setText(mode + nativeStaticMessage);
        if(nativeStaticMessage.contains("hook param") && nativeStaticMessage.contains("hook result")) {
            mNativeStaticText.setTextColor(Color.GREEN);
        }else {
            mNativeStaticText.setTextColor(Color.RED);
        }

        String systemMessage = mSystemTextString;
        mSystemText.setText(systemMessage);
        mSystemText.setText(mode + mSystemText.getText());
        if(mSystemText.getText().toString().contains("hook param")) {
            mSystemText.setTextColor(Color.GREEN);
        }else {
            mSystemText.setTextColor(Color.RED);
        }

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            FastHookManager.disableHiddenApiCheck();
            try {
                Class<?> activityClass = Class.forName("dalvik.system.VMRuntime");
                Method setHiddenApiExemptions = activityClass.getDeclaredMethod("setHiddenApiExemptions", String[].class);
                setHiddenApiExemptions.setAccessible(true);
                mHiddenText.setText(setHiddenApiExemptions.getName());
            }catch (Exception e) {
                e.printStackTrace();
            }

            if(!mHiddenText.getText().equals(mHiddenTextString)) {
                mHiddenText.setTextColor(Color.GREEN);
            }else {
                mHiddenText.setTextColor(Color.RED);
            }
        }
    }
}
