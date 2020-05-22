package com.wilbert.sveditor.library.clips.abs;

/**
 * author : Administrator
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public interface ISeekListener {
    void onSeekFinish(long inTimeUs, long outTimeUs);
}
