package com.example.uncaughtexception;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by Administrator on 2017/11/22.
 *
 * 自定义处理异常：
 * 全局异常捕获
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    /**
     * 系统默认异常处理器
     */
    private Thread.UncaughtExceptionHandler mDefaultHandler;

    private Context mContext;

    /**
     * 存储设备信息、异常信息
     */
    private Map<String,String> mInfo=new HashMap<>();

    /**
     * 文件日期格式
     */
    private DateFormat dateFormat=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    private static volatile CrashHandler mInstance;

    private CrashHandler() {}

    /**
     * 懒汉 单例模式
     * @return
     */
    public static CrashHandler getInstance() {
        if (null == mInstance) {
            synchronized (CrashHandler.class) {
                if (null == mInstance) {
                    mInstance = new CrashHandler();
                }
            }
        }
        return mInstance;
    }

    /**
     * 该方法抛出的错误信息都会被Java虚拟机忽略：
     * 1、收集错误信息
     * 2、保存错误信息
     * 3、上传错误信息（本例未实现文件上传）
     *
     * @param t
     * @param e
     */
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        //未人为处理，调用系统默认处理器处理
        if (!handleException(e)) {
            if (null != mDefaultHandler) {
                mDefaultHandler.uncaughtException(t, e);
            }

            //已经人为处理
        } else {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }

            //强制退出
            Process.killProcess(Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 初始化默认处理器
     * @param context
     */
    public void init(Context context){
        this.mContext=context;
        mDefaultHandler=Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 人为处理异常
     *
     * @param e
     * @return true已处理,false未处理
     */
    private boolean handleException(Throwable e){
        if (null == e) {
            return false;
        }

        //给提示
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(mContext, "UncaughtException", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();

        //收集错误信息
        collectErrorInfo();

        //保存错误信息
        saveErrorInfo(e);

        return false;
    }

    /**
     * 保存错误信息，即以文件形式保存到sdCard
     */
    private void saveErrorInfo(Throwable e) {
        StringBuffer stringBuf = new StringBuffer();

        //把mInfo中的设备信息、错误信息等写到StringBuffer中
        for (Map.Entry<String, String> entry : mInfo.entrySet()) {
            String keyName = entry.getKey();
            String value = entry.getValue();
            stringBuf.append(keyName).append("-").append(value).append("\n");
        }

        //StringBuffer中的信息写到sdCard中
        //装饰者模式
        Writer writer = new StringWriter();
        PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);

        Throwable cause = e.getCause();
        while (null != cause) {
            cause.printStackTrace(printWriter);
            cause = e.getCause();
        }

        printWriter.close();

        String result = writer.toString();
        stringBuf.append(result);

        long curTime = System.currentTimeMillis();
        String time = dateFormat.format(new Date());
        String fileName = "crash-" + time + "-" + curTime + ".log";

        //判断有无sdCard
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //String path = "/sdCard/crash/";
            String path=mContext.getCacheDir().getPath();
            Log.d("tag","有sdCard："+path);

            File dir = new File(path);

            if (!dir.exists()) {
                dir.mkdirs();
            }

            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(path + fileName);
                fos.write(stringBuf.toString().getBytes());
            } catch (FileNotFoundException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                try {
                    assert fos != null;
                    fos.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }else {
            Log.d("tag","无sdCard");
        }
    }

    /**
     * 收集错误信息
     */
    private void collectErrorInfo() {
        PackageManager pm = mContext.getPackageManager();
        try {
            PackageInfo pi = pm.getPackageInfo(mContext.getPackageName(), PackageManager.GET_ACTIVITIES);

            if (null != pi) {
                String versionName = TextUtils.isEmpty(pi.versionName) ? "未设置版本名称" : pi.versionName;
                String versionCode = pi.versionCode + "";
                mInfo.put("versionName", versionName);
                mInfo.put("versionCode", versionCode);
            }

            //通过反射获取所有信息
            Field[] fields = Build.class.getFields();

            if (null != fields && fields.length > 0) {
                for (Field field : fields) {
                    //可访问
                    field.setAccessible(true);

                    try {
                        mInfo.put(field.getName(), field.get(null).toString());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

}
