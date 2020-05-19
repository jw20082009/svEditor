package com.wilbert.sveditor.codecs;

import android.media.MediaCodec;
import android.media.MediaFormat;

import com.wilbert.sveditor.codecs.abs.FrameInfo;
import com.wilbert.sveditor.codecs.abs.IAudioParams;
import com.wilbert.sveditor.codecs.abs.IDecoder;
import com.wilbert.sveditor.codecs.abs.IVideoParams;
import com.wilbert.sveditor.codecs.abs.InputInfo;
import com.wilbert.sveditor.log.ALog;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * author : Administrator
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class SvMediaDecoder implements IDecoder, IAudioParams, IVideoParams {
    private final String TAG = "SvMediaDecoder";
    private MediaCodec mDecoder;
    private boolean mPrepared = false;
    private MediaFormat mInputFormat;
    private MediaFormat mOutputFormat;
    private InputInfo mCurrentInputInfo = null;
    private FrameInfo mCurrentFrameInfo = null;
    private LinkedBlockingDeque<InputInfo> mInputBuffers = new LinkedBlockingDeque<>(10);
    private LinkedBlockingDeque<FrameInfo> mOutputBuffers = new LinkedBlockingDeque<>(10);
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

    public SvMediaDecoder() {
    }

    @Override
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

    @Override
    public boolean flush() {
        ALog.i(TAG, "flush");
        if (mFlushEnable) {
            mCurrentInputInfo = null;
            mCurrentFrameInfo = null;
            mInputBuffers.clear();
            mOutputBuffers.clear();
            mDecoder.flush();
            mDecoder.start();
            mFlushEnable = false;
            return true;
        }
        return false;
    }

    @Override
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

    @Override
    public InputInfo dequeueInputBuffer() {
        if (mDecoder == null) {
            return null;
        }
        if (mCurrentInputInfo != null) {
            //保证外部必须先返回上一个buffer,才能取走下一个buffer
            //ALog.i(TAG, "dequeueInputBuffer same frame again");
            return null;
        }
        try {
            mCurrentInputInfo = mInputBuffers.takeLast();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //if (mCurrentInputInfo != null) {
        //ALog.i(TAG, "dequeueInputBuffer:" + mCurrentInputInfo.time + ";bufferIndex:" + mCurrentInputInfo.inputIndex + ";inputSize:" + mInputBuffers.size() + ";" + mType);
        //}
        return mCurrentInputInfo;
    }

    @Override
    public void queueInputBuffer(InputInfo inputInfo) {
        if (mDecoder != null) {
            if (inputInfo != null) {
                //ALog.i(TAG, "queueInputBuffer,time:" + inputInfo.time + ";size:" + inputInfo.size + ";flag:" + inputInfo.lastFrameFlag + ";index:" + inputInfo.inputIndex + ";" + mType);
                mDecoder.queueInputBuffer(inputInfo.inputIndex, 0, inputInfo.size <= 0 ? 0 : inputInfo.size,
                        inputInfo.time <= 0 ? 0 : inputInfo.time, inputInfo.lastFrameFlag ? MediaCodec.BUFFER_FLAG_END_OF_STREAM : 0);
                if (mCurrentInputInfo != null && mCurrentInputInfo.inputIndex == inputInfo.inputIndex) {
                    mCurrentInputInfo = null;
                }
            } else {
                mInputBuffers.offerFirst(new InputInfo(-1, null));
            }
        }
    }

    @Override
    public FrameInfo dequeueOutputBuffer() {
        if (mDecoder == null) {
            return null;
        }
        if (mCurrentFrameInfo != null) {
            //保证外部必须先返回上一个buffer,才能取走下一个buffer
            //ALog.i(TAG, "dequeueOutputBuffer same frame again");
            return null;
        }
        try {
            mCurrentFrameInfo = mOutputBuffers.takeLast();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //if (mCurrentFrameInfo != null) {
        //ALog.i(TAG, "dequeueOutputBuffer:" + mCurrentFrameInfo.presentationTimeUs + ";bufferIndex:" + mCurrentFrameInfo.outputIndex + ";" + mType);
        //}
        return mCurrentFrameInfo;
    }

    @Override
    public void queueOutputBuffer(FrameInfo frameInfo) {
        if (mDecoder != null) {
            if (frameInfo != null) {
                mDecoder.releaseOutputBuffer(frameInfo.outputIndex, false);
                if (mCurrentFrameInfo != null && mCurrentFrameInfo.outputIndex == frameInfo.outputIndex) {
                    mCurrentFrameInfo = null;
                }
            } else {
                mOutputBuffers.offerFirst(new FrameInfo(null, -1, -1, -1));
            }
        }
    }

    private MediaCodec.Callback mCallback = new MediaCodec.Callback() {

        @Override
        public void onInputBufferAvailable(MediaCodec codec, int index) {
            if (index >= 0) {
                try {
                    //ALog.i(TAG, "onInputBufferAvailable,index:" + index + ";" + mType);
                    mInputBuffers.putFirst(new InputInfo(index, codec.getInputBuffer(index)));
                } catch (InterruptedException e) {
                    e.printStackTrace();
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
                    try {
                        mOutputBuffers.putFirst(frameInfo);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
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
        public void onOutputFormatChanged(MediaCodec codec,MediaFormat format) {
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
