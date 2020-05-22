package com.wilbert.sveditor.library.codecs;

import android.media.MediaFormat;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.wilbert.sveditor.library.codecs.abs.FrameInfo;
import com.wilbert.sveditor.library.codecs.abs.IDecoder;
import com.wilbert.sveditor.library.codecs.abs.InputInfo;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/22
 * desc   :
 */
public class SvDecoder implements IDecoder {

    IDecoder mDecoder;

    public static int STATUS_RELEASED = 0x00;
    public static int STATUS_RELEASING = 0x01;
    public static int STATUS_PREPARING = 0x02;
    public static int STATUS_PREPARING_EXTRACTOR = 0x03;
    public static int STATUS_PREPARING_DECODER = 0x04;
    public static int STATUS_PREPARED = 0x05;
    public static int STATUS_STARTING = 0x06;
    public static int STATUS_STARTED = 0x07;

    private AtomicInteger mStatus = new AtomicInteger(STATUS_RELEASED);


    public SvDecoder(Object lock) {
        mDecoder = new SvMediaDecoder();
    }

    @Override
    public int getStatus() {
        return mStatus.get();
    }

    @Override
    public boolean prepare(MediaFormat format, Object lock) throws IOException {
        return false;
    }

    @Override
    public InputInfo dequeueInputBuffer() {
        return null;
    }

    @Override
    public void queueInputBuffer(InputInfo inputInfo) {

    }

    @Override
    public FrameInfo dequeueOutputBuffer() {
        return null;
    }

    @Override
    public void queueOutputBuffer(FrameInfo frameInfo) {

    }

    @Override
    public boolean flush() {
        return false;
    }

    @Override
    public void release() {

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
}
