package com.wilbert.sveditor.library.codecs;

import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.wilbert.sveditor.library.codecs.abs.FrameInfo;
import com.wilbert.sveditor.library.codecs.abs.IAudioParams;
import com.wilbert.sveditor.library.codecs.abs.IMediaExtractor;
import com.wilbert.sveditor.library.codecs.abs.IExtractorListener;
import com.wilbert.sveditor.library.codecs.abs.IVideoParams;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class SvMediaExtractorWrapper implements IMediaExtractor, IAudioParams, IVideoParams {
    private final String TAG = "SvMediaExtractorWrapper";

    public static int STATUS_RELEASED = 0x00;
    public static int STATUS_RELEASING = 0x01;
    public static int STATUS_PREPARING = 0x02;
    public static int STATUS_PREPARING_EXTRACTOR = 0x03;
    public static int STATUS_PREPARING_DECODER = 0x04;
    public static int STATUS_PREPARED = 0x05;
    public static int STATUS_STARTING = 0x06;
    public static int STATUS_STARTED = 0x07;

    private AtomicInteger mStatus = new AtomicInteger(STATUS_RELEASED);

    private IExtractorListener mListener;
    private String mFilePath = null;

    private int mFps = 0;
    private int mBitRate = 0;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mRotation = 0;
    private int mChannelCount = 0;
    private int mSampleRate = 0;
    private long mDuration = 0;

    private SvDecoder mDecoder;
    private SvExtractor mExtractor;
    private Semaphore mReleasePhore = new Semaphore(2);
    private Semaphore mDecodePhore = new Semaphore(0);
    private Object mLock = new Object();

    public SvMediaExtractorWrapper() {
        mExtractor = new SvExtractor();
        mExtractor.setExtractorListener(mExtractorListener);

    }

    @Override
    public void prepare(String filePath, SvExtractor.Type type) {
        if (mStatus.get() >= STATUS_PREPARING)
            return;
        mStatus.set(STATUS_PREPARING);
        initHandler();
        mFilePath = filePath;
        mExtractor.prepare(filePath, type);
    }

    @Override
    public void start() {
        if (mStatus.get() == STATUS_STARTING || mStatus.get() < STATUS_PREPARING)
            return;
        mStatus.set(STATUS_STARTING);
        initHandler();
        if (!mHandler.hasMessages(MSG_FEED_BUFFER)) {
            mHandler.sendEmptyMessage(MSG_FEED_BUFFER);
        }
        mStatus.set(STATUS_STARTED);
    }

    @Override
    public FrameInfo getNextFrameBuffer() {
        if (mStatus.get() < STATUS_PREPARING_DECODER)
            return null;
        FrameInfo frameInfo = mDecoder.dequeueOutputBuffer();
        if (!mHandler.hasMessages(MSG_FEED_BUFFER)) {
            mHandler.sendEmptyMessage(MSG_FEED_BUFFER);
        }
        return frameInfo;
    }

    @Override
    public void releaseFrameBuffer(FrameInfo frameInfo) {
        if (mStatus.get() < STATUS_PREPARED || frameInfo == null)
            return;
        mDecoder.queueOutputBuffer(frameInfo);
    }

    @Override
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

    @Override
    public long getDuration() {
        long result = mDuration;
        if (mDecoder != null) {
            result = mDecoder.getDuration();
        }
        return result > 0 ? result : mDuration;
    }

    @Override
    public int getWidth() {
        int result = mWidth;
        if (mDecoder != null) {
            result = mDecoder.getWidth();
        }
        return result > 0 ? result : mWidth;
    }

    @Override
    public int getHeight() {
        int result = mHeight;
        if (mDecoder != null) {
            result = mDecoder.getHeight();
        }
        return result > 0 ? result : mHeight;
    }

    @Override
    public int getRotation() {
        int result = mRotation;
        if (mDecoder != null) {
            result = mDecoder.getRotation();
        }
        return result > 0 ? result : mRotation;
    }

    @Override
    public int getFps() {
        int result = mFps;
        if (mDecoder != null) {
            result = mDecoder.getFps();
        }
        return result > 0 ? result : mFps;
    }

    @Override
    public MediaFormat getMediaFormat() {
        if (mStatus.get() < STATUS_PREPARED) {
            return null;
        }
        return mFormat;
    }

    @Override
    public void setListener(IExtractorListener listener) {
        synchronized (mLock) {
            this.mListener = listener;
        }
    }

    @Override
    public void release() {
        if (mStatus.get() <= STATUS_RELEASING)
            return;
        mStatus.set(STATUS_RELEASING);
        mDecoder.queueInputBuffer(null);
        mDecoder.queueOutputBuffer(null);
        mDecodeHandler.release();
    }

    private void initHandler() {
        if (mDecodeHandler == null) {
            HandlerThread thread1 = new HandlerThread("VideoDecoder[" + hashCode() + "]");
            thread1.start();
            mDecodeHandler = new DecodeHandler(thread1.getLooper());
        }
    }

    private void _release() {
        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
            mReleasePhore.release(1);
            if (mReleasePhore.availablePermits() <= 0) {
                mStatus.set(STATUS_RELEASED);
                notifyReleased();
            }
        }
    }

    @Override
    public int getBitrate() {
        int result = mBitRate;
        if (mDecoder != null) {
            result = mDecoder.getBitrate();
        }
        return result > 0 ? result : mBitRate;
    }

    @Override
    public int getSampleRate() {
        int result = mSampleRate;
        if (mDecoder != null) {
            result = mDecoder.getSampleRate();
        }
        return result > 0 ? result : mSampleRate;
    }

    @Override
    public int getChannels() {
        int result = mChannelCount;
        if (mDecoder != null) {
            result = mDecoder.getChannels();
        }
        return result > 0 ? result : mChannelCount;
    }


    private final int MSG_PREPARE_DECODER = 0x01;

    class DecodeHandler extends Handler {

        public DecodeHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PREPARE_DECODER:
                    try {
                        mDecoder = new SvMediaDecoder();
                        mStatus.set(STATUS_PREPARING_DECODER);
                        mDecodePhore.acquire();
                        if (mFormat != null) {
                            mDecoder.prepare(mFormat);
                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }
                    mStatus.set(STATUS_PREPARED);
                    notifyPrepared();
                    break;
            }
        }

        public void release() {
            post(new Runnable() {
                @Override
                public void run() {
                    if (mDecoder != null) {
                        mDecoder.release();
                        mReleasePhore.release(1);
                        if (mReleasePhore.availablePermits() <= 0) {
                            mStatus.set(STATUS_RELEASED);
                            notifyReleased();
                        }
                        mDecoder = null;
                    }
                    getLooper().quit();
                }
            });
        }
    }

    private void notifyPrepared() {
        synchronized (mLock) {
            if (mListener != null) {
                mListener.onPrepared(SvMediaExtractorWrapper.this);
            }
        }
    }

    private void notifyonError(Throwable throwable) {
        synchronized (mLock) {
            if (mListener != null) {
                mListener.onError(SvMediaExtractorWrapper.this, throwable);
            }
        }
    }

    private void notifyReleased() {
        synchronized (mLock) {
            if (mListener != null) {
                mListener.onReleased(SvMediaExtractorWrapper.this);
            }
        }
    }

    IExtractorListener mExtractorListener = new IExtractorListener() {
        @Override
        public void onPrepared(SvExtractor extractor) {
            MediaFormat format = extractor.getMediaFormat();
            try {
                mDecoder.prepare(format);
                initParams(format);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onReleased(SvExtractor extractor) {
        }

        @Override
        public void onError(SvExtractor extractor, Throwable throwable) {
        }
    };


}

