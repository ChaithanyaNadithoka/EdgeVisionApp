package com.example.edgevisionapp.gl;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * A reusable OpenGL surface view for rendering OpenCV-processed frames.
 * Works with both programmatic and XML usage.
 */
public class GLView extends GLSurfaceView {

    private GLRenderer renderer;

    // XML-based constructor (used when declared in activity_main.xml)
    public GLView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, null);
    }

    // Programmatic constructor
    public GLView(Context context, GLRenderer customRenderer) {
        super(context);
        init(context, customRenderer);
    }

    private void init(Context context, GLRenderer customRenderer) {
        setEGLContextClientVersion(2);
        setZOrderOnTop(true); // Required to draw over camera
        getHolder().setFormat(PixelFormat.TRANSLUCENT); // Transparent surface

        if (customRenderer != null) {
            this.renderer = customRenderer;
        } else {
            this.renderer = new GLRenderer();
        }

        setRenderer(this.renderer);
        setRenderMode(RENDERMODE_WHEN_DIRTY); // Only update on request
        Log.d("GLView", "GLSurfaceView initialized");
    }

    public void updateBitmap(Bitmap bitmap) {
        if (renderer != null) {
            renderer.updateBitmap(bitmap);
            requestRender();
        }
    }

    // Optional accessor if needed
    public GLRenderer getRenderer() {
        return renderer;
    }
}
