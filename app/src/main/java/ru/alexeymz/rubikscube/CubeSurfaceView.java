package ru.alexeymz.rubikscube;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.alexeymz.rubikscube.elements.RubiksCube;

public class CubeSurfaceView extends GLSurfaceView {

    private RubiksCube cube;

    private float[] projection = new float[16];
    private float[] view = new float[16];
    private float[] viewProjection = new float[16];

    public CubeSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 unused, EGLConfig config) {
                // Set the background frame color to Cornflower Blue
                GLES20.glClearColor(100 / 255f, 149 / 255f, 237 / 255f, 1.0f);
                Matrix.setLookAtM(view, 0, 0, 0, 500, 0, 0, 0, 0, 1, 0);

                cube = new RubiksCube(3);
            }

            @Override
            public void onSurfaceChanged(GL10 unused, int width, int height) {
                GLES20.glViewport(0, 0, width, height);
                float aspectRatio = (float)width / height;
                Matrix.perspectiveM(projection, 0, (float)Math.PI / 4, aspectRatio, 1, 1000);
            }

            @Override
            public void onDrawFrame(GL10 unused) {
                // Redraw background color
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                // Calculate VP matrix
                Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0);
                cube.draw(viewProjection);
            }
        });
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
