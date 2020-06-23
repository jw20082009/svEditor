package com.wilbert.sveditor.library.codecs.abs;

import com.wilbert.sveditor.library.codecs.SvDecoder;
import com.wilbert.sveditor.library.codecs.SvExtractor;

public interface IDecoderListener {

    void onPrepared(SvDecoder extractor);

    void onReleased(SvDecoder extractor);

    void onError(SvDecoder extractor, Throwable throwable);
}
