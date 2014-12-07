package ru.alexeymz.rubikscube;

import static ru.alexeymz.rubikscube.graphics.MathUtils.*;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.alexeymz.rubikscube.elements.RubiksCube;
import ru.alexeymz.rubikscube.view.PartSideCoords;

public class CubeSurfaceView extends GLSurfaceView {

    private static final float MAX_ROTATION_SPEED = 1000;

    private final Object rendererLock = new Object();
    private final CubeRenderer renderer;

    private long previousTime;
    private float previousX, previousY;
    private float rotationX, rotationY;

    private boolean rotateMode = true;

    private final Runnable frameRendered = new Runnable() {
        @Override
        public void run() {
            long time = SystemClock.uptimeMillis();
            long elapsed = time - previousTime;
            updateRotation(elapsed / 100f);
            previousTime = time;
        }
    };

    public CubeSurfaceView(Context context) {
        super(context);
        setEGLContextClientVersion(2);
        this.renderer = new CubeRenderer() {
            @Override
            public void onDrawFrame(GL10 unused) {
                super.onDrawFrame(unused);
                getHandler().post(frameRendered);
            }
        };
        setRenderer(renderer);
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public void resetView() {
        synchronized (renderer.model) {
            Matrix.setIdentityM(renderer.model, 0);
            rotationX = rotationY = 0;
        }
        requestRender();
    }

    public void toggle() {
        rotateMode = !rotateMode;
    }

    private void updateRotation(float dt) {
        synchronized (renderer.model) {
            Matrix.rotateM(renderer.model, 0, rotationX * dt, 0, 1, 0);
            Matrix.rotateM(renderer.model, 0, rotationY * dt,
                    renderer.model[RIGHT_X], renderer.model[RIGHT_Y], renderer.model[RIGHT_Z]);
        }
        rotationX *= Math.pow(0.1f, dt * 0.1f);
        rotationY *= Math.pow(0.1f, dt * 0.1f);
        if (Math.abs(rotationX) + Math.abs(rotationY) > 1f) {
            requestRender();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        final float x = e.getX();
        final float y = e.getY();
        long time = e.getEventTime();
        long elapsed = time - previousTime;

        switch (e.getAction()) {
            case MotionEvent.ACTION_MOVE:
                if (rotateMode) {
                    float dx = x - previousX;
                    float dy = y - previousY;
                    if (renderer.model[UP_Y] < 0) {
                        dx = -dx;
                    }
                    rotationX = clamp(dx, -MAX_ROTATION_SPEED, MAX_ROTATION_SPEED);
                    rotationY = clamp(dy, -MAX_ROTATION_SPEED, MAX_ROTATION_SPEED);
                    updateRotation(elapsed / 100f);
                } else {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            PartSideCoords coords = renderer.cube.locationAtPixel((int) x, (int) y);
                            final String data = coords == null ? "nothing" : coords.toString();
                            getHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    setTitle(String.format("(%s, %s) -> ", x, y) + data);
                                }
                            });
                        }
                    });
                }
                break;
        }

        previousX = x;
        previousY = y;
        previousTime = time;
        return true;
    }

    protected void setTitle(String title) {}
}
