package com.wilbert.sveditor.library.codecs.abs;

import android.media.MediaFormat;

import com.wilbert.sveditor.library.codecs.SvExtractor;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/09
 * desc   :
 */
public interface IMediaExtractor {
    void prepare(String filePath, SvExtractor.Type type);

    void start();

    FrameInfo getNextFrameBuffer();

    void releaseFrameBuffer(FrameInfo frameInfo);

    void seekTo(long timeUs);

    long getDuration();

    MediaFormat getMediaFormat();

    void setListener(IExtractorListener listener);

    void release();
}
