package com.wilbert.sveditor.clips.abs;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/09
 * desc   :
 */
public interface IAudioClip {
    void prepare(String filepath);

    void setOnPreparedListener(IPreparedListener listener);

    String getFilepath();

}
