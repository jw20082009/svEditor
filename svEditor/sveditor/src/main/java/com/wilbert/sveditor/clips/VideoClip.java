package com.wilbert.sveditor.clips;

import android.text.TextUtils;

import com.wilbert.sveditor.clips.abs.IFrameWorker;
import com.wilbert.sveditor.clips.abs.IPreparedListener;
import com.wilbert.sveditor.clips.abs.IVideoClip;
import com.wilbert.sveditor.codecs.SvMediaExtractor;
import com.wilbert.sveditor.codecs.SvMediaExtractorWrapper;
import com.wilbert.sveditor.codecs.abs.FrameInfo;
import com.wilbert.sveditor.codecs.abs.IExtractorListener;
import com.wilbert.sveditor.codecs.abs.IVideoParams;
import com.wilbert.sveditor.log.ALog;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * author : Administrator
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class VideoClip implements IVideoClip, IVideoParams, IFrameWorker {
    private final String TAG = "VideoClip" + hashCode();
    private SvMediaExtractorWrapper mExtractor;
    private String mFilePath = null;

    public static final int STATUS_RELEASED = 0x00;
    public static final int STATUS_RELEASING = 0X01;
    public static final int STATUS_PREPARING = 0x02;
    public static final int STATUS_IDLE = 0x03;
    public static final int STATUS_PREPARED = 0x04;

    private AtomicInteger mStatus = new AtomicInteger(0);
    private IPreparedListener mPreparedListener;
    private Object mLock = new Object();

    public VideoClip() {
    }

    @Override
    public int getWidth() {
        if (mStatus.get() < STATUS_PREPARED)
            return 0;
        return mExtractor.getWidth();
    }

    @Override
    public int getHeight() {
        if (mStatus.get() < STATUS_PREPARED)
            return 0;
        return mExtractor.getHeight();
    }

    @Override
    public int getRotation() {
        if (mStatus.get() < STATUS_PREPARED)
            return 0;
        return mExtractor.getRotation();
    }

    @Override
    public int getFps() {
        if (mStatus.get() < STATUS_PREPARED)
            return 0;
        return mExtractor.getFps();
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
    public void prepare(String filePath) {
        if (TextUtils.isEmpty(filePath) || mStatus.get() >= STATUS_PREPARING) {
            return;
        }
        mStatus.set(STATUS_PREPARING);
        if (mExtractor != null) {
            mExtractor.release();
        }
        mFilePath = filePath;
        mExtractor = new SvMediaExtractorWrapper();
        mExtractor.setListener(extractorListener);
        mExtractor.prepare(mFilePath, SvMediaExtractor.Type.VIDEO);
        mStatus.set(STATUS_IDLE);
    }

    @Override
    public void setOnPreparedListener(IPreparedListener listener) {
        synchronized (mLock) {
            this.mPreparedListener = listener;
        }
    }

    @Override
    public void seekTo(long timeUs) {
        if (mStatus.get() < STATUS_PREPARED) {
            ALog.i(TAG, "seekTo:" + timeUs);
            return;
        }
        mExtractor.seekTo(timeUs);
    }

    @Override
    public void speedUp(float speed) {

    }

    @Override
    public String getFilepath() {
        synchronized (mLock) {
            return mFilePath;
        }
    }

    @Override
    public FrameInfo getNextFrame() {
        if (mStatus.get() < STATUS_IDLE)
            return null;
        return mExtractor.getNextFrameBuffer();
    }

    @Override
    public void releaseFrame(FrameInfo frameInfo) {
        if (mStatus.get() < STATUS_IDLE) {
            ALog.i(TAG, "releaseFrame when not prepared");
            return;
        }
        mExtractor.releaseFrameBuffer(frameInfo);
    }

    @Override
    public void release() {
        if (mStatus.get() <= STATUS_RELEASING)
            return;
        mStatus.set(STATUS_RELEASING);
        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
        }
    }

    @Override
    public String toString() {
        return "VideoClip[" + hashCode() + "]";
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
