package ru.alexeymz.rubikscube;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.alexeymz.rubikscube.elements.RubiksCube;

public class CubeRenderer implements GLSurfaceView.Renderer {

    public RubiksCube cube;
    public final float[] model = new float[16];
    public volatile float absoluteTimeMs;

    private float[] projection = new float[16];
    private float[] view = new float[16];
    private float[] viewProjection = new float[16];
    private float[] mvp = new float[16];

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST);
        // Set the background frame color to Cornflower Blue
        GLES20.glClearColor(100 / 255f, 149 / 255f, 237 / 255f, 1.0f);
        Matrix.setLookAtM(view, 0,
            0, 0, 5, /* eye position */
            0, 0, 0, /* object position */
            0, 1, 0);/* up vector */
        Matrix.setIdentityM(model, 0);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        float aspectRatio = (float)width / height;
        Matrix.perspectiveM(projection, 0,
            45 /* (!) degrees */, aspectRatio, 1, 10);
        if (cube == null) {
            cube = new RubiksCube(2, width, height);
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        // Redraw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        // Calculate VP matrix
        Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0);
        synchronized (model) {
            Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0);
        }
        cube.draw(mvp, absoluteTimeMs);
    }
}
