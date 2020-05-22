package com.wilbert.sveditor.library.gles.effect;

import android.content.Context;
import android.opengl.GLES20;

import com.wilbert.sveditor.library.gles.OpenGLUtils;

/**
 * 仿抖音黑白三屏特效
 */
public class GLImageEffectBlackWhiteThreeFilter extends GLImageEffectFilter {

    private int mScaleHandle;
    private float mScale = 1.2f;

    public GLImageEffectBlackWhiteThreeFilter(Context context) {
        this(context, VERTEX_SHADER, OpenGLUtils.getShaderFromAssets(context,
                "shader/action/fragment_effect_multi_bw_three.glsl"));
    }

    public GLImageEffectBlackWhiteThreeFilter(Context context, String vertexShader, String fragmentShader) {
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
}
