package com.wilbert.sveditor.gles.transition;

import android.content.Context;
import android.opengl.GLES20;
import android.util.Log;

import com.wilbert.sveditor.gles.GLImageFilter;
import com.wilbert.sveditor.gles.OpenGLUtils;

import java.nio.FloatBuffer;

/**
 * Created by Android Studio.
 * User: wilbert jw20082009@qq.com
 * Date: 2019/8/13 18:18
 */
public class GLTransitionFilter extends GLImageFilter {
    private final int FRAME_BUFFER_SIZE = 2;
    protected int mCurrentFrameBuffer = 0;

    public GLTransitionFilter(Context context) {
        super(context);
    }

    public GLTransitionFilter(Context context, String vertexShader, String fragmentShader) {
        super(context, vertexShader, fragmentShader);
    }

    public GLTransitionFilter(String vertexShader, String fragmentShader) {
        super(vertexShader, fragmentShader);
    }

    protected void changeFrameBuffer() {
        mCurrentFrameBuffer = (mCurrentFrameBuffer + 1) % 2;
    }

    public int getCurrentFrameBufferIndex() {
        return mCurrentFrameBuffer;
    }

    /**
     * 创建FBO
     */
    public void initFrameBuffer(int width, int height) {
        mImageWidth = width;
        mImageHeight = height;
        if (!isInitialized()) {
            return;
        }
        if (mFrameBuffers != null && (mFrameWidth != width || mFrameHeight != height)) {
            destroyFrameBuffer();
        }
        if (mFrameBuffers == null) {
            mFrameWidth = width;
            mFrameHeight = height;
            mFrameBuffers = new int[FRAME_BUFFER_SIZE];
            mFrameBufferTextures = new int[FRAME_BUFFER_SIZE];
            OpenGLUtils.createFrameBuffer(mFrameBuffers, mFrameBufferTextures, width, height);
        }
    }

    /**
     * 绘制到FBO
     *
     * @return FBO绑定的Texture
     */
    public int drawFrameBuffer(int textureId, FloatBuffer vertexBuffer, FloatBuffer textureBuffer) {
        // 没有FBO、没初始化、输入纹理不合法、滤镜不可用时，直接返回
        if (textureId == OpenGLUtils.GL_NOT_TEXTURE
                || mFrameBuffers == null
                || mFrameBufferTextures == null
                || !mIsInitialized
                || !mFilterEnable) {
            return textureId;
        }

        // 绑定FBO
        GLES20.glViewport(mFrameStartX, mFrameStartY, mFrameWidth, mFrameHeight);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[mCurrentFrameBuffer]);
        if (mHasFrameBufferSizeChanged) {
            GLES20.glClearColor(0f, 0f, 0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            mHasFrameBufferSizeChanged = false;
        }
        // 使用当前的program
        GLES20.glUseProgram(mProgramHandle);
        // 运行延时任务，这个要放在glUseProgram之后，要不然某些设置项会不生效
        runPendingOnDrawTasks();

        // 绘制纹理
        onDrawTexture(textureId, vertexBuffer, textureBuffer);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        return mFrameBufferTextures[mCurrentFrameBuffer];
    }

    /**
     * 销毁纹理
     */
    public void destroyFrameBuffer() {
        if (!mIsInitialized) {
            return;
        }
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(FRAME_BUFFER_SIZE, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }

        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(FRAME_BUFFER_SIZE, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
        mFrameWidth = -1;
    }

    public int getFrameBuffer() {
        if (mFrameBuffers != null)
            return mFrameBuffers[mCurrentFrameBuffer];
        return -1;
    }

    public int getNextFrameBuffer() {
        if (mFrameBuffers != null)
            return mFrameBuffers[(mCurrentFrameBuffer + 1) % 2];
        return -1;
    }

    public int getFrameBufferTextureId() {
        if (mFrameBufferTextures != null)
            return mFrameBufferTextures[mCurrentFrameBuffer];
        return -1;
    }

    public int getNextFrameBufferTextureId() {
        if (mFrameBufferTextures != null)
            return mFrameBufferTextures[(mCurrentFrameBuffer + 1) % 2];
        return -1;
    }

    /**
     * 修改frameBuffer绘制区域，必须先初始化过frameBuffer
     * 根据新的frameWidth，frameHeight在现有framebuffer大小内做centerInside操作
     *
     * @param frameWidth
     * @param frameHeight
     */
    public void centerInsideFrameBuffer(int frameWidth, int frameHeight) {
        if (mImageWidth > 0 && mImageHeight > 0 && mFrameWidth > 0 && mFrameHeight > 0 && frameWidth > 0 && frameHeight > 0) {
            if (1.0f * frameWidth / frameHeight > 1.0f * mImageWidth / mImageHeight) {
                mFrameStartX = 0;
                mFrameWidth = mImageWidth;
                mFrameHeight = (int) (mFrameWidth / (1.0f * frameWidth / frameHeight));
                mFrameStartY = (int) ((mImageHeight - mFrameHeight) / 2.0f);
            } else if (1.0f * frameWidth / frameHeight < 1.0f * mImageWidth / mImageHeight) {
                mFrameStartY = 0;
                mFrameHeight = mImageHeight;
                mFrameWidth = (int) (mFrameHeight * (1.0f * frameWidth / frameHeight));
                mFrameStartX = (int) ((mImageWidth - mFrameWidth) / 2.0f);
            } else {
                mFrameStartX = 0;
                mFrameStartY = 0;
                mFrameWidth = mImageWidth;
                mFrameHeight = mImageHeight;
            }
            mHasFrameBufferSizeChanged = true;
            changeFrameBuffer();
        }
    }

    public void centerInsideFrameBufferSimple(int frameWidth, int frameHeight) {
        Log.i(TAG, "centerInsideFrameBufferSimple:" + frameWidth + ";" + frameHeight);
        if (mImageWidth > 0 && mImageHeight > 0 && mFrameWidth > 0 && mFrameHeight > 0 && frameWidth > 0 && frameHeight > 0) {
            if (1.0f * frameWidth / frameHeight > 1.0f * mImageWidth / mImageHeight) {
                mFrameStartX = 0;
                mFrameWidth = mImageWidth;
                mFrameHeight = (int) (mFrameWidth / (1.0f * frameWidth / frameHeight));
                mFrameStartY = (int) ((mImageHeight - mFrameHeight) / 2.0f);
            } else if (1.0f * frameWidth / frameHeight < 1.0f * mImageWidth / mImageHeight) {
                mFrameStartY = 0;
                mFrameHeight = mImageHeight;
                mFrameWidth = (int) (mFrameHeight * (1.0f * frameWidth / frameHeight));
                mFrameStartX = (int) ((mImageWidth - mFrameWidth) / 2.0f);
            } else {
                mFrameStartX = 0;
                mFrameStartY = 0;
                mFrameWidth = mImageWidth;
                mFrameHeight = mImageHeight;
            }
            mHasFrameBufferSizeChanged = true;
        }
    }
}
