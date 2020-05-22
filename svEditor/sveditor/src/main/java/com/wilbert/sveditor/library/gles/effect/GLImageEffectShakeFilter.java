package com.wilbert.sveditor.library.gles.effect;

import android.content.Context;
import android.opengl.GLES20;

import com.wilbert.sveditor.library.gles.OpenGLUtils;
import com.wilbert.sveditor.library.gles.shaders.Shader;

/**
 * 仿抖音抖动特效
 */
public class GLImageEffectShakeFilter extends GLImageEffectFilter {

    private int mScaleHandle;

    private float mScale = 1.0f;
    private float mOffset = 0.0f;

    public GLImageEffectShakeFilter(){
        super(VERTEX_SHADER, Shader.FRAGMENT_SHAKE);
    }

    public GLImageEffectShakeFilter(Context context) {
        this(context, VERTEX_SHADER, OpenGLUtils.getShaderFromAssets(context, "shader/action/fragment_effect_shake.glsl"));
    }

    private GLImageEffectShakeFilter(Context context, String vertexShader, String fragmentShader) {
        super(context, vertexShader, fragmentShader);
    }

    @Override
    public void initProgramHandle() {
        super.initProgramHandle();
        if (mProgramHandle != OpenGLUtils.GL_NOT_INIT) {
            mScaleHandle = GLES20.glGetUniformLocation(mProgramHandle, "scale");
        }
    }

    @Override
    public void onDrawFrameBegin() {
        super.onDrawFrameBegin();

        GLES20.glUniform1f(mScaleHandle, mScale);
    }

    @Override
    protected void calculateInterval() {
        // 步进，40ms算一次步进
        float interval = mCurrentPosition % 50.0f;
        mOffset += interval * 0.0025f;
        if (mOffset > 1.0f) {
            mOffset = 0.0f;
        }
        mScale = 1.0f + 0.3f * getInterpolation(mOffset);
    }

    private float getInterpolation(float input) {
        return (float)(Math.cos((input + 1) * Math.PI) / 2.0f) + 0.5f;
    }
}
