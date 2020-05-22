package com.wilbert.sveditor.library.clips;

import android.text.TextUtils;

import com.wilbert.sveditor.library.clips.abs.IAudioClip;
import com.wilbert.sveditor.library.clips.abs.IFrameWorker;
import com.wilbert.sveditor.library.clips.abs.IPreparedListener;
import com.wilbert.sveditor.library.codecs.SvExtractor;
import com.wilbert.sveditor.library.codecs.SvMediaExtractorWrapper;
import com.wilbert.sveditor.library.codecs.abs.FrameInfo;
import com.wilbert.sveditor.library.codecs.abs.IAudioParams;
import com.wilbert.sveditor.library.codecs.abs.IExtractorListener;
import com.wilbert.sveditor.library.log.ALog;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * author : Administrator
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class AudioClip implements IAudioClip, IAudioParams, IFrameWorker {
    private final String TAG = "AudioClip";

    public static final int STATUS_RELEASED = 0x00;
    public static final int STATUS_RELEASING = 0X01;
    public static final int STATUS_PREPARING = 0x02;
    public static final int STATUS_IDLE = 0x03;
    public static final int STATUS_PREPARED = 0x04;

    private SvMediaExtractorWrapper mExtractor;
    private IPreparedListener mPreparedListener;
    private String mFilePath;
    private Object mLock = new Object();

    private AtomicInteger mStatus = new AtomicInteger(0);

    public AudioClip() {
    }

    @Override
    public void prepare(String filepath) {
        if (TextUtils.isEmpty(filepath) || mStatus.get() >= STATUS_PREPARING) {
            return;
        }
        mStatus.set(STATUS_PREPARING);
        if (mExtractor != null) {
            mExtractor.release();
        }
        synchronized (mLock) {
            this.mFilePath = filepath;
            mExtractor = new SvMediaExtractorWrapper();
            mExtractor.setListener(extractorListener);
            mExtractor.prepare(mFilePath, SvExtractor.Type.AUDIO);
        }
        mStatus.set(STATUS_IDLE);
    }

    @Override
    public void setOnPreparedListener(IPreparedListener listener) {
        synchronized (mLock) {
            this.mPreparedListener = listener;
        }
    }

    @Override
    public String getFilepath() {
        synchronized (mLock) {
            return mFilePath;
        }
    }

    @Override
    public int getChannels() {
        if (mStatus.get() < STATUS_PREPARED)
            return 0;
        return mExtractor.getChannels();
    }

    @Override
    public int getSampleRate() {
        if (mStatus.get() < STATUS_PREPARED)
            return 0;
        return mExtractor.getSampleRate();
    }

    @Override
    public long getDuration() {
        if (mStatus.get() < STATUS_PREPARED)
            return 0;
        return mExtractor.getDuration();
    }

    @Override
    public int getBitrate() {
        if (mStatus.get() < STATUS_PREPARED)
            return 0;
        return mExtractor.getBitrate();
    }

    @Override
    public FrameInfo getNextFrame() {
        if (mStatus.get() < STATUS_IDLE)
            return null;
        return mExtractor.getNextFrameBuffer();
    }

    @Override
    public void releaseFrame(FrameInfo frameInfo) {
        if (mStatus.get() < STATUS_PREPARING) {
            ALog.i(TAG, "releaseFrame when not prepared");
            return;
        }
        mExtractor.releaseFrameBuffer(frameInfo);
    }

    @Override
    public void release() {
        if (mStatus.get() < STATUS_PREPARED) {
            return;
        }
        mStatus.set(STATUS_RELEASING);
        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }
    }

    public IExtractorListener extractorListener = new IExtractorListener() {

        @Override
        public void onPrepared(SvMediaExtractorWrapper extractor) {
            mStatus.set(STATUS_PREPARED);
            mExtractor.start();
            synchronized (mLock) {
                if (mPreparedListener != null) {
                    mPreparedListener.onPrepared(extractor);
                }
            }
        }

        @Override
        public void onReleased(SvMediaExtractorWrapper extractor) {
            mStatus.set(STATUS_RELEASED);
        }

        @Override
        public void onError(SvMediaExtractorWrapper extractor, Throwable throwable) {

        }
    };
}
