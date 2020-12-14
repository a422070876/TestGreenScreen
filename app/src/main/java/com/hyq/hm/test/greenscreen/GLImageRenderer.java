package com.hyq.hm.test.greenscreen;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * Created by 海米 on 2017/8/16.
 */

public class GLImageRenderer {

    private int programId = -1;
    private int aPositionHandle;
    private int vTextureSamplerHandle;
    private int aTextureCoordHandle;
    private int aImgworldHandle;
    private float[] world = new float[]{
            2f / 1920, 0, 0,
            0, 2f / 1080, 0,
            -1f, -1f, 1
    };

    private int[] bos = new int[2];
    private int[] textures = new int[1];


    public void initShader() {
        String fragmentShader =
                "varying highp vec3 vTexCoord;\n" +
                "uniform sampler2D vTexture;\n" +
                "void main() {\n" +
                "   highp vec2 ig_flip_y = vTexCoord.xy / vTexCoord.z;\n" +
                "   highp vec4 rgba = texture2D(vTexture , vec2(ig_flip_y.x , 1.0 - ig_flip_y.y));\n" +
                "   gl_FragColor = rgba;\n" +
                "}";
        String vertexShader = "attribute vec2 aPosition;\n" +
                "attribute vec3 aTexCoord;\n" +
                "varying vec3 vTexCoord;\n" +
                "uniform mat3 imgworld;\n" +
                "void main() {\n" +
                "  vTexCoord = aTexCoord;\n" +
                "  vec3 xyz = imgworld * vec3(aPosition, 1);\n" +
                "  gl_Position = vec4(xyz.xy, 0, 1);\n" +
                "}";

        programId = ShaderUtils.createProgram(vertexShader, fragmentShader);
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition");
        vTextureSamplerHandle = GLES20.glGetUniformLocation(programId, "vTexture");
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord");
        aImgworldHandle = GLES20.glGetUniformLocation(programId, "imgworld");


        GLES20.glGenBuffers(bos.length, bos, 0);




        float[] vertexData = {
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

        float[] textureVertexData = {
                1f, -1f, 0f,
                -1f, -1f, 0f,
                1f, 1f, 0f,
                -1f, 1f, 0f
        };
        FloatBuffer textureVertexBuffer = ByteBuffer.allocateDirect(textureVertexData.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
                .put(textureVertexData);
        textureVertexBuffer.position(0);

        GLES20.glGenBuffers(bos.length, bos, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vertexData.length * 4, vertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1]);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, textureVertexData.length * 4, textureVertexBuffer, GLES20.GL_STATIC_DRAW);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);



        GLES20.glGenTextures(textures.length, textures, 0);
        for (int texture : textures) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
    public void setWorld(int width,int height){
        world = new float[]{
                2f / width, 0, 0,
                0, 2f / height, 0,
                -1f, -1f, 1
        };
    }

    private FloatBuffer positionBuffer;
    private FloatBuffer attributesBuffer;

    public void setVertexData(float[] positionData, float[] attributesData) {
        if (positionBuffer == null) {
            positionBuffer = ByteBuffer.allocateDirect(positionData.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        positionBuffer.position(0);
        positionBuffer.put(positionData);
        positionBuffer.position(0);
        if (attributesBuffer == null) {
            attributesBuffer = ByteBuffer.allocateDirect(attributesData.length * 4)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
        }
        attributesBuffer.position(0);
        attributesBuffer.put(attributesData);
        attributesBuffer.position(0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0]);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,positionData.length * 4, positionBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1]);
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,attributesData.length * 4, attributesBuffer);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
    }


    public void setBitmap(Bitmap bitmap){
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }

    public void drawFrame() {
        GLES20.glUseProgram(programId);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textures[0]);
        GLES20.glUniform1i(vTextureSamplerHandle, 0);


        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[0]);
        GLES20.glEnableVertexAttribArray(aPositionHandle);
        GLES20.glVertexAttribPointer(aPositionHandle, 2, GLES20.GL_FLOAT, false,
                0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, bos[1]);
        GLES20.glEnableVertexAttribArray(aTextureCoordHandle);
        GLES20.glVertexAttribPointer(aTextureCoordHandle, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glUniformMatrix3fv(aImgworldHandle, 1, false, world, 0);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
    }
    public void release() {
        GLES20.glDeleteProgram(programId);
        GLES20.glDeleteTextures(textures.length, textures, 0);
        GLES20.glDeleteBuffers(bos.length, bos, 0);
    }


}
