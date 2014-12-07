package ru.alexeymz.rubikscube;

import static ru.alexeymz.rubikscube.graphics.MathUtils.*;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.view.MotionEvent;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.alexeymz.rubikscube.elements.RubiksCube;

public class CubeSurfaceView extends GLSurfaceView {

    private static final float MAX_ROTATION_SPEED = 1000;

    private long lastTime = 0;

    private RubiksCube cube;

    private float[] model = new float[16];
    private float[] projection = new float[16];
    private float[] view = new float[16];
    private float[] viewProjection = new float[16];
    private float[] mvp = new float[16];

    private float previousX, previousY;
    private float rotationX, rotationY;

    public CubeSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        setRenderer(new GLSurfaceView.Renderer() {
            @Override
            public void onSurfaceCreated(GL10 unused, EGLConfig config) {
                GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                // Set the background frame color to Cornflower Blue
                GLES20.glClearColor(100 / 255f, 149 / 255f, 237 / 255f, 1.0f);
                Matrix.setLookAtM(view, 0,
                        0, 0, 5,
                        0, 0, 0,
                        0, 1, 0);
                Matrix.setIdentityM(model, 0);
                cube = new RubiksCube(5);
            }

            @Override
            public void onSurfaceChanged(GL10 unused, int width, int height) {
                GLES20.glViewport(0, 0, width, height);
                float aspectRatio = (float)width / height;
                Matrix.perspectiveM(projection, 0,
                        45 /* (!) degrees */, aspectRatio, 1, 10);
            }

            @Override
            public void onDrawFrame(GL10 unused) {
                long currentTime = System.currentTimeMillis();
                long elapsed = currentTime - lastTime;
                if (lastTime > 0) { update(elapsed); }
                draw();
                lastTime = currentTime;
            }

            private void update(long elapsed) {
                float step = elapsed / 100f;
                Matrix.rotateM(model, 0, rotationX * step, 0, 1, 0);
                Matrix.rotateM(model, 0, rotationY * step,
                        model[RIGHT_X], model[RIGHT_Y], model[RIGHT_Z]);
                rotationX *= Math.pow(0.1f, step * 0.1f);
                rotationY *= Math.pow(0.1f, step * 0.1f);
                if (Math.abs(rotationX) + Math.abs(rotationY) > 1f) {
                    continueAnimation();
                }
            }

            private void draw() {
                // Redraw background color
                GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
                // Calculate VP matrix
                Matrix.multiplyMM(viewProjection, 0, projection, 0, view, 0);
                Matrix.multiplyMM(mvp, 0, viewProjection, 0, model, 0);
                //Matrix.setIdentityM(mvp, 0);
                cube.draw(mvp);
            }
        });
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private void startAnimation() {
        lastTime = System.currentTimeMillis();
        requestRender();
    }

    private void continueAnimation() {
        requestRender();
    }

    public void resetView() {
        Matrix.setIdentityM(model, 0);
        rotationX = 0;
        rotationY = 0;
        requestRender();
        //Matrix.scaleM(model, 0, 100, 100, 100);
        //Matrix.rotateM(model, 0, 45, 1, 0, 0);
        //Matrix.rotateM(model, 0, 45, 0, 1, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();
        e.getEventTime();

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx = x - previousX;
                float dy = y - previousY;
                if (model[UP_Y] < 0) { dx = -dx; }
                rotationX = clamp(dx, -MAX_ROTATION_SPEED, MAX_ROTATION_SPEED);
                rotationY = clamp(dy, -MAX_ROTATION_SPEED, MAX_ROTATION_SPEED);
                startAnimation();
                break;
        }

        previousX = x;
        previousY = y;
        return true;
    }
}
