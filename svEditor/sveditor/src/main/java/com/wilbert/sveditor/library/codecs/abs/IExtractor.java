package com.wilbert.sveditor.library.codecs.abs;

import android.media.MediaFormat;

import com.wilbert.sveditor.library.codecs.SvExtractor;

import java.io.IOException;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/22
 * desc   :
 */
public interface IExtractor {

    MediaFormat _prepare(String filepath, SvExtractor.Type type) throws IOException;

    long _fillBuffer(InputInfo buffer);

    long _seekTo(long timeUs);

    boolean isPrepared();

    void _release();
}
