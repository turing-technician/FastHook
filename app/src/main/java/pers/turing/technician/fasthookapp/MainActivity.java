package pers.turing.technician.fasthookapp;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import pers.turing.technician.fasthook.FastHookManager;
import pers.turing.technician.fasthookapp.hook.Test;

public class MainActivity extends AppCompatActivity {

    TextView mConstructorText;
    TextView mDirectText;
    TextView mVirtualText;
    TextView mStaticText;
    TextView mNativeDirectText;
    TextView mNativeVirtualText;
    TextView mNativeStaticText;

    String mConstructorTextString;
    String mDirectTextString;
    String mVirtualTextString;
    String mStaticTextString;
    String mNativeDirectTextString;
    String mNativeVirtualTextString;
    String mNativeStaticTextString;

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

        mConstructorTextString = mConstructorText.getText().toString();
        mDirectTextString = mDirectText.getText().toString();
        mVirtualTextString = mVirtualText.getText().toString();
        mStaticTextString = mStaticText.getText().toString();
        mNativeDirectTextString = mNativeDirectText.getText().toString();
        mNativeVirtualTextString = mNativeVirtualText.getText().toString();
        mNativeStaticTextString = mNativeStaticText.getText().toString();

        mRewriteMode = findViewById(R.id.button_rewrite);
        mReplaceMode = findViewById(R.id.button_replace);

        mRewriteMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!MainApplication.mTest) {
                    MainApplication.mTest = true;
                    FastHookManager.doHook("pers.turing.technician.fasthookapp.hook.HookInfo",null,null,null,null,false);
                    test("1:");
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
                    FastHookManager.doHook("pers.turing.technician.fasthookapp.hook.HookInfo2",null,null,null,null,false);
                    test("2:");
                }else {
                    Toast toast = Toast.makeText(mContext,"already hook,please restart app to test!",Toast.LENGTH_SHORT);
                    toast.show();
                }
            }
        });
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
    }
}
