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

    void queueInputBuffer(InputInfo inputInfo);

    void releaseOutputBuffer(FrameInfo frameInfo);

    boolean flush();

    void release();
}
