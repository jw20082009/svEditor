package com.wilbert.sveditor.library.contexts.yuv;

import android.opengl.GLES20;

import com.wilbert.sveditor.library.codecs.abs.FrameInfo;
import com.wilbert.sveditor.library.gles.OpenGLUtils;
import com.wilbert.sveditor.library.gles.TextureRotationUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/15
 * desc   :
 */

public class YUV420pRenderer implements IYuvRenderer {

    private static final String YUV_INPUT_VERTEX_SHADER = "" + "attribute vec4 position;\n"
            + "attribute vec4 inputTextureCoordinate;\n" + "\n"
            + "varying vec2 textureCoordinate;\n" + "\n" + "void main()\n" + "{\n"
            + "	textureCoordinate = inputTextureCoordinate.xy;\n" + "	gl_Position = position;\n"
            + "}";

    private static final String YUV_TEXTURE = "precision mediump float;                           \n"
            + "varying vec2 textureCoordinate;                           \n"
            + "uniform sampler2D y_texture;                       \n"
            + "uniform sampler2D u_texture;                      \n"
            + "uniform sampler2D v_texture;                      \n"
            + "void main (void){                                  \n"
            + "   vec3 yuv;"
            + "   float x = textureCoordinate.x;        \n"
            + "   float y = textureCoordinate.y;"
            + "   vec2 cord = vec2(x,y);"
            + "   yuv.x = texture2D(y_texture,cord).r;"
            + "   yuv.y = texture2D(u_texture,cord).r - 0.5;"
            + "   yuv.z = texture2D(v_texture,cord).r - 0.5;"
            + "   yuv.x = 1.1643 * yuv.x - 0.0728;"
            + "   gl_FragColor = vec4(yuv.x + 1.5958 * yuv.z,yuv.x - 0.39173 * yuv.y -0.8129 * yuv.z,yuv.x + 2.017 * yuv.y,1.0);"
            + "}";

    private int mSurfaceWidth = -1;
    private int mSurfaceHeight = -1;
    private int mProgramIn;
    private int yuvPositionLoc = -1;
    private int yuvTextureLoc = -1;
    private int yTextureLoc = -1;
    private int uTextureLoc = -1;
    private int vTextureLoc = -1;
    private int[] mTextureY;
    private int[] mTextureUV;
    private int[] mFrameBuffers;
    private int[] mFrameBufferTextures;
    private int mCoordsPerVertex = TextureRotationUtils.CoordsPerVertex;
    private FloatBuffer mTextureBuffer;
    private FloatBuffer mVertexBuffer;
    private boolean mTextureInit = false;
    private boolean mIsInitialized = false;
    private final int TEXTURE_TYPE = GLES20.GL_TEXTURE_2D;

    public YUV420pRenderer() {
        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtils.TextureVertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureBuffer.put(TextureRotationUtils.TextureVertices).position(0);
        mVertexBuffer = ByteBuffer.allocateDirect(TextureRotationUtils.CubeVertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mVertexBuffer.put(TextureRotationUtils.CubeVertices).position(0);
    }

    @Override
    public void onSurfaceCreated() {
        mProgramIn = OpenGLUtils.createProgram(YUV_INPUT_VERTEX_SHADER, YUV_TEXTURE);
        yuvPositionLoc = GLES20.glGetAttribLocation(mProgramIn, "position");
        yuvTextureLoc = GLES20.glGetAttribLocation(mProgramIn, "inputTextureCoordinate");
        yTextureLoc = GLES20.glGetUniformLocation(mProgramIn, "y_texture");
        uTextureLoc = GLES20.glGetUniformLocation(mProgramIn, "u_texture");
        vTextureLoc = GLES20.glGetUniformLocation(mProgramIn, "v_texture");
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        this.mSurfaceWidth = width;
        this.mSurfaceHeight = height;
    }

    private void calculateVertexBuffer(int displayW, int displayH, int imageW, int imageH) {
        int outputHeight = displayH;
        int outputWidth = displayW;
        float ratio1 = (float) outputWidth / imageW;
        float ratio2 = (float) outputHeight / imageH;
        float ratio = Math.max(ratio1, ratio2);//max时为内切，min时为填满并裁剪
        int imageWidthNew = Math.round(imageW * ratio);
        int imageHeightNew = Math.round(imageH * ratio);
        float ratioWidth = imageWidthNew / (float) outputWidth;
        float ratioHeight = imageHeightNew / (float) outputHeight;
        float[] cube = new float[]{
                TextureRotationUtils.CubeVertices[0] / ratioHeight, TextureRotationUtils.CubeVertices[1] / ratioWidth,
                TextureRotationUtils.CubeVertices[2] / ratioHeight, TextureRotationUtils.CubeVertices[3] / ratioWidth,
                TextureRotationUtils.CubeVertices[4] / ratioHeight, TextureRotationUtils.CubeVertices[5] / ratioWidth,
                TextureRotationUtils.CubeVertices[6] / ratioHeight, TextureRotationUtils.CubeVertices[7] / ratioWidth,
        };
        if (mVertexBuffer == null) {
            mVertexBuffer = ByteBuffer.allocateDirect(cube.length * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer();
        }
        mVertexBuffer.clear();
        mVertexBuffer.put(cube).position(0);
    }

    private void initFrameBuffers(int width, int height) {
        if (mFrameBuffers == null) {
            mFrameBuffers = new int[1];
            mFrameBufferTextures = new int[1];
            GLES20.glGenFramebuffers(1, mFrameBuffers, 0);
            GLES20.glGenTextures(1, mFrameBufferTextures, 0);
            bindFrameBuffer(mFrameBufferTextures[0], mFrameBuffers[0], width, height);
        }
        setUpTexture();
        mIsInitialized = true;
    }

    private void bindFrameBuffer(int textureId, int frameBuffer, int width, int height) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer);
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
    }

    private void setUpTexture() {
        // nv21 y texture
        mTextureY = new int[1];
        GLES20.glGenTextures(1, mTextureY, 0);
        GLES20.glBindTexture(TEXTURE_TYPE, mTextureY[0]);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        // nv21 uv texture
        mTextureUV = new int[2];
        GLES20.glGenTextures(2, mTextureUV, 0);
        GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[0]);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);

        GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[1]);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
    }

    private void updateYUVTexture(FrameInfo frameInfo) {
        if (frameInfo == null) {
            return;
        }
        int align;
        if (frameInfo.frameWidth % 8 == 0) {
            align = 4;
        } else if (frameInfo.frameWidth % 4 == 0) {
            align = 2;
        } else {
            align = 1;
        }
        if (align != 4) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, align);
        }
        ByteBuffer frameRenderBuffer = frameInfo.outputBuffer;
        if (!mTextureInit) {
            frameRenderBuffer.position(0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureY[0]);
            GLES20.glTexImage2D(TEXTURE_TYPE, 0, GLES20.GL_LUMINANCE, frameInfo.frameWidth,
                    frameInfo.frameHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                    frameRenderBuffer);

            frameRenderBuffer.position(frameInfo.frameHeight * frameInfo.frameWidth);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[0]);
            GLES20.glTexImage2D(TEXTURE_TYPE, 0, GLES20.GL_LUMINANCE,
                    frameInfo.frameWidth / 2, frameInfo.frameHeight / 2, 0, GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);

            frameRenderBuffer.position(frameInfo.frameHeight * frameInfo.frameWidth * 5 / 4);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[1]);
            GLES20.glTexImage2D(TEXTURE_TYPE, 0, GLES20.GL_LUMINANCE,
                    frameInfo.frameWidth / 2, frameInfo.frameHeight / 2, 0, GLES20.GL_LUMINANCE,
                    GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);
            mTextureInit = true;
        } else {
            frameRenderBuffer.position(0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureY[0]);
            GLES20.glTexSubImage2D(TEXTURE_TYPE, 0, 0, 0, frameInfo.frameWidth, frameInfo.frameHeight,
                    GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);

            frameRenderBuffer.position(frameInfo.frameWidth * frameInfo.frameHeight);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[0]);
            GLES20.glTexSubImage2D(TEXTURE_TYPE, 0, 0, 0, frameInfo.frameWidth / 2, frameInfo.frameHeight / 2,
                    GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);

            frameRenderBuffer.position(frameInfo.frameWidth * frameInfo.frameHeight * 5 / 4);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[1]);
            GLES20.glTexSubImage2D(TEXTURE_TYPE, 0, 0, 0, frameInfo.frameWidth / 2, frameInfo.frameHeight / 2,
                    GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);
        }
        if (align != 4) {
            GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, 4);
        }
    }

    @Override
    public int yuv2Rgb(FrameInfo frameInfo) {
        if (frameInfo == null)
            return -1;
        if (mFrameBuffers == null) {
            initFrameBuffers(frameInfo.frameWidth, frameInfo.frameHeight);
        }
        if (!mIsInitialized) {
            return -1;
        }
        updateYUVTexture(frameInfo);
        if (mVertexBuffer == null) {
            calculateVertexBuffer(mSurfaceWidth, mSurfaceHeight, frameInfo.frameWidth, frameInfo.frameHeight);
        }
        GLES20.glUseProgram(mProgramIn);
        mVertexBuffer.position(0);
        GLES20.glVertexAttribPointer(yuvPositionLoc, mCoordsPerVertex, GLES20.GL_FLOAT, false, 0, mVertexBuffer);
        GLES20.glEnableVertexAttribArray(yuvPositionLoc);

        mTextureBuffer.position(0);
        GLES20.glVertexAttribPointer(yuvTextureLoc, mCoordsPerVertex, GLES20.GL_FLOAT, false, 0, mTextureBuffer);
        GLES20.glEnableVertexAttribArray(yuvTextureLoc);

        if (mTextureY[0] != -1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureY[0]);
            GLES20.glUniform1i(yTextureLoc, 0);
        }
        if (mTextureUV[0] != -1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[0]);
            GLES20.glUniform1i(uTextureLoc, 1);
        }
        if (mTextureUV[1] != -1) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[1]);
            GLES20.glUniform1i(vTextureLoc, 2);
        }
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffers[0]);
        GLES20.glViewport(0, 0, frameInfo.frameWidth, frameInfo.frameHeight);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glDisableVertexAttribArray(yuvPositionLoc);
        GLES20.glDisableVertexAttribArray(yuvTextureLoc);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(TEXTURE_TYPE, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
        GLES20.glBindTexture(TEXTURE_TYPE, 0);
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        GLES20.glUseProgram(0);
        return mFrameBufferTextures[0];
    }

    @Override
    public void destroyFrameBuffers() {
        if (mFrameBufferTextures != null) {
            GLES20.glDeleteTextures(1, mFrameBufferTextures, 0);
            mFrameBufferTextures = null;
        }
        if (mFrameBuffers != null) {
            GLES20.glDeleteFramebuffers(1, mFrameBuffers, 0);
            mFrameBuffers = null;
        }
    }

    @Override
    public void release() {
        mTextureInit = false;
        destroyFrameBuffers();
        GLES20.glDeleteProgram(mProgramIn);
    }
}
