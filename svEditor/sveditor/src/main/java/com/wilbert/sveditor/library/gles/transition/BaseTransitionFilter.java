package com.wilbert.sveditor.library.gles.transition;

import android.content.Context;
import android.opengl.GLES20;

import com.wilbert.sveditor.library.gles.GLImageFilter;

import java.nio.FloatBuffer;

/**
 * Created by Android Studio.
 * User: wilbert jw20082009@qq.com
 * Date: 2019/8/14 14:24
 */
public class BaseTransitionFilter extends GLImageFilter {
    private final String TAG = "BaseTransitionFilter";
    private int inputImageTexture2Handler;
    private int progressHandler;
    private int ratioHandler;
    private float currentProgress = 0f;
    private boolean active = false;
    private GLTransitionFilter transitionFilter;

    public BaseTransitionFilter(Context context, String vertexShader, String fragmentShader) {
        super(context, vertexShader, fragmentShader);
        transitionFilter = new GLTransitionFilter(context);
    }

    @Override
    public void initProgramHandle() {
        super.initProgramHandle();
        inputImageTexture2Handler = GLES20.glGetUniformLocation(mProgramHandle, "inputImageTexture2");
        progressHandler = GLES20.glGetUniformLocation(mProgramHandle, "progress");
        ratioHandler = GLES20.glGetUniformLocation(mProgramHandle, "ratio");
    }

    public void centerInsideFrameBuffer(int width, int height) {
        if (reversed) {
            int temp = width;
            width = height;
            height = temp;
        }
        if (mImageWidth > 0 && mImageHeight > 0 && mFrameWidth > 0 && mFrameHeight > 0 && width > 0 && height > 0) {
            active = true;
            transitionFilter.centerInsideFrameBuffer(width, height);
            currentProgress = 0;
        }
    }

    public void centerInsideFrameBufferSimple(int width, int height) {
        if (reversed) {
            int temp = width;
            width = height;
            height = temp;
        }
        if (mImageWidth > 0 && mImageHeight > 0 && mFrameWidth > 0 && mFrameHeight > 0 && width > 0 && height > 0) {
            transitionFilter.centerInsideFrameBufferSimple(width, height);
        }
    }

    private boolean reversed = false;

    public void setReversed(boolean reversed) {
        if (reversed && !this.reversed) {
            this.reversed = true;
            if (mImageWidth > 0 && mImageHeight > 0 && mFrameWidth > 0 && mFrameHeight > 0) {
                transitionFilter.centerInsideFrameBufferSimple(transitionFilter.getFrameHeight(), transitionFilter.getFrameWidth());
            }
        } else if (!reversed && this.reversed) {
            this.reversed = false;
            if (mImageWidth > 0 && mImageHeight > 0 && mFrameWidth > 0 && mFrameHeight > 0) {
                transitionFilter.centerInsideFrameBufferSimple(transitionFilter.getFrameHeight(), transitionFilter.getFrameWidth());
            }
        }
    }

    @Override
    public void onInputSizeChanged(int width, int height) {
        super.onInputSizeChanged(width, height);
        transitionFilter.onInputSizeChanged(width, height);
    }

    @Override
    public void initFrameBuffer(int width, int height) {
        super.initFrameBuffer(width, height);
        transitionFilter.initFrameBuffer(width, height);
    }

    @Override
    public int drawFrameBuffer(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        transitionFilter.drawFrameBuffer(textureId, vertexBuffer, textureBuffer);
        if (active) {
            return super.drawFrameBuffer(transitionFilter.getNextFrameBufferTextureId(), vertexBuffer, textureBuffer);
        } else {
            return transitionFilter.getFrameBufferTextureId();
        }
    }

    @Override
    public void onDrawFrameBegin() {
        super.onDrawFrameBegin();
        if (active && currentProgress < 1.0f) {
            currentProgress = currentProgress + 0.03f;
        }
        if (currentProgress >= 1.0) {
            active = false;
        }
        GLES20.glUniform1f(ratioHandler, 1.0f * mImageWidth / mImageHeight);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(getTextureType(), transitionFilter.getFrameBufferTextureId());
        GLES20.glUniform1i(inputImageTexture2Handler, 1);
        GLES20.glUniform1f(progressHandler, currentProgress);
    }

    @Override
    public void release() {
        super.release();
        active = false;
        transitionFilter.release();
    }
}
