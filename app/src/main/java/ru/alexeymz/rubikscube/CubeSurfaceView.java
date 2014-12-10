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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import ru.alexeymz.rubikscube.core.Rotation;
import ru.alexeymz.rubikscube.elements.RubiksCube;
import ru.alexeymz.rubikscube.graphics.MathUtils;
import ru.alexeymz.rubikscube.view.PartSideCoords;

public class CubeSurfaceView extends GLSurfaceView {

    private static final float MAX_ROTATION_SPEED = 1000;
    private static final float SELECTION_MOVE_THRESHOLD = 200;
    private static final double LAYER_ROTATION_DURATION_MS = 500;

    private final CubeRenderer renderer;

    private long startTime, time, previousTime;

    private boolean isRotating = false;
    private boolean hasSelection = false;

    private float originMoveX, originMoveY;
    private float previousX, previousY;

    private float rotationX, rotationY;

    private final Deque<Rotation> rotations = new ArrayDeque<Rotation>();

    private final Runnable frameRendered = new Runnable() {
        @Override
        public void run() {
            time = SystemClock.uptimeMillis();
            update(time - previousTime);
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
        startTime = SystemClock.uptimeMillis();
    }

    protected void update(long elapsed) {
        renderer.absoluteTimeMs = time - startTime;
        boolean redraw = false;
        redraw |= updateCubeRotation(elapsed);
        redraw |= updateLayerRotation();
        redraw |= hasSelection;
        if (redraw) {
            requestRenderProvidedTime();
        }
    }

    private boolean updateLayerRotation() {
        if (renderer.cube.isAnimationInProgress()) {
            renderer.cube.updateAnimation(renderer.absoluteTimeMs);
            return true;
        } else if (!rotations.isEmpty()) {
            renderer.cube.beginLayerRotation(rotations.pop(),
                LAYER_ROTATION_DURATION_MS, renderer.absoluteTimeMs);
            return true;
        } else {
            return false;
        }
    }

    protected void requestRenderProvidedTime() {
        requestRender();
    }

    public void resetView() {
        synchronized (renderer.model) {
            Matrix.setIdentityM(renderer.model, 0);
            rotationX = rotationY = 0;
        }
        requestRenderProvidedTime();
    }

    private boolean updateCubeRotation(double elapsedMs) {
        float dt = (float)(elapsedMs / 100);
        synchronized (renderer.model) {
            Matrix.rotateM(renderer.model, 0, rotationX * dt, 0, 1, 0);
            Matrix.rotateM(renderer.model, 0, rotationY * dt,
                    renderer.model[RIGHT_X], renderer.model[RIGHT_Y], renderer.model[RIGHT_Z]);
        }
        rotationX *= Math.pow(0.1f, dt * 0.1f);
        rotationY *= Math.pow(0.1f, dt * 0.1f);
        return Math.abs(rotationX) + Math.abs(rotationY) > 1f;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        final float x = e.getX();
        final float y = e.getY();
        time = e.getEventTime();
        final long elapsed = time - previousTime;

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isRotating = false;
                originMoveX = x;
                originMoveY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                float dx = x - previousX;
                float dy = y - previousY;
                synchronized (renderer.model) {
                    if (renderer.model[UP_Y] < 0) { dx = -dx; }
                }
                rotationX = clamp(dx, -MAX_ROTATION_SPEED, MAX_ROTATION_SPEED);
                rotationY = clamp(dy, -MAX_ROTATION_SPEED, MAX_ROTATION_SPEED);
                update(elapsed);
                if (!isRotating && MathUtils.length(
                        x - originMoveX, y - originMoveY) < SELECTION_MOVE_THRESHOLD) {
                    isRotating = true;
                }
                break;
            case MotionEvent.ACTION_UP:
                if (!isRotating) {
                    queueEvent(new Runnable() {
                        @Override
                        public void run() {
                            final PartSideCoords coords = renderer.cube.locationAtPixel((int) x, (int) y);
                            final PartSideCoords currentSelection = renderer.cube.getSelection();
                            final String data = coords == null ? "nothing" : coords.toString();
                            getHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    if (coords != null && currentSelection != null) {
                                        Rotation rotation = renderer.cube.createRotationFromSides(
                                                currentSelection.location, currentSelection.side,
                                                coords.location, coords.side);
                                        if (rotation != null) {
                                            rotations.addLast(rotation);
                                            renderer.cube.setSelection(null);
                                        } else {
                                            renderer.cube.setSelection(coords);
                                        }
                                    } else {
                                        renderer.cube.setSelection(coords);
                                    }
                                    hasSelection = renderer.cube.getSelection() != null;
                                    update(elapsed);
                                    requestRenderProvidedTime();
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
