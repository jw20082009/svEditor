package com.wilbert.sveditor.library.codecs.abs;

import com.wilbert.sveditor.library.codecs.SvExtractor;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/27
 * desc   :
 */
public interface IExtractorListener {

    void onPrepared(SvExtractor extractor);

    void onReleased(SvExtractor extractor);

    void onError(SvExtractor extractor, Throwable throwable);
}
