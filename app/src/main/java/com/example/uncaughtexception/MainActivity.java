package com.example.uncaughtexception;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * 自定义处理异常：
 * 全局异常捕获
 *
 * 1、自定义异常处理类CrashHandler并实现接口Thread.UncaughtExceptionHandler
 * 2、自定义CrashApp继承Application
 * 3、<application/>标签中给出android:name=".CrashApp"
 */
public class MainActivity extends AppCompatActivity {
    private String string;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d("tag","结果："+"anything".equals(string));
        Log.d("tag","长度："+string.length());
    }

}
