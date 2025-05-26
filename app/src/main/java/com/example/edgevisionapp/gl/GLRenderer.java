package com.example.edgevisionapp.gl;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRenderer implements GLSurfaceView.Renderer {

    private Bitmap bitmap;
    private final int[] textureId = new int[1];
    private boolean textureInitialized = false;

    public synchronized void updateBitmap(Bitmap bmp) {
        if (this.bitmap != null && !this.bitmap.isRecycled()) {
            this.bitmap.recycle();
        }
        this.bitmap = bmp;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        GLES20.glGenTextures(1, textureId, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glClearColor(0f, 0f, 0f, 1f);

        // Initialize with dummy 1x1 texture
        Bitmap dummy = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, dummy, 0);
        dummy.recycle();
        textureInitialized = true;

        Log.d("GLRenderer", "Surface created and dummy texture initialized");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        if (bitmap != null && !bitmap.isRecycled()) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId[0]);

            if (!textureInitialized) {
                GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
                textureInitialized = true;
                Log.d("GLRenderer", "Initial texImage2D called");
            } else {
                GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bitmap);
                Log.d("GLRenderer", "Bitmap frame updated");
            }
        }
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }
}
