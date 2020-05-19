package com.wilbert.sveditor.contexts;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import com.wilbert.sveditor.clips.abs.IFrameWorker;
import com.wilbert.sveditor.codecs.abs.FrameInfo;
import com.wilbert.sveditor.contexts.abs.ITimeline;
import com.wilbert.sveditor.contexts.yuv.IYuvRenderer;
import com.wilbert.sveditor.contexts.yuv.NV21Renderer;
import com.wilbert.sveditor.gles.OpenGLUtils;
import com.wilbert.sveditor.gles.TextureRotationUtils;
import com.wilbert.sveditor.log.ALog;

import java.nio.FloatBuffer;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * author : Administrator
 * e-mail : jw20082009@qq.com
 * time   : 2020/04/26
 * desc   :
 */
public class VideoContext {
    public static final String TAG = "VideoContext";
    private LinkedBlockingDeque<FrameInfo> mFrameInfos = new LinkedBlockingDeque<>(1);
    private GLSurfaceView mSurfaceView;
    private IFrameWorker mWorker;
    private AtomicBoolean mPlaying = new AtomicBoolean(true);
    private final int EMPTY_WAITING = 5_000;//us
    private final int FRAME_TIME = 33_333;//33ms一帧
    private int mSurfaceWidth, mSurfaceHeight;
    private int mProgramOut;
    private int mPositionHandle;
    private int mTextureCoordinateHandle;
    private int mInputTextureHandle;
    private int mCoordsPerVertex = TextureRotationUtils.CoordsPerVertex;
    private int mVertexCount = TextureRotationUtils.CubeVertices.length / mCoordsPerVertex;
    private ITimeline mTimeline;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;
    private VideoRenderer mRender;
    private Object mLock = new Object();
    private Object mSync = new Object();


    public VideoContext(GLSurfaceView surfaceView, IFrameWorker worker, ITimeline timeline) {
        mWorker = worker;
        mSurfaceView = surfaceView;
        mTimeline = timeline;
        mSurfaceView.setEGLContextClientVersion(2);
        mRender = new VideoRenderer();
        mSurfaceView.setRenderer(mRender);
        mSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        new Thread(mFrameWorker, "VideoContext_Worker").start();
    }

    public void pause() {
        ALog.i(TAG, "release");
        synchronized (mSync) {
            mFrameInfos.offerFirst(new FrameInfo(null, -1, -1, -1));
            mRender.release();
        }
    }

    public void release() {
        ALog.i(TAG, "release");
        synchronized (mSync) {
            mFrameInfos.offerFirst(new FrameInfo(null, -1, -1, -1));
            mPlaying.set(false);
            mRender.release();
        }
    }

    private Runnable mFrameWorker = new Runnable() {
        private boolean mFirstFrame = true;

        @Override
        public void run() {
            ALog.i(TAG, "worker started");
            while (mPlaying.get()) {
                if (mWorker != null) {
                    FrameInfo frameInfo = mWorker.getNextFrame();
                    long waiting = -1;
                    if (mFirstFrame) {
                        mTimeline.start();
                        mFirstFrame = false;
                    }
                    if (frameInfo != null) {
                        try {
                            mFrameInfos.putFirst(frameInfo);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        waiting = mTimeline.compareTime(frameInfo.presentationTimeUs);
                    } else {
                        waiting = EMPTY_WAITING;
                    }
                    do {
                        try {
                            synchronized (mLock) {
                                long timeWait = waiting / 1000;
                                if (timeWait > 0) {
                                    mLock.wait(timeWait);
                                }
                            }
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (frameInfo != null) {
                            waiting = mTimeline.compareTime(frameInfo.presentationTimeUs);
                        } else {
                            waiting = 0;
                        }
                    } while (waiting > 0);
                    if (frameInfo != null)
                        mSurfaceView.requestRender();
                } else {
                    ALog.i(TAG, "no worker and quit");
                    mPlaying.set(false);
                }
            }
        }
    };

    private class VideoRenderer implements GLSurfaceView.Renderer {

        private final int STATUS_IDLE = 0x00;
        private final int STATUS_PREPARED = 0x01;
        private final int STATUS_RUNNING = 0x02;
        private final int STATUS_RELEASING = 0x03;
        private final int STATUS_RELEASED = 0x04;

        private int mStatus = STATUS_IDLE;
        private IYuvRenderer mYuvRender;

        public VideoRenderer() {
            mYuvRender = new NV21Renderer();
        }

        @Override
        public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            ALog.i(TAG, "onSurfaceCreated");
            mProgramOut = OpenGLUtils.createProgram(OpenGLUtils.getShaderFromAssets(mSurfaceView.getContext(),
                    "shader/base/vertex_normal.glsl"), OpenGLUtils.getShaderFromAssets(mSurfaceView.getContext(),
                    "shader/base/fragment_normal.glsl"));
            mPositionHandle = GLES20.glGetAttribLocation(mProgramOut, "aPosition");
            mTextureCoordinateHandle = GLES20.glGetAttribLocation(mProgramOut, "aTextureCoord");
            mInputTextureHandle = GLES20.glGetUniformLocation(mProgramOut, "inputTexture");
            mVertexBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.CubeVertices);
            mTextureBuffer = OpenGLUtils.createFloatBuffer(TextureRotationUtils.TextureVertices);
            mYuvRender.onSurfaceCreated();
            mStatus = STATUS_PREPARED;
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        }

        @Override
        public void onSurfaceChanged(GL10 gl, int width, int height) {
            ALog.i(TAG, "onSurfaceChanged");
            mSurfaceWidth = width;
            mSurfaceHeight = height;
            mYuvRender.onSurfaceChanged(width, height);
            mStatus = STATUS_RUNNING;
        }

        @Override
        public void onDrawFrame(GL10 gl) {
            if (mWorker == null) {
                ALog.i(TAG, "onDrawFrame null worker");
                return;
            }
            FrameInfo frameInfo = mFrameInfos.pollLast();//(10, TimeUnit.MILLISECONDS);
            if (frameInfo == null || frameInfo.size <= 0) {
                ALog.i(TAG, "onDrawFrame null frameInfo");
                return;
            }
            int textureId = mYuvRender.yuv2Rgb(frameInfo);
            if (textureId == -1) {
                ALog.i(TAG, "onDrawFrame null textureId");
                return;
            }
            GLES20.glUseProgram(mProgramOut);
            mVertexBuffer.position(0);
            GLES20.glVertexAttribPointer(mPositionHandle, mCoordsPerVertex, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
            GLES20.glEnableVertexAttribArray(mPositionHandle);
            mTextureBuffer.position(0);
            GLES20.glVertexAttribPointer(mTextureCoordinateHandle, mCoordsPerVertex, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
            GLES20.glEnableVertexAttribArray(mTextureCoordinateHandle);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            GLES20.glUniform1i(mInputTextureHandle, 0);
            GLES20.glViewport(0, 0, mSurfaceWidth, mSurfaceHeight);
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, mVertexCount);
            GLES20.glDisableVertexAttribArray(mPositionHandle);
            GLES20.glDisableVertexAttribArray(mTextureCoordinateHandle);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glUseProgram(0);
            mWorker.releaseFrame(frameInfo);
        }

        private void _release() {
            ALog.i(TAG, "mYuvRender.release()");
            if (mYuvRender != null) {
                mYuvRender.release();
            }
        }

        public void release() {
            ALog.i(TAG, "VideoRenderer release");
            changeStatus(STATUS_RELEASING);
            mSurfaceView.queueEvent(new Runnable() {
                @Override
                public void run() {
                    if (mStatus == STATUS_RELEASING) {
                        _release();
                        changeStatus(STATUS_RELEASED);
                        ALog.i(TAG, "onDrawFrame when releasing");
                        return;
                    }
                }
            });
        }

        public void changeStatus(int status) {
            synchronized (mSync) {
                mStatus = status;
            }
        }
    }
}
