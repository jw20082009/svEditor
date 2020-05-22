package com.wilbert.sveditor.library.clips.abs;

import com.wilbert.sveditor.library.codecs.abs.FrameInfo;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/11
 * desc   :
 */
public interface IFrameWorker {

    FrameInfo getNextFrame();

    void releaseFrame(FrameInfo frameInfo);

    void release();
}
