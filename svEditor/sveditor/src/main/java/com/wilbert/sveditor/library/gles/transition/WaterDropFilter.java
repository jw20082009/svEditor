package com.wilbert.sveditor.library.gles.transition;

import android.content.Context;

import com.wilbert.sveditor.library.gles.OpenGLUtils;

/**
 * Created by Android Studio.
 * User: wilbert jw20082009@qq.com
 * Date: 2019/8/13 17:59
 */
public class WaterDropFilter extends BaseTransitionFilter {


    public WaterDropFilter(Context context) {
        super(context, VERTEX_SHADER, OpenGLUtils.getShaderFromAssets(context, "shader/transition/waterdrop.glsl"));
    }

}
