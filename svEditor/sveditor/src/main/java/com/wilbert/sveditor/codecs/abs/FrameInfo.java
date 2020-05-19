package com.wilbert.sveditor.codecs.abs;

import java.nio.ByteBuffer;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class FrameInfo {

    public int frameWidth;
    public int frameHeight;
    public int rotation = 0;
    public int outputIndex;
    public int size;
    public int sampleRate;
    public int channels;
    public long presentationTimeUs;
    public ByteBuffer outputBuffer;

    public FrameInfo(int outIndex, ByteBuffer buffer, int size, long presentationTimeUs, int frameWidth, int frameHeight, int rotation) {
        this.outputIndex = outIndex;
        this.outputBuffer = buffer;
        this.size = size;
        this.presentationTimeUs = presentationTimeUs;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.rotation = rotation;
    }

    public FrameInfo(int outIndex, ByteBuffer buffer, int size, long presentationTimeUs, int sampleRate, int channels) {
        this.outputIndex = outIndex;
        this.outputBuffer = buffer;
        this.size = size;
        this.presentationTimeUs = presentationTimeUs;
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public FrameInfo(ByteBuffer buffer, int frameWidth, int frameHeight, int rotation) {
        this.outputBuffer = buffer;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.rotation = rotation;
    }
}
