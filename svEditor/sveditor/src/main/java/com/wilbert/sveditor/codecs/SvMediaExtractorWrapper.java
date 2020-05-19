package com.wilbert.sveditor.codecs;

import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;

import com.wilbert.sveditor.codecs.abs.FrameInfo;
import com.wilbert.sveditor.codecs.abs.IAudioParams;
import com.wilbert.sveditor.codecs.abs.IExtractor;
import com.wilbert.sveditor.codecs.abs.IExtractorListener;
import com.wilbert.sveditor.codecs.abs.IVideoParams;
import com.wilbert.sveditor.codecs.abs.InputInfo;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class SvMediaExtractorWrapper implements IExtractor, IAudioParams, IVideoParams {
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

    private SvMediaExtractor mExtractor;
    private MediaFormat mFormat;
    private ExtractorHandler mHandler;
    private DecodeHandler mDecodeHandler;
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

    private SvMediaDecoder mDecoder;
    private Semaphore mReleasePhore = new Semaphore(2);
    private Semaphore mDecodePhore = new Semaphore(0);
    private Object mLock = new Object();

    public SvMediaExtractorWrapper() {
    }

    @Override
    public void prepare(String filePath, SvMediaExtractor.Type type) {
        if (mStatus.get() >= STATUS_PREPARING)
            return;
        mStatus.set(STATUS_PREPARING);
        initHandler();
        mFilePath = filePath;
        mHandler.removeMessages(MSG_PREPARE_EXTRACTOR);
        Message prepareMessage = mHandler.obtainMessage(MSG_PREPARE_EXTRACTOR);
        prepareMessage.obj = type;
        prepareMessage.sendToTarget();
        mDecodeHandler.removeMessages(MSG_PREPARE_DECODER);
        Message decodeMessage = mDecodeHandler.obtainMessage(MSG_PREPARE_DECODER);
        decodeMessage.sendToTarget();
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
        if (mStatus.get() < STATUS_RELEASING)
            return;
        mStatus.set(STATUS_RELEASING);
        mHandler.release();
        mDecodeHandler.release();
    }

    private void initHandler() {
        if (mHandler == null) {
            HandlerThread thread = new HandlerThread("VideoExtractorWrapper[" + hashCode() + "]");
            thread.start();
            mHandler = new ExtractorHandler(thread.getLooper());
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

    private final int MSG_PREPARE_EXTRACTOR = 0x01;
    private final int MSG_FEED_BUFFER = 0x02;
    private final int MSG_SEEK = 0x03;

    private boolean firstFrame = true;

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

    class ExtractorHandler extends Handler {

        public ExtractorHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_PREPARE_EXTRACTOR:
                    if (mExtractor != null) {
                        mExtractor.release();
                    }
                    try {
                        mExtractor = new SvMediaExtractor();
                        mStatus.set(STATUS_PREPARING_EXTRACTOR);
                        if (!TextUtils.isEmpty(mFilePath)) {
                            mExtractor.prepare(mFilePath, (SvMediaExtractor.Type) msg.obj);
                        } else {
                            return;
                        }
                        try {
                            synchronized (mLock) {
                                mFormat = mExtractor.getMediaFormat();
                                if (mFormat != null) {
                                    if (mFormat.containsKey(MediaFormat.KEY_WIDTH))
                                        mWidth = mFormat.getInteger(MediaFormat.KEY_WIDTH);
                                    if (mFormat.containsKey(MediaFormat.KEY_HEIGHT))
                                        mHeight = mFormat.getInteger(MediaFormat.KEY_HEIGHT);
                                    if (mFormat.containsKey(MediaFormat.KEY_ROTATION))
                                        mRotation = mFormat.getInteger(MediaFormat.KEY_ROTATION);
                                    if (mFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                                        mChannelCount = mFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                                    if (mFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                                        mSampleRate = mFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                                    if (mFormat.containsKey(MediaFormat.KEY_FRAME_RATE))
                                        mFps = mFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                                    if (mFormat.containsKey(MediaFormat.KEY_BIT_RATE))
                                        mBitRate = mFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                                    if (mFormat.containsKey(MediaFormat.KEY_DURATION))
                                        mDuration = mFormat.getLong(MediaFormat.KEY_DURATION);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        mDecodePhore.release(1);
                    } catch (IOException e) {
                        e.printStackTrace();
                        notifyonError(e);
                    }
                    break;
                case MSG_FEED_BUFFER:
                    if (mStatus.get() < STATUS_PREPARED)
                        break;
                    if (firstFrame) {
                        firstFrame = false;
                    }
                    InputInfo inputInfo = mDecoder.dequeueInputBuffer();
                    if (inputInfo != null && inputInfo.buffer != null) {
                        long time = mExtractor.fillBuffer(inputInfo);
                        inputInfo.lastFrameFlag = time == -1 ? true : false;
                        mDecoder.queueInputBuffer(inputInfo);
                    }
                    if (mStatus.get() >= STATUS_PREPARED) {
                        sendEmptyMessage(MSG_FEED_BUFFER);
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
                    mExtractor.seekTo(seekTimeUs);
                    mDecoder.flush();
                }
                break;
            }
        }

        public void release() {
            mDecoder.queueInputBuffer(null);
            mDecoder.queueOutputBuffer(null);
            post(new Runnable() {
                @Override
                public void run() {
                    _release();
                    getLooper().quit();
                }
            });
        }
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
}

