package com.wilbert.sveditor.library.contexts.abs;

/**
 * author : Administrator
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public interface ITimeline {

    long start();

    long pause();

    long stop();

    long seekTo(long timeUs);

    boolean isPlaying();

    long compareTime(long timeUs);

    long getCurrentTime();
}
