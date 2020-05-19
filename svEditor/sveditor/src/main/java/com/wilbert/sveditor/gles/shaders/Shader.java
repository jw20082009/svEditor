package com.wilbert.sveditor.gles.shaders;

public class Shader {

    public static String FRAGMENT_OES = "#extension GL_OES_EGL_image_external : require\n"  +
            "precision mediump float;\n"                                                    +
            "varying vec2 textureCoordinate;\n"                                             +
            "uniform samplerExternalOES inputTexture;\n"                                    +
            "void main() {\n"                                                               +
            "    gl_FragColor = texture2D(inputTexture, textureCoordinate);\n"              +
            "}";

    public static String VERTEX_OES = "uniform mat4 transformMatrix;\n" +
            "attribute vec4 aPosition;\n" +
            "attribute vec4 aTextureCoord;\n" +
            "\n" +
            "varying vec2 textureCoordinate;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = aPosition;\n" +
            "    textureCoordinate = (transformMatrix * aTextureCoord).xy;\n" +
            "}";

    public static String VERTEX = "attribute vec4 aPosition;           // 图像顶点坐标\n" +
            "attribute vec4 aTextureCoord;       // 图像纹理坐标\n" +
            "\n" +
            "varying vec2 textureCoordinate;     // 图像纹理坐标\n" +
            "\n" +
            "void main() {\n" +
            "    gl_Position = aPosition;\n" +
            "    textureCoordinate = aTextureCoord.xy;\n" +
            "}";

    public static String FRAGMENT = "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform sampler2D inputTexture;\n" +
            "\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(inputTexture, textureCoordinate);\n" +
            "}";

    public static String FRAGMENT_SOUL_STUFF = "precision highp float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform sampler2D inputTexture;\n" +
            "\n" +
            "uniform float scale;\n" +
            "\n" +
            "void main() {\n" +
            "     vec2 uv = textureCoordinate.xy;\n" +
            "     // 输入纹理\n" +
            "     vec4 sourceColor = texture2D(inputTexture, fract(uv));\n" +
            "     // 将纹理坐标中心转成(0.0, 0.0)再做缩放\n" +
            "     vec2 center = vec2(0.5, 0.5);\n" +
            "     uv -= center;\n" +
            "     uv = uv / scale;\n" +
            "     uv += center;\n" +
            "     // 缩放纹理\n" +
            "     vec4 scaleColor = texture2D(inputTexture, fract(uv));\n" +
            "     // 线性混合\n" +
            "     gl_FragColor = mix(sourceColor, scaleColor, 0.5 * (0.6 - fract(scale)));\n" +
            "}";

    public static String FRAGMENT_SHAKE = "precision highp float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform sampler2D inputTexture;\n" +
            "\n" +
            "uniform float scale;\n" +
            "\n" +
            "void main()\n" +
            "{\n" +
            "    vec2 uv = textureCoordinate.xy;\n" +
            "    vec2 scaleCoordinate = vec2((scale - 1.0) * 0.5 + uv.x / scale ,\n" +
            "                                (scale - 1.0) * 0.5 + uv.y / scale);\n" +
            "    vec4 smoothColor = texture2D(inputTexture, scaleCoordinate);\n" +
            "\n" +
            "    // 计算红色通道偏移值\n" +
            "    vec4 shiftRedColor = texture2D(inputTexture,\n" +
            "         scaleCoordinate + vec2(-0.1 * (scale - 1.0), - 0.1 *(scale - 1.0)));\n" +
            "\n" +
            "    // 计算绿色通道偏移值\n" +
            "    vec4 shiftGreenColor = texture2D(inputTexture,\n" +
            "         scaleCoordinate + vec2(-0.075 * (scale - 1.0), - 0.075 *(scale - 1.0)));\n" +
            "\n" +
            "    // 计算蓝色偏移值\n" +
            "    vec4 shiftBlueColor = texture2D(inputTexture,\n" +
            "         scaleCoordinate + vec2(-0.05 * (scale - 1.0), - 0.05 *(scale - 1.0)));\n" +
            "\n" +
            "    vec3 resultColor = vec3(shiftRedColor.r, shiftGreenColor.g, shiftBlueColor.b);\n" +
            "\n" +
            "    gl_FragColor = vec4(resultColor, smoothColor.a);\n" +
            "}";

    public static String FRAGMENT_MULTI =
            "precision mediump float;\n" +
                    "varying vec2 textureCoordinate;\n" +
                    "uniform sampler2D inputTexture;\n" +
                    "uniform int multiswitch;//0关闭，3：3分屏幕，4:4分屏，9：9分屏\n" +
                    "\n" +
                    "void main() {\n" +
                    "    mediump vec2 uv = textureCoordinate;\n" +
                    "    if (multiswitch == 9){ //分屏\n" +
                    "        if (uv.x < 1.0 / 3.0) {\n" +
                    "            uv.x = uv.x * 3.0;\n" +
                    "        } else if (uv.x < 2.0 / 3.0) {\n" +
                    "            uv.x = (uv.x - 1.0 / 3.0) * 3.0;\n" +
                    "        } else {\n" +
                    "            uv.x = (uv.x - 2.0 / 3.0) * 3.0;\n" +
                    "        }\n" +
                    "        if (uv.y <= 1.0 / 3.0) {\n" +
                    "            uv.y = uv.y * 3.0;\n" +
                    "        } else if (uv.y < 2.0 / 3.0) {\n" +
                    "            uv.y = (uv.y - 1.0 / 3.0) * 3.0;\n" +
                    "        } else {\n" +
                    "            uv.y = (uv.y - 2.0 / 3.0) * 3.0;\n" +
                    "        }\n" +
                    "    } else if (multiswitch == 6){\n" +
                    "        if (uv.x <= 1.0 / 3.0) {\n" +
                    "            uv.x = uv.x + 1.0 / 3.0;\n" +
                    "        } else if (uv.x >= 2.0 / 3.0) {\n" +
                    "            uv.x = uv.x - 1.0 / 3.0;\n" +
                    "        }\n" +
                    "        // 上下分两屏，保留 0.25 ~ 0.75部分\n" +
                    "        if (uv.y <= 0.5) {\n" +
                    "            uv.y = uv.y + 0.25;\n" +
                    "        } else {\n" +
                    "            uv.y = uv.y - 0.25;\n" +
                    "        }\n" +
                    "    } else if (multiswitch == 4){\n" +
                    "        if (uv.x <= 0.5) {\n" +
                    "            uv.x = uv.x * 2.0;\n" +
                    "        } else {\n" +
                    "            uv.x = (uv.x - 0.5) * 2.0;\n" +
                    "        }\n" +
                    "        if (uv.y <= 0.5) {\n" +
                    "            uv.y = uv.y * 2.0;\n" +
                    "        } else {\n" +
                    "            uv.y = (uv.y - 0.5) * 2.0;\n" +
                    "        }\n" +
                    "    } else if (multiswitch == 3){\n" +
                    "        if (uv.y < 1.0 / 3.0) {\n" +
                    "            uv.y = uv.y + 1.0 / 3.0;\n" +
                    "        } else if (uv.y > 2.0 / 3.0) {\n" +
                    "            uv.y = uv.y - 1.0 / 3.0;\n" +
                    "        }\n" +
                    "    } else if (multiswitch == 2){\n" +
                    "        if (uv.y >= 0.0 && uv.y <= 0.5) {\n" +
                    "            uv.y = uv.y + 0.25;\n" +
                    "        } else {\n" +
                    "            uv.y = uv.y - 0.25;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    gl_FragColor = texture2D(inputTexture, uv);\n" +
                    "}";

    public static String FRAGMENT_ILLUSION = "#extension GL_EXT_shader_framebuffer_fetch : require\n" +
            "precision mediump float;\n" +
            "varying vec2 textureCoordinate;\n" +
            "uniform sampler2D inputTexture;     // 当前输入纹理\n" +
            "\n" +
            "// 分RGB通道混合，不同颜色通道混合值不一样\n" +
            "const lowp vec3 blendValue = vec3(0.1, 0.3, 0.6);\n" +
            "\n" +
            "void main() {\n" +
            "    // 当前纹理颜色\n" +
            "    vec4 currentColor = texture2D(inputTexture, textureCoordinate);\n" +
            "    // 提取上一轮纹理颜色\n" +
            "    vec4 lastColor = gl_LastFragData[0];\n" +
            "    // 将纹理与上一轮的纹理进行线性混合\n" +
            "    gl_FragColor = vec4(mix(lastColor.rgb, currentColor.rgb, blendValue), currentColor.a);\n" +
            "}";
}
