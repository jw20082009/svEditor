package com.wilbert.sveditor.contexts;

import android.os.SystemClock;

import com.wilbert.sveditor.contexts.abs.ITimeline;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/13
 * desc   :
 */
public class Timeline implements ITimeline {

    private long mStartTime = -1;
    private long mPauseTime = -1;
    private Object mLock = new Object();

    @Override
    public long start() {
        synchronized (mLock) {
            long currentTime = SystemClock.elapsedRealtimeNanos();
            long pts = currentTime - mStartTime;
            if (mStartTime == -1) {
                pts = 0;
                mStartTime = currentTime;
            } else if (mPauseTime > mStartTime) {
                pts = mPauseTime - mStartTime;
                mStartTime = currentTime - pts;
            }
            mPauseTime = -1;
            return pts / 1000;
        }
    }

    @Override
    public long pause() {
        synchronized (mLock) {
            long currentTime = SystemClock.elapsedRealtimeNanos();
            long pts = 0;
            if (mPauseTime <= 0) {
                pts = currentTime - mStartTime;
            } else {
                pts = mPauseTime - mStartTime;
                mStartTime = currentTime - pts;
            }
            mPauseTime = currentTime;
            return pts / 1000;
        }
    }

    @Override
    public long stop() {
        synchronized (mLock) {
            mStartTime = -1;
            mPauseTime = -1;
            return 0;
        }
    }

    @Override
    public long seekTo(long timeUs) {
        synchronized (mLock) {
            if (timeUs < 0)
                timeUs = 0;
            long currentTime = SystemClock.elapsedRealtimeNanos();
            long pausedPts = 0;
            if (mPauseTime > mStartTime) {
                pausedPts = mPauseTime - mStartTime;
            }
            mStartTime = currentTime - timeUs * 1000;
            if (pausedPts > 0) {
                mPauseTime = mStartTime + pausedPts;
            } else {
                mPauseTime = -1;
            }
            return timeUs;
        }
    }

    @Override
    public boolean isPlaying() {
        synchronized (mLock) {
            return mPauseTime <= mStartTime;
        }
    }

    @Override
    public long compareTime(long timeUs) {
        synchronized (mLock) {
            if (timeUs < 0)
                timeUs = 0;
            long result = 0;
            if (mPauseTime <= 0) {
                long currentTime = SystemClock.elapsedRealtimeNanos();
                result = timeUs * 1000 + mStartTime - currentTime;
            } else {
                result = timeUs * 1000 + mStartTime - mPauseTime;
            }
            return result / 1000;
        }
    }

    @Override
    public long getCurrentTime() {
        synchronized (mLock) {
            long pts = 0;
            if (mPauseTime <= 0) {
                pts = SystemClock.elapsedRealtimeNanos() - mStartTime;
            } else {
                pts = mPauseTime - mStartTime;
            }
            return pts / 1000;
        }
    }
}
