package com.wilbert.sveditor.library.codecs;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.wilbert.sveditor.library.codecs.abs.FrameInfo;
import com.wilbert.sveditor.library.codecs.abs.IAudioParams;
import com.wilbert.sveditor.library.codecs.abs.IDecoder;
import com.wilbert.sveditor.library.codecs.abs.IVideoParams;
import com.wilbert.sveditor.library.codecs.abs.InputInfo;
import com.wilbert.sveditor.library.log.ALog;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * author : Administrator
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class SvMediaDecoder implements IAudioParams, IVideoParams {
    private final String TAG = "SvMediaDecoder";
    private MediaCodec mDecoder;
    private boolean mPrepared = false;
    private MediaFormat mInputFormat;
    private MediaFormat mOutputFormat;

    private int mType = 0;//0:video;1:audio;
    private int mFps = 0;
    private int mBitRate = 0;
    private int mDecodeWidth = 0;
    private int mDecodeHeight = 0;
    private int mDecodeRotation = 0;
    private int mChannelCount = 0;
    private int mSampleRate = 0;
    private long mDuration = 0;
    /**
     * care must be taken if the codec is flushed immediately or shortly
     * after start, before any output buffer or output format change has been returned, as the codec
     * specific data may be lost during the flush. You must resubmit the data using buffers marked with
     * #BUFFER_FLAG_CODEC_CONFIG after such flush to ensure proper codec operation.
     */
    private boolean mFlushEnable = false;
    private SvDecoder mCounsumer;

    public SvMediaDecoder(SvDecoder handle) {
        mCounsumer = handle;
    }

    public boolean isPrepared() {
        return mPrepared;
    }

    public boolean prepare(MediaFormat format) throws IOException {
        ALog.i(TAG, "prepare:" + mPrepared);
        if (mPrepared && mDecoder != null) {
            return true;
        }
        if (format == null) {
            return false;
        }
        mInputFormat = format;
        String mime = format.getString(MediaFormat.KEY_MIME);
        if (mime.contains("video")) {
            mType = 0;
        } else if (mime.contains("audio")) {
            mType = 1;
        } else {
            mType = -1;
        }
        mDecoder = MediaCodec.createDecoderByType(mime);
        mDecoder.setCallback(mCallback);
        mDecoder.configure(mInputFormat, null, null, 0);
        mDecoder.start();
        mFlushEnable = false;
        return mPrepared = true;
    }

    public boolean flush() {
        ALog.i(TAG, "flush");
        if (mFlushEnable) {
            mDecoder.flush();
            mDecoder.start();
            mFlushEnable = false;
            return true;
        }
        return false;
    }

    public void release() {
        ALog.i(TAG, "release");
        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.setCallback(null);
            mDecoder.release();
            mDecoder = null;
            mFlushEnable = false;
            mPrepared = false;
        }
    }

    public void queueInputBuffer(InputInfo inputInfo) {
        if (mDecoder == null || inputInfo == null) {
            return;
        }
        mDecoder.queueInputBuffer(inputInfo.inputIndex, 0, inputInfo.size <= 0 ? 0 : inputInfo.size,
                inputInfo.time <= 0 ? 0 : inputInfo.time, inputInfo.lastFrameFlag ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
    }

    public void queueOutputBuffer(FrameInfo frameInfo) {
        if (mDecoder == null || frameInfo == null) {
            return;
        }
        mDecoder.releaseOutputBuffer(frameInfo.outputIndex, false);
    }

    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            if (index >= 0) {
                //ALog.i(TAG, "onInputBufferAvailable,index:" + index + ";" + mType);
                if (mCounsumer != null) {
                    mCounsumer.offerInputInfo(new InputInfo(index, codec.getInputBuffer(index)));
                } else {
                    codec.queueInputBuffer(index, 0, 0, 0, 0);
                }
            } else {
                ALog.i(TAG, "onInputBufferAvailable:" + index);
            }
        }

        @Override
        public void onOutputBufferAvailable(MediaCodec codec, int index, MediaCodec.BufferInfo info) {
            //ALog.i(TAG, "onOutputBufferAvailable,index:" + index + ";" + mType);
            if (index >= 0 && mOutputFormat != null) {
                FrameInfo frameInfo = null;
                if (mType == 0) {
                    frameInfo = new FrameInfo(index, codec.getOutputBuffer(index), info.size,
                            info.presentationTimeUs, mDecodeWidth, mDecodeHeight, mDecodeRotation);
                } else if (mType == 1) {
                    frameInfo = new FrameInfo(index, codec.getOutputBuffer(index), info.size,
                            info.presentationTimeUs, mSampleRate, mChannelCount);
                }
                if (frameInfo != null) {
                    if (mCounsumer != null) {
                        mCounsumer.offerFrameInfo(frameInfo);
                    } else {
                        codec.releaseOutputBuffer(index, false);
                    }
                }
            } else {
                ALog.i(TAG, "onOutputBufferAvailable:" + index);
            }
        }

        @Override
        public void onError(MediaCodec codec, MediaCodec.CodecException e) {
            ALog.e(TAG, "onError", e);
            mDecoder.reset();
            mDecoder.configure(mInputFormat, null, null, 0);
            mDecoder.start();
        }

        @Override
        public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
            mOutputFormat = format;
            if (mOutputFormat != null) {
                if (mOutputFormat.containsKey(MediaFormat.KEY_WIDTH))
                    mDecodeWidth = mOutputFormat.getInteger(MediaFormat.KEY_WIDTH);
                if (mOutputFormat.containsKey(MediaFormat.KEY_HEIGHT))
                    mDecodeHeight = mOutputFormat.getInteger(MediaFormat.KEY_HEIGHT);
                if (mOutputFormat.containsKey(MediaFormat.KEY_ROTATION))
                    mDecodeRotation = mOutputFormat.getInteger(MediaFormat.KEY_ROTATION);
                if (mOutputFormat.containsKey(MediaFormat.KEY_CHANNEL_COUNT))
                    mChannelCount = mOutputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                if (mOutputFormat.containsKey(MediaFormat.KEY_SAMPLE_RATE))
                    mSampleRate = mOutputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                if (mOutputFormat.containsKey(MediaFormat.KEY_FRAME_RATE))
                    mFps = mOutputFormat.getInteger(MediaFormat.KEY_FRAME_RATE);
                if (mOutputFormat.containsKey(MediaFormat.KEY_BIT_RATE))
                    mBitRate = mOutputFormat.getInteger(MediaFormat.KEY_BIT_RATE);
                if (mOutputFormat.containsKey(MediaFormat.KEY_DURATION))
                    mDuration = mOutputFormat.getLong(MediaFormat.KEY_DURATION);
            }
            mFlushEnable = true;
        }
    };

    @Override
    public long getDuration() {
        return mDuration;
    }

    @Override
    public int getWidth() {
        return mDecodeWidth;
    }

    @Override
    public int getHeight() {
        return mDecodeHeight;
    }

    @Override
    public int getRotation() {
        return mDecodeRotation;
    }

    @Override
    public int getFps() {
        return mFps;
    }

    @Override
    public int getBitrate() {
        return mBitRate;
    }

    @Override
    public int getSampleRate() {
        return mSampleRate;
    }

    @Override
    public int getChannels() {
        return mChannelCount;
    }
}
