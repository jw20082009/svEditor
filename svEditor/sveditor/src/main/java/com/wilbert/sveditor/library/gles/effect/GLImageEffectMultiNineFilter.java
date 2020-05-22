package com.wilbert.sveditor.library.gles.effect;

import android.content.Context;
import android.opengl.GLES20;

import com.wilbert.sveditor.library.gles.OpenGLUtils;
import com.wilbert.sveditor.library.gles.shaders.Shader;

public class GLImageEffectMultiNineFilter extends GLImageEffectFilter {

    int mMultiNumHandler;
    int multiNum = 9;

    public GLImageEffectMultiNineFilter(){
        super(VERTEX_SHADER, Shader.FRAGMENT_MULTI);
    }

    public GLImageEffectMultiNineFilter(Context context) {
        this(context, VERTEX_SHADER, OpenGLUtils.getShaderFromAssets(context,
                "shader/action/fragment_multi.glsl"));
    }

    public GLImageEffectMultiNineFilter(Context context, String vertexShader, String fragmentShader) {
        super(context, vertexShader, fragmentShader);
    }

    /**
     * 初始化程序句柄
     */
    public void initProgramHandle() {
        super.initProgramHandle();
        if (mProgramHandle != OpenGLUtils.GL_NOT_INIT) {
            mMultiNumHandler = GLES20.glGetUniformLocation(mProgramHandle, "multiswitch");
        }
    }

    @Override
    public void onDrawFrameBegin() {
        super.onDrawFrameBegin();
        GLES20.glUniform1i(mMultiNumHandler, multiNum);
    }
}
