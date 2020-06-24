package com.wilbert.sveditor.library.codecs;

import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.wilbert.sveditor.library.codecs.abs.IDecoder;
import com.wilbert.sveditor.library.codecs.abs.IExtractor;
import com.wilbert.sveditor.library.codecs.abs.IExtractorListener;
import com.wilbert.sveditor.library.codecs.abs.InputInfo;
import com.wilbert.sveditor.library.log.ALog;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author : Wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class SvExtractor {
    private final String TAG = "SvMediaExtractor";
    private ExtractorHandler mHandler;

    public enum Type {
        AUDIO, VIDEO;
    }

    private IExtractor mExtractor;
    private MediaFormat mFormat;
    private long mCurrentTimeUs = 0;
    private Type mType;

    public static int STATUS_RELEASED = 0x00;
    public static int STATUS_RELEASING = 0x01;
    public static int STATUS_PREPARING = 0x02;
    public static int STATUS_PREPARED = 0x05;
    public static int STATUS_STARTED = 0x06;

    private AtomicInteger mStatus = new AtomicInteger(STATUS_RELEASED);
    private IDecoder mDecoder;
    private IExtractorListener mListener;
    private Object mLock = new Object();

    public SvExtractor() {
    }

    public void setExtractorListener(IExtractorListener listener) {
        synchronized (mLock) {
            mListener = listener;
        }
    }

    public void prepare(String filePath, Type type) {
        if (TextUtils.isEmpty(filePath) || mStatus.get() >= STATUS_PREPARING)
            return;
        if (type == null)
            type = Type.VIDEO;
        mStatus.set(STATUS_PREPARING);
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new ExtractorHandler(thread.getLooper());
        Message prepareMessage = mHandler.obtainMessage(MSG_PREPARE_EXTRACTOR);
        prepareMessage.obj = type;
        Bundle data = new Bundle();
        data.putString("filepath", filePath);
        prepareMessage.setData(data);
        prepareMessage.sendToTarget();
    }

    public void start(IDecoder decoder) {
        synchronized (mLock) {
            mDecoder = decoder;
        }
        if (mStatus.get() < STATUS_PREPARING) {
            return;
        }
        if (!mHandler.hasMessages(MSG_FEED_BUFFER)) {
            mHandler.sendEmptyMessage(MSG_FEED_BUFFER);
        }
        mStatus.set(STATUS_STARTED);
    }

    public void release() {
        if (mStatus.get() <= STATUS_RELEASING)
            return;
        mStatus.set(STATUS_RELEASING);
        mHandler.release();
    }

    public int getStatus() {
        return mStatus.get();
    }

    public MediaFormat getMediaFormat() {
        synchronized (mLock) {
            return mFormat;
        }
    }


    private final int MSG_PREPARE_EXTRACTOR = 0x01;
    private final int MSG_FEED_BUFFER = 0x02;
    private final int MSG_SEEK = 0x03;

    private boolean firstFrame = true;

    class ExtractorHandler extends Handler {

        public ExtractorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PREPARE_EXTRACTOR: {
                    Type type = (Type) msg.obj;
                    Bundle data = msg.getData();
                    String filepath = data.getString("filepath");
                    if (mExtractor != null) {
                        mExtractor._release();
                    }
                    try {
                        if (!TextUtils.isEmpty(filepath)) {
                            mExtractor = new SvMediaExtractor();
                            MediaFormat format = mExtractor._prepare(filepath, type);
                            synchronized (mLock) {
                                mFormat = format;
                            }
                        } else {
                            return;
                        }
                        mStatus.set(STATUS_PREPARED);
                        notifyPrepared();
                    } catch (IOException e) {
                        e.printStackTrace();
                        notifyonError(e);
                    }
                }
                break;
                case MSG_FEED_BUFFER: {
                    if (mStatus.get() < STATUS_PREPARING)
                        break;
                    if (firstFrame) {
                        firstFrame = false;
                    }
                    IDecoder localDecoder = null;
                    synchronized (mLock) {
                        localDecoder = mDecoder;
                    }
                    if (localDecoder != null && localDecoder.isPrepared()) {
                        InputInfo inputInfo = localDecoder.dequeueInputBuffer();
                        if (inputInfo != null && inputInfo.buffer != null) {
                            long time = mExtractor._fillBuffer(inputInfo);
                            inputInfo.lastFrameFlag = time == -1 ? true : false;
                            localDecoder.queueInputBuffer(inputInfo);
                        }
                        if (mStatus.get() >= STATUS_PREPARING) {
                            sendEmptyMessage(MSG_FEED_BUFFER);
                        }
                    }
                }
                break;
                case MSG_SEEK: {
                    if (mStatus.get() < STATUS_PREPARED) {
                        break;
                    }
                    Object tag = msg.obj;
                    long seekTimeUs = 0;
                    if (tag != null) {
                        seekTimeUs = (long) tag;
                    }
                    mExtractor._seekTo(seekTimeUs);
                    IDecoder localDecoder = null;
                    synchronized (mLock) {
                        localDecoder = mDecoder;
                    }
                    if (localDecoder != null && localDecoder.isPrepared()) {
                        localDecoder.flush();
                    }
                }
                break;
            }
        }

        public void release() {
            post(new Runnable() {
                @Override
                public void run() {
                    mExtractor._release();
                    mExtractor = null;
                    notifyReleased();
                    getLooper().quit();
                }
            });
        }
    }

    private void notifyPrepared() {
        synchronized (mLock) {
            if (mListener != null) {
                mListener.onPrepared(SvExtractor.this);
            }
        }
    }

    private void notifyonError(Throwable throwable) {
        synchronized (mLock) {
            if (mListener != null) {
                mListener.onError(SvExtractor.this, throwable);
            }
        }
    }

    private void notifyReleased() {
        synchronized (mLock) {
            if (mListener != null) {
                mListener.onReleased(SvExtractor.this);
            }
        }
    }
}
