package ru.alexeymz.rubikscube;

import static ru.alexeymz.rubikscube.graphics.MathUtils.*;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

import javax.microedition.khronos.opengles.GL10;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.Rotation;
import ru.alexeymz.rubikscube.view.PartSideCoords;

public class CubeSurfaceView extends GLSurfaceView {
    public static final String IN_UNDO_MODE_PROPERTY = "IN_UNDO_MODE";

    private static final float MAX_ROTATION_SPEED = 1000;
    private static final double LAYER_ROTATION_DURATION_MS = 500;
    private static final double UNDO_ROTATION_DURATION_MS = 200;

    private PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    private final CubeRenderer renderer;

    private GestureDetector detector;

    private long startTime, time, previousTime;

    private boolean hasSelection = false;
    private boolean inUndoMode = false;
    private float rotationX, rotationY;

    private final Deque<Rotation> rotations = new ArrayDeque<Rotation>();
    private final Deque<Rotation> undoStack = new ArrayDeque<Rotation>();

    private Random random = new Random();

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
        detector = new GestureDetector(context, createGestureListener());
        setEGLContextClientVersion(2);
        this.renderer = new CubeRenderer() {
            @Override
            public void onDrawFrame(GL10 unused) {
                super.onDrawFrame(unused);
                post(frameRendered);
            }
        };
        setRenderer(renderer);
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        startTime = SystemClock.uptimeMillis();
        pcs.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                post(frameRendered);
            }
        });
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        pcs.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        pcs.removePropertyChangeListener(listener);
    }

    public boolean isInUndoMode() {
        return inUndoMode;
    }

    public void setInUndoMode(boolean value) {
        boolean oldValue = inUndoMode;
        inUndoMode = value;
        pcs.firePropertyChange(IN_UNDO_MODE_PROPERTY, oldValue, inUndoMode);
    }

    public void randomize(int rotationCount) {
        Axis lastAxis = null;
        int lastLayer = -1;
        while (rotationCount > 0) {
            Axis axis = Axis.fromOrdinal(random.nextInt(Axis.ordinalCount()));
            int layer = random.nextInt(renderer.cube.size());
            if (axis == lastAxis && layer == lastLayer) { continue; }
            boolean clockwise = random.nextBoolean();
            rotations.addLast(new Rotation(axis, layer, clockwise));
            lastAxis = axis;
            lastLayer = layer;
            rotationCount--;
        }
        post(frameRendered);
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
            Rotation rotation = rotations.pop();
            undoStack.push(rotation.inverse());
            renderer.cube.beginLayerRotation(rotation,
                LAYER_ROTATION_DURATION_MS, renderer.absoluteTimeMs);
            return true;
        } else if (inUndoMode) {
            if (undoStack.isEmpty()) {
                setInUndoMode(false);
                return false;
            } else {
                renderer.cube.beginLayerRotation(undoStack.pop(),
                    UNDO_ROTATION_DURATION_MS, renderer.absoluteTimeMs);
                return true;
            }
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
        float dt = clamp((float)elapsedMs, 0, 48) / 100;
        synchronized (renderer.model) {
            Matrix.rotateM(renderer.model, 0, rotationX * dt, 0, 1, 0);
            Matrix.rotateM(renderer.model, 0, rotationY * dt,
                    renderer.model[RIGHT_X], renderer.model[RIGHT_Y], renderer.model[RIGHT_Z]);
        }
        rotationX *= Math.pow(0.1f, dt * 0.1f);
        rotationY *= Math.pow(0.1f, dt * 0.1f);
        return Math.abs(rotationX) + Math.abs(rotationY) > 1f;
    }

    private GestureDetector.OnGestureListener createGestureListener() {
        return new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                previousTime = time = e.getEventTime();
                return true;
            }

            @Override
            public void onShowPress(MotionEvent e) {
                super.onShowPress(e);
            }

            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                time = e.getEventTime();
                final long elapsed = time - previousTime;
                final float x = e.getX();
                final float y = e.getY();
                queueEvent(new Runnable() {
                    @Override
                    public void run() {
                        final PartSideCoords coords = renderer.cube.locationAtPixel((int) x, (int) y);
                        final PartSideCoords currentSelection = renderer.cube.getSelection();
                        post(new Runnable() {
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
                            }
                        });
                    }
                });
                previousTime = time;
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
                time = e2.getEventTime();
                final long elapsed = time - previousTime;
                float dx = -distanceX, dy = -distanceY;
                synchronized (renderer.model) {
                    if (renderer.model[UP_Y] < 0) { dx = -dx; }
                }
                rotationX = clamp(dx, -MAX_ROTATION_SPEED, MAX_ROTATION_SPEED);
                rotationY = clamp(dy, -MAX_ROTATION_SPEED, MAX_ROTATION_SPEED);
                update(elapsed);
                previousTime = time;
                return true;
            }
        };
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        return detector.onTouchEvent(e);
    }
}
