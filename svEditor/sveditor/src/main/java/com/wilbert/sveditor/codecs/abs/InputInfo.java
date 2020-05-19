package com.wilbert.sveditor.codecs.abs;

import java.nio.ByteBuffer;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class InputInfo {
    public int inputIndex;
    public ByteBuffer buffer;
    public int size = 0;
    public long time = 0;
    public boolean lastFrameFlag = false;

    public InputInfo(int inputIndex, ByteBuffer buffer) {
        this.inputIndex = inputIndex;
        this.buffer = buffer;
    }
}
