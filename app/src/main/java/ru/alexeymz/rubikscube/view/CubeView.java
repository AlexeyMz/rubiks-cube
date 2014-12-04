package ru.alexeymz.rubikscube.view;

import static ru.alexeymz.rubikscube.graphics.MathUtils.*;

import android.opengl.Matrix;

import java.util.Collections;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.CubeCoords;
import ru.alexeymz.rubikscube.core.CubeSide;
import ru.alexeymz.rubikscube.core.DataCube;
import ru.alexeymz.rubikscube.core.Rotation;
import ru.alexeymz.rubikscube.elements.SmallCube;

public class CubeView {
    private DataCube<CubePart> viewCube;

    private int animatedLayer;
    private Axis animatedAxis;

    private float rotationAngle;
    private int stepsLeft = 0;

    public CubeView(DataCube<SmallCube> model, int[] colorMap, boolean removeBlackParts) {
        if (colorMap == null)
            throw new IllegalArgumentException("colorMap");

        viewCube = new DataCube<CubePart>(model.size);
        float[] partTransform = new float[16];
        Matrix.setIdentityM(partTransform, 0);
        float scaleFactor = 0.9f * 2f / model.size;
        Matrix.scaleM(partTransform, 0, scaleFactor, scaleFactor, scaleFactor);
        EmptyCubePart emptyPart = new EmptyCubePart();

        float[] position = new float[3];

        int maxIndex = model.size - 1;
        for (int i = 0; i < model.size; i++) {
            for (int j = 0; j < model.size; j++) {
                for (int k = 0; k < model.size; k++) {
                    boolean isInnerCube = i != 0 && j != 0 && k != 0 &&
                        i != maxIndex && j != maxIndex && k != maxIndex;

                    if (model.get(i, j, k) == null || removeBlackParts && isInnerCube) {
                        viewCube.set(i, j, k, emptyPart);
                    } else {
                        CubePart part = new EmptyCubePart();
                        paintCubePart(part, model.get(i, j, k), colorMap);

                        copy(partTransform, part.world, 16);
                        new CubeCoords(i, j, k).getPositionInSpace(position, 0, model.size);

                        setPosition(part.world, position[0], position[1], position[2]);

                        viewCube.set(i, j, k, part);
                    }
                }
            }
        }
    }

    public boolean isAnimationInProgress() {
        return stepsLeft > 0;
    }

    private void paintCubePart(CubePart part, SmallCube model, int[] colorMap) {
//        for (CubeSide side : CubeSide.values()) {
//            Triangle[] sidePolygons = part.get(side);
//            int sideColor = colorMap[model.get(side)];
//            for (int j = 0; j < sidePolygons.length; j++) {
//                sidePolygons[j].Color = sideColor;
//            }
//        }
    }

    public void beginLayerRotation(
            Rotation rotation, int stepCount) {
        if (rotation.layer >= viewCube.size)
            throw new IllegalArgumentException("rotatedLayer must be in [0..size)");
        if (stepCount <= 0)
            throw new IllegalArgumentException("stepCount must be >= 0.");
        if (isAnimationInProgress())
            throw new IllegalStateException("Animation already in progress.");

        animatedAxis = rotation.axis;
        animatedLayer = rotation.layer;
        stepsLeft = stepCount;

        rotationAngle = (float)Math.PI / (2 * stepCount) * (rotation.clockwise ? +1 : -1);
    }

    private void rotateLayer(float angle) {
        for (int i = 0; i < viewCube.size; i++) {
            for (int j = 0; j < viewCube.size; j++) {
                switch (animatedAxis) {
                    case LEFT:
                        rotateAround(viewCube.get(animatedLayer, i, j).world,
                                0, 0, 0, angle, 0, 0);
                        break;
                    case TOP:
                        rotateAround(viewCube.get(i, animatedLayer, j).world,
                                0, 0, 0, 0, -angle, 0);
                        break;
                    case DEPTH:
                        rotateAround(viewCube.get(i, j, animatedLayer).world,
                                0, 0, 0, 0, 0, -angle);
                        break;
                }
            }
        }
    }

    public void updateAnimation() {
        if (!isAnimationInProgress()) { return; }

        rotateLayer(rotationAngle);

        stepsLeft--;
        if (stepsLeft == 0) {
            endRotation();
        }
    }

    public void endAnimation() {
        if (!isAnimationInProgress())
            throw new IllegalStateException("No animation in progress.");

        float rotationLeft = rotationAngle * stepsLeft;
        rotateLayer(rotationLeft);

        stepsLeft = 0;
        endRotation();
    }

    private void endRotation() {
        viewCube.rotateLayer(animatedAxis, animatedLayer, rotationAngle > 0);
    }
}
