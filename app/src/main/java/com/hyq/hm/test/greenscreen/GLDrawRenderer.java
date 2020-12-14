package com.hyq.hm.test.greenscreen;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class GLDrawRenderer {
    private int programId = -1;
    private int aPositionHandle;
    private int uTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int smoothHandle;

    private int[] bos = new int[2];

    public void initShader() {
        String fragmentShader = "varying highp vec2 vTexCoord;\n" +
                "uniform sampler2D sTexture;\n" +
                "uniform highp float smooth;\n" +
                "highp vec3 rgb2hsv(highp vec3 c){\n" +
                "    highp vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);\n" +
                "    highp vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));\n" +
                "    highp vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));\n" +
                "    highp float d = q.x - min(q.w, q.y);\n" +
                "    highp float e = 1.0e-10;\n" +
                "    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);\n" +
                "}\n"+
//                "void modifyColor(vec4 color){\n" +
//                "    color.r=max(min(color.r,1.0),0.0);\n" +
//                "    color.g=max(min(color.g,1.0),0.0);\n" +
//                "    color.b=max(min(color.b,1.0),0.0);\n" +
//                "    color.a=max(min(color.a,1.0),0.0);\n" +
//                "}\n"+
                "void main() {\n" +
                "   highp vec4 rgba = texture2D(sTexture , vec2(vTexCoord.x,1.0 - vTexCoord.y));\n" +
                "   highp float rbAverage = (rgba.r + rgba.b)*0.8;\n"+
                "   highp vec3 hsv = rgb2hsv(rgba.rgb);\n"+
                "   highp float hmin = 0.19444000;\n" +
                "   highp float hmax = 0.42777888;\n" +
                "   highp float smin = 0.16862000;\n" +
                "   highp float smax = 1.0;\n" +
                "   highp float vmin = 0.18039000;\n" +
                "   highp float vmax = 1.0;\n" +
                "   int gs = 0;\n"+
                "   if(hsv.x >= hmin && hsv.x <= hmax &&\n" +
                "       hsv.y >= smin && hsv.y <= smax &&\n" +
                "       hsv.z >= vmin && hsv.z <= vmax){\n" +
                "       gs = 1;\n"+
                "   }else if(rgba.g >= rbAverage && rgba.g > 0.6){\n" +
                "       gs = 1;\n"+
                "   }\n"+
                "   if(gs == 1){\n" +
                "       rbAverage = (rgba.r + rgba.b)*0.65;\n"+
                "       if(rbAverage > rgba.g)rbAverage = rgba.g;"+
                "       highp float gDelta = rgba.g - rbAverage;\n"+
                "       highp float ss = smoothstep(0.0, smooth, gDelta);\n"+
                "       rgba.a = 1.0 - ss;\n"+
                "       rgba.a = rgba.a * rgba.a * rgba.a;\n"+
                "       rgba = mix(vec4(0.0),rgba,rgba.a);\n"+
//                "       rgba.a = 0.0;\n"+     //这样可以大幅度减少绿边,但是会丢失细节
                "   }\n"+
//                "   highp vec4 deltaColor = rgba + vec4(0.1, 0.1, 0.0, 0.0);\n"+
//                "   modifyColor(deltaColor);\n"+
//                "   gl_FragColor = deltaColor;\n" +
//                "   lowp float average = (rgba.r + rgba.g + rgba.b) / 3.0;\n" +
//                "   lowp float mx = max(rgba.r, max(rgba.g, rgba.b));\n" +
//                "   lowp float amt = (mx - average) * (0.1 * 3.0);\n" +
//                "   rgba.rgb = mix(rgba.rgb, vec3(mx), amt);\n" +
                "   gl_FragColor = rgba;\n"+
                "}";
        String vertexShader = "attribute vec4 aPosition;\n" +
                "attribute vec2 aTexCoord;\n" +
                "varying vec2 vTexCoord;\n" +
                "void main() {\n" +
                "  vTexCoord = aTexCoord;\n" +
                "  gl_Position = aPosition;\n" +
                "}";
        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        uTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "sTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
        smoothHandle = GLES20.glGetUniformLocation(programId, "smooth");


        float[] vertexData = {
                1f, -1f, 0f,
                -1f, -1f, 0f,
                1f, 1f, 0f,
                -1f, 1f, 0f
        };


        float[] textureVertexData = {
                1f, 0f,
                0f, 0f,
                1f, 1f,
                0f, 1f
        };
        FloatBuffer vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(vertexData);
        vertexBuffer.position(0);


        FloatBuffer textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);

        GLES20.glGenBuffers(2, bos, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureVertexData.length * 4, textureVertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

    }
    private float smooth = 0.05f;
    public void setSmooth(float smooth){
        this.smooth = smooth;
    }
    private FloatBuffer attributesBuffer;

    public void setVertexData( float[] attributesData) {

        if (attributesBuffer == null) {
            attributesBuffer = ByteBuffer.allocateDirect(attributesData.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        attributesBuffer.position(0);
        attributesBuffer.put(attributesData);
        attributesBuffer.position(0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1]);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,attributesData.length * 4, attributesBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }

    public void drawFrame(int texture) {
        GLES20.glUseProgram(programId);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
        GLES20.glUniform1i(uTextureSamplerHandle, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0]);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 3, GLES20.GL_FLOAT, false,
                0, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1]);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 2, GLES20.GL_FLOAT, false, 0, 0);


        GLES20.glUniform1f(smoothHandle, smooth);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void release() {
        GLES20.glDeleteProgram(programId);
        GLES20.glDeleteBuffers(bos.length, bos, 0);
    }
}
