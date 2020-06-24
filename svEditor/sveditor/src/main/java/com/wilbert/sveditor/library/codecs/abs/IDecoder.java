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

    boolean isPrepared();

    boolean prepare(MediaFormat format) throws IOException;

    InputInfo dequeueInputBuffer();

    void queueInputBuffer(InputInfo inputInfo);

    FrameInfo dequeueOutputBuffer();

    void releaseOutputBuffer(FrameInfo frameInfo);

    int getStatus();

    boolean flush();

    void release();
}
