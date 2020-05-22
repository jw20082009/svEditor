package com.wilbert.sveditor.library.codecs.abs;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/19
 * desc   :
 */
public interface IVideoParams {

    long getDuration();

    int getWidth();

    int getHeight();

    int getRotation();

    int getFps();

    int getBitrate();
}
