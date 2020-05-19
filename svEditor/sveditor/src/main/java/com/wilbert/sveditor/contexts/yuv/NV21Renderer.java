package com.wilbert.sveditor.contexts.yuv;

import android.opengl.GLES20;

import com.wilbert.sveditor.codecs.abs.FrameInfo;
import com.wilbert.sveditor.gles.OpenGLUtils;
import com.wilbert.sveditor.gles.TextureRotationUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * author : wilbert
 * e-mail : jw20082009@qq.com
 * time   : 2020/05/11
 * desc   :
 */
public class NV21Renderer implements IYuvRenderer {

    private static final String YUV_INPUT_VERTEX_SHADER = "" + "attribute vec4 position;\n"
            + "attribute vec4 inputTextureCoordinate;\n" + "\n"
            + "varying vec2 textureCoordinate;\n" + "\n" + "void main()\n" + "{\n"
            + "	textureCoordinate = inputTextureCoordinate.xy;\n" + "	gl_Position = position;\n"
            + "}";

    private static final String YUV_TEXTURE = "precision mediump float;                           \n"
            + "varying vec2 textureCoordinate;                           \n"
            + "uniform sampler2D y_texture;                       \n"
            + "uniform sampler2D uv_texture;                      \n"
            + "void main (void){                                  \n"
            + "   float y = texture2D(y_texture, textureCoordinate).r;        \n"

            // We had put the Y values of each pixel to the R,G,B components by
            // GL_LUMINANCE,
            // that's why we're pulling it from the R component, we could also use G or B
            + "   vec2 uv = texture2D(uv_texture, textureCoordinate).xw - 0.5;       \n"

            // The numbers are just YUV to RGB conversion constants
            + "   float b = y + 1.370705 * uv.x;\n"
            + "   float g = y - 0.698001 * uv.x - 0.337633 * uv.y;\n"
            + "   float r = y + 1.732446 * uv.y;\n      " +
            // We finally set the RGB color of our pixel
            // 摄像头返回的NV21数据与MediaCodec（Redmi Note 5）解码返回的yuv数据不同
            // NV21数据：b = y + 1.732446 * uv.y;   r = y + 1.370705 * uv.x;"
            // MediaCodec解码数据：b = y + 1.370705 * uv.x;   r = y + 1.732446 * uv.y;
            "   gl_FragColor = vec4(r, g, b, 1.0);              \n"
            + "}";

    private static final String YUV_TEXTURE2 = "precision mediump float;                           \n"
            + "varying vec2 textureCoordinate;                           \n"
            + "uniform sampler2D y_texture;                       \n"
            + "uniform sampler2D uv_texture;                      \n"
            + "void main (void){                                  \n"
            + "   float y = texture2D(y_texture, textureCoordinate).r;        \n"

            // We had put the Y values of each pixel to the R,G,B components by
            // GL_LUMINANCE,
            // that's why we're pulling it from the R component, we could also use G or B
            + "   vec2 uv = texture2D(uv_texture, textureCoordinate).xw - 0.5;       \n"

            // The numbers are just YUV to RGB conversion constants
            + "   float r = y + 1.370705 * uv.x;\n"
            + "   float g = y - 0.698001 * uv.x - 0.337633 * uv.y;\n"
            + "   float b = y + 1.732446 * uv.y;\n      " +
            // We finally set the RGB color of our pixel
            // 摄像头返回的NV21数据与MediaCodec（Redmi Note 5）解码返回的yuv数据不同
            // NV21数据：b = y + 1.732446 * uv.y;   r = y + 1.370705 * uv.x;"
            // MediaCodec解码数据：b = y + 1.370705 * uv.x;   r = y + 1.732446 * uv.y;
            "   gl_FragColor = vec4(r, g, b, 1.0);              \n"
            + "}";

    private int mSurfaceWidth = -1;
    private int mSurfaceHeight = -1;
    private int mProgramIn;
    private int yuvPositionLoc = -1;
    private int yuvTextureLoc = -1;
    private int yTextureLoc = -1;
    private int uvTextureLoc = -1;
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

    public interface Type {
        int CAMERA_PREVIEW = 0X01;
        int DECODE_PREVIEW = 0x02;
    }

    private int mType = Type.DECODE_PREVIEW;

    public NV21Renderer() {
        mTextureBuffer = ByteBuffer.allocateDirect(TextureRotationUtils.TextureVertices.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTextureBuffer.put(TextureRotationUtils.TextureVertices).position(0);
    }

    public NV21Renderer(int type) {
        this();
        mType = type;
    }

    @Override
    public void onSurfaceCreated() {
        mProgramIn = OpenGLUtils.createProgram(YUV_INPUT_VERTEX_SHADER, mType == Type.DECODE_PREVIEW ? YUV_TEXTURE : YUV_TEXTURE2);
        yuvPositionLoc = GLES20.glGetAttribLocation(mProgramIn, "position");
        yuvTextureLoc = GLES20.glGetAttribLocation(mProgramIn, "inputTextureCoordinate");
        yTextureLoc = GLES20.glGetUniformLocation(mProgramIn, "y_texture");
        uvTextureLoc = GLES20.glGetUniformLocation(mProgramIn, "uv_texture");
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
        mTextureUV = new int[1];
        GLES20.glGenTextures(1, mTextureUV, 0);
        GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[0]);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(TEXTURE_TYPE, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);
    }

    private void updateNV21YUVTexture(FrameInfo frameInfo) {
        if (frameInfo == null) {
            return;
        }
        ByteBuffer frameRenderBuffer = frameInfo.outputBuffer;
        if (!mTextureInit) {
            frameRenderBuffer.position(0);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureY[0]);
            GLES20.glTexImage2D(TEXTURE_TYPE, 0, GLES20.GL_LUMINANCE, frameInfo.frameWidth,
                    frameInfo.frameHeight, 0, GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE,
                    frameRenderBuffer);
            frameRenderBuffer.position(frameInfo.frameHeight * frameInfo.frameWidth);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[0]);
            GLES20.glTexImage2D(TEXTURE_TYPE, 0, GLES20.GL_LUMINANCE_ALPHA,
                    frameInfo.frameWidth / 2, frameInfo.frameHeight / 2, 0, GLES20.GL_LUMINANCE_ALPHA,
                    GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);
            mTextureInit = true;
        } else {
            frameRenderBuffer.position(0);
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureY[0]);
            GLES20.glTexSubImage2D(TEXTURE_TYPE, 0, 0, 0, frameInfo.frameWidth, frameInfo.frameHeight,
                    GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);
            frameRenderBuffer.position(frameInfo.frameWidth * frameInfo.frameHeight);
            GLES20.glBindTexture(TEXTURE_TYPE, mTextureUV[0]);
            GLES20.glTexSubImage2D(TEXTURE_TYPE, 0, 0, 0, frameInfo.frameWidth / 2, frameInfo.frameHeight / 2,
                    GLES20.GL_LUMINANCE_ALPHA, GLES20.GL_UNSIGNED_BYTE, frameRenderBuffer);
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
        updateNV21YUVTexture(frameInfo);
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
            GLES20.glUniform1i(uvTextureLoc, 1);
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
