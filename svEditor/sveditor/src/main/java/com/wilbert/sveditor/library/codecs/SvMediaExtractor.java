package com.wilbert.sveditor.library.codecs;

import android.media.MediaFormat;
import android.text.TextUtils;

import com.wilbert.sveditor.library.codecs.abs.IExtractor;
import com.wilbert.sveditor.library.codecs.abs.IExtractorListener;
import com.wilbert.sveditor.library.codecs.abs.InputInfo;
import com.wilbert.sveditor.library.log.ALog;

import java.io.IOException;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/22
 * desc   :
 */
public class SvMediaExtractor implements IExtractor {
    private final String TAG = "SvMediaExtractor";
    private long mCurrentTimeUs = 0;
    private boolean mPrepared = false;
    private android.media.MediaExtractor mExtractor;
    private MediaFormat mFormat;
    private final long mIgnoreTimeUs = 40000;//40ms

    @Override
    public boolean _prepare(String filepath, SvExtractor.Type type) throws IOException {
        if (TextUtils.isEmpty(filepath)) {
            throw new IOException("cannot prepare empty filepath");
        }
        mExtractor = new android.media.MediaExtractor();
        mExtractor.setDataSource(filepath);
        for (int i = 0; i < this.mExtractor.getTrackCount(); ++i) {
            MediaFormat format = this.mExtractor.getTrackFormat(i);
            String mime = format.getString(MediaFormat.KEY_MIME);
            if ((type == SvExtractor.Type.VIDEO && mime.startsWith("video/")) || (type == SvExtractor.Type.AUDIO && mime.startsWith("audio/"))) {
                mExtractor.selectTrack(i);
                mFormat = format;
                break;
            }
        }
        if (mFormat != null) {
            mPrepared = true;
            mCurrentTimeUs = 0;
        } else {
            _release();
            mPrepared = false;
        }
        return mPrepared;
    }

    @Override
    public long _fillBuffer(InputInfo buffer) {
        if (buffer == null || !mPrepared) {
            ALog.i(TAG, "fillBuffer when " + (buffer == null ? "buffer is null" : "buffer not null") + "; mPrepared:" + mPrepared);
            return -1;
        }
        int size = mExtractor.readSampleData(buffer.buffer, 0);
        long time = mExtractor.getSampleTime();
        buffer.time = time;
        buffer.size = size;
        if (size > 0) {
            mExtractor.advance();
            mCurrentTimeUs = mExtractor.getSampleTime();
        } else {
            time = -1;
            buffer.lastFrameFlag = true;
        }
        return time;
    }

    @Override
    public long _seekTo(long timeUs) {
        if (!mPrepared || Math.abs(timeUs - mCurrentTimeUs) < mIgnoreTimeUs) {
            return mCurrentTimeUs;
        }
        long time = mCurrentTimeUs;
        int retryTimes = 10;
        do {
            mExtractor.seekTo(timeUs, android.media.MediaExtractor.SEEK_TO_CLOSEST_SYNC);
            time = mExtractor.getSampleTime();
        } while (time < 0 && --retryTimes > 0);
        mCurrentTimeUs = time;
        return mCurrentTimeUs;
    }

    @Override
    public void _release() {
        if (mExtractor != null) {
            mExtractor.release();
            mExtractor = null;
            mPrepared = false;
        }
    }
}
