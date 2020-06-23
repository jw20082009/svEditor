package com.wilbert.sveditor.library.codecs.abs;

import android.media.MediaFormat;

import java.io.IOException;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/09
 * desc   :
 */
public interface IDecoder {

    int STATUS_RELEASED = 0X00;
    int STATUS_RELEASING = 0X01;
    int STATUS_PREPARING = 0X02;
    int STATUS_PREPARED = 0x03;

    boolean isPrepared();

    boolean prepare(MediaFormat format) throws IOException;

    InputInfo dequeueInputBuffer();

    void queueInputBuffer(InputInfo inputInfo);

    FrameInfo dequeueOutputBuffer();

    void queueOutputBuffer(FrameInfo frameInfo);

    int getStatus();

    boolean flush();

    void release();
}
