package com.wilbert.sveditor.library.contexts.abs;

import android.opengl.GLSurfaceView;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/19
 * desc   :
 */
public interface IPlayer {

    void setDataSource(String filepath);

    void prepare(GLSurfaceView surfaceView);

    void setOnPreparedListener(IPrepareListener listener);

    long getDuration();

    long getCurrentTimeUs();

    boolean isPlaying();

    void start();

    void pause();

    void seekTo(long timeUs);

    void release();
}
