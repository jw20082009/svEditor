package com.wilbert.sveditor.library.codecs;

import android.media.MediaFormat;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import com.wilbert.sveditor.library.codecs.abs.FrameInfo;
import com.wilbert.sveditor.library.codecs.abs.IDecoder;
import com.wilbert.sveditor.library.codecs.abs.IDecoderListener;
import com.wilbert.sveditor.library.codecs.abs.InputInfo;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/22
 * desc   :
 */
public class SvDecoder {
    private final String TAG = "SvDecoder";

    public static int STATUS_RELEASED = 0x00;
    public static int STATUS_RELEASING = 0x01;
    public static int STATUS_PREPARING_DECODER = 0x04;
    public static int STATUS_PREPARED = 0x05;

    private AtomicInteger mStatus = new AtomicInteger(STATUS_RELEASED);
    private Object mLock = new Object();

    private IDecoder mDecoder;
    private IDecoderListener mListener;
    private MediaFormat mFormat;
    private LinkedBlockingDeque<InputInfo> mInputBuffers = new LinkedBlockingDeque<>(10);
    private LinkedBlockingDeque<FrameInfo> mOutputBuffers = new LinkedBlockingDeque<>(10);
    private DecodeHandler mHandler;

    public SvDecoder() {
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mHandler = new DecodeHandler(thread.getLooper());
        mHandler.sendEmptyMessage(MSG_PREPARE_DECODER);
    }

    public int getStatus() {
        return mStatus.get();
    }

    public boolean isPrepared() {
        return mStatus.get() >= STATUS_PREPARED;
    }

    public boolean prepare(MediaFormat format) throws IOException {
        if (mStatus.get() >= STATUS_PREPARING_DECODER)
            return true;
        synchronized (mLock) {
            mStatus.set(STATUS_PREPARING_DECODER);
            mFormat = format;
            mLock.notifyAll();
            return true;
        }
    }

    public InputInfo dequeueInputBuffer() {
        if (mDecoder == null) {
            return null;
        }
        InputInfo result = null;
        try {
            result = mInputBuffers.takeLast();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void queueInputBuffer(InputInfo inputInfo) {
        if (mDecoder == null) {
            return;
        }
        if (inputInfo != null) {
            mDecoder.queueInputBuffer(inputInfo);
        } else {
            mInputBuffers.offerFirst(new InputInfo(-1, null));
        }
    }

    public FrameInfo dequeueOutputBuffer() {
        if (mDecoder == null) {
            return null;
        }
        FrameInfo result = null;
        try {
            result = mOutputBuffers.takeLast();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void releaseOutputBuffer(FrameInfo frameInfo) {
        if (mDecoder == null) {
            return;
        }
        if (frameInfo != null) {
            mDecoder.releaseOutputBuffer(frameInfo);
        } else {
            mOutputBuffers.offerFirst(new FrameInfo(null, -1, -1, -1));
        }
    }

    void offerInputInfo(InputInfo inputInfo) {
        mInputBuffers.offerFirst(inputInfo == null ? new InputInfo(-1, null) : inputInfo);
    }

    void offerFrameInfo(FrameInfo frameInfo) {
        mOutputBuffers.offerFirst(frameInfo == null ? new FrameInfo(null, -1, -1, -1) : frameInfo);
    }

    public boolean flush() {
        if (mStatus.get() <= STATUS_PREPARED) {
            return false;
        }
        mInputBuffers.clear();
        mOutputBuffers.clear();
        return true;
    }

    public void release() {
        if (mStatus.get() <= STATUS_RELEASING)
            return;
        mStatus.set(STATUS_RELEASING);
        mHandler.release();
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
                        mDecoder = new SvMediaDecoder(SvDecoder.this);
                        if (mFormat == null) {
                            synchronized (mLock) {
                                mLock.wait();
                            }
                        }
                        if (mFormat != null) {
                            mDecoder.prepare(mFormat);
                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                        notifyonError(e);
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
                        mDecoder = null;
                    }
                    mStatus.set(STATUS_RELEASED);
                    notifyReleased();
                    getLooper().quit();
                }
            });
        }
    }

    public void setListener(IDecoderListener listener) {
        synchronized (mLock) {
            mListener = listener;
        }
    }

    private void notifyPrepared() {
        synchronized (mLock) {
            if (mListener != null) {
                mListener.onPrepared(SvDecoder.this);
            }
        }
    }

    private void notifyonError(Throwable throwable) {
        synchronized (mLock) {
            if (mListener != null) {
                mListener.onError(SvDecoder.this, throwable);
            }
        }
    }

    private void notifyReleased() {
        synchronized (mLock) {
            if (mListener != null) {
                mListener.onReleased(SvDecoder.this);
            }
        }
    }
}
