package com.wilbert.sveditor.library.log;

import android.util.Log;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   : 对android.util.Log的简单包装
 */
public class ALog {

    public static int v(String tag, String msg) {
        return Log.i(tag, msg);
    }

    public static int v(String tag, String msg, Throwable tr) {
        return Log.v(tag, msg, tr);
    }

    public static int d(String tag, String msg) {
        return Log.d(tag, msg);
    }

    public static int d(String tag, String msg, Throwable tr) {
        return Log.d(tag, msg, tr);
    }

    public static int i(String tag, String msg) {
        return Log.i(tag, msg);
    }

    public static int i(String tag, String msg, Throwable tr) {
        return Log.i(tag, msg, tr);
    }

    public static int w(String tag, String msg) {
        return Log.w(tag, msg);
    }

    public static int w(String tag, String msg, Throwable tr) {
        return Log.w(tag, msg, tr);
    }

    public static int w(String tag, Throwable tr) {
        return Log.w(tag, tr);
    }

    public static int e(String tag, String msg) {
        return Log.e(tag, msg);
    }

    public static int e(String tag, String msg, Throwable tr) {
        return Log.e(tag, msg, tr);
    }

    public static int wtf(String tag, String msg) {
        return Log.wtf(tag, msg);
    }

    public static int wtf(String tag, Throwable tr) {
        return Log.wtf(tag, tr);
    }

    /**
     * What a Terrible Failure: Report an exception that should never happen.
     * Similar to {@link #wtf(String, Throwable)}, with a message as well.
     *
     * @param tag Used to identify the source of a log message.
     * @param msg The message you would like logged.
     * @param tr  An exception to log.  May be null.
     */
    public static int wtf(String tag, String msg, Throwable tr) {
        return Log.wtf(tag, msg, tr);
    }

}
