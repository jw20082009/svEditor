package com.wilbert.sveditor.library.codecs.abs;

import com.wilbert.sveditor.library.codecs.SvMediaExtractorWrapper;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/27
 * desc   :
 */
public interface IExtractorListener {

    void onPrepared(SvMediaExtractorWrapper extractor);

    void onReleased(SvMediaExtractorWrapper extractor);

    void onError(SvMediaExtractorWrapper extractor, Throwable throwable);
}
