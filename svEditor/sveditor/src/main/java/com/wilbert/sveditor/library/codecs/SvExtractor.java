package com.wilbert.sveditor.library.codecs;

import android.media.MediaFormat;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.wilbert.sveditor.library.codecs.abs.FrameInfo;
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
    private SvDecoder mDecoder;
    private IExtractorListener mListener;
    private Object mLock = new Object();

    private int mFps = 0;
    private int mBitRate = 0;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mRotation = 0;
    private int mChannelCount = 0;
    private int mSampleRate = 0;
    private long mDuration = 0;

    public SvExtractor() {
        synchronized (mLock) {
            mDecoder = new SvDecoder();
        }
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

    public void start() {
        if (mStatus.get() < STATUS_PREPARING) {
            return;
        }
        if (!mHandler.hasMessages(MSG_FEED_BUFFER)) {
            mHandler.sendEmptyMessage(MSG_FEED_BUFFER);
        }
        mStatus.set(STATUS_STARTED);
    }

    public FrameInfo getNextFrameBuffer() {
        if (mStatus.get() < STATUS_PREPARING_DECODER)
            return null;
        FrameInfo frameInfo = mDecoder.dequeueOutputBuffer();
        if (!mHandler.hasMessages(MSG_FEED_BUFFER)) {
            mHandler.sendEmptyMessage(MSG_FEED_BUFFER);
        }
        return frameInfo;
    }

    public void releaseFrameBuffer(FrameInfo frameInfo) {
        if (mStatus.get() < STATUS_PREPARED || frameInfo == null)
            return;
        mDecoder.releaseOutputBuffer(frameInfo);
    }

    public void seekTo(long timeUs) {
        if (mStatus.get() < STATUS_PREPARED) {
            return;
        }
        mHandler.removeMessages(MSG_FEED_BUFFER);
        mHandler.removeMessages(MSG_SEEK);
        Message seekMessage = mHandler.obtainMessage(MSG_SEEK);
        seekMessage.obj = timeUs;
        seekMessage.sendToTarget();
        mHandler.removeMessages(MSG_FEED_BUFFER);
        mHandler.sendEmptyMessage(MSG_FEED_BUFFER);
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
                                initParams(mFormat);
                                mDecoder.prepare(format);
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
                    SvDecoder localDecoder = null;
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
                    SvDecoder localDecoder = null;
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

    private void initParams(MediaFormat format) {
        if (format == null)
            return;
        if (format.containsKey(MediaFormat.KEY_WIDTH))
            mWidth = format.getInteger(MediaFormat.KEY_WIDTH);
        if (format.containsKey(MediaFormat.KEY_HEIGHT))
            mHeight = format.getInteger(MediaFormat.KEY_HEIGHT);
        if (format.containsKey(MediaFormat.KEY_ROTATION))
            mRotation = format.getInteger(MediaFormat.KEY_ROTATION);
        if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
            mChannelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
        if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE))
            mSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
        if (format.containsKey(MediaFormat.KEY_FRAME_RATE))
            mFps = format.getInteger(MediaFormat.KEY_FRAME_RATE);
        if (format.containsKey(MediaFormat.KEY_BIT_RATE))
            mBitRate = format.getInteger(MediaFormat.KEY_BIT_RATE);
        if (format.containsKey(MediaFormat.KEY_DURATION))
            mDuration = format.getLong(MediaFormat.KEY_DURATION);
    }
}
