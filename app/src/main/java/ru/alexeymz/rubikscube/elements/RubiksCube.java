package ru.alexeymz.rubikscube.elements;

import android.graphics.Color;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.CubeCoords;
import ru.alexeymz.rubikscube.core.CubeSide;
import ru.alexeymz.rubikscube.core.DataCube;
import ru.alexeymz.rubikscube.core.Rotation;
import ru.alexeymz.rubikscube.view.CubeView;
import ru.alexeymz.rubikscube.view.PartSideCoords;

public final class RubiksCube {
    private static int[] defaultColors = new int[] {
        Color.BLACK,
        Color.RED,
        Color.rgb(255, 127, 0),
        Color.BLUE,
        Color.GREEN,
        Color.WHITE,
        Color.YELLOW,
    };

    private static final CubeSide[][] rotationRules = new CubeSide[][] {
        // left
        { CubeSide.UP, CubeSide.BACK },
        { CubeSide.BACK, CubeSide.DOWN },
        { CubeSide.DOWN, CubeSide.FRONT },
        { CubeSide.FRONT, CubeSide.UP },
        // top
        { CubeSide.LEFT, CubeSide.FRONT },
        { CubeSide.FRONT, CubeSide.RIGHT },
        { CubeSide.RIGHT, CubeSide.BACK },
        { CubeSide.BACK, CubeSide.LEFT },
        // depth
        { CubeSide.UP, CubeSide.RIGHT },
        { CubeSide.RIGHT, CubeSide.DOWN },
        { CubeSide.DOWN, CubeSide.LEFT },
        { CubeSide.LEFT, CubeSide.UP },
    };

    private DataCube<SmallCube> dataCube;
    private CubeView view;
    private int[] colorMap;

    private volatile PartSideCoords selection;

    public RubiksCube(int size, int screenWidth, int screenHeight) {
        if (size <= 0)
            throw new IllegalArgumentException("size must be > 0.");
        if (colorMap == null) { colorMap = defaultColors; }
        if (colorMap.length < 7) {
            throw new IllegalArgumentException(
                "colorMap.Length must be not less than 7 (non-color + face colors).");
        }

        dataCube = getDefaultCube(size);
        view = new CubeView(this, dataCube, colorMap, true, screenWidth, screenHeight);
    }

    public int size() {
        return dataCube.size;
    }

    public boolean isAnimationInProgress() {
        return view.isAnimationInProgress();
    }

    private static SmallCube setCubeSide(SmallCube cube, CubeSide side, int value) {
        if (cube == null) {
            cube = new SmallCube();
        }
        cube.set(side, value);
        return cube;
    }

    public static DataCube<SmallCube> getDefaultCube(int size) {
        if (size <= 0)
            throw new IllegalArgumentException("size must be > 0.");

        DataCube<SmallCube> bigCube = new DataCube<SmallCube>(size);
        int maxIndex = bigCube.size - 1;

        for (int i = 0; i < bigCube.size; i++) {
            for (int j = 0; j < bigCube.size; j++) {
                for (int k = 0; k < bigCube.size; k++) {
                    SmallCube cube = null;
                    if (i == 0) { cube = setCubeSide(cube, CubeSide.LEFT, 1); }
                    if (j == 0) { cube = setCubeSide(cube, CubeSide.UP, 3); }
                    if (k == 0) { cube = setCubeSide(cube, CubeSide.FRONT, 5); }
                    if (i == maxIndex) { cube = setCubeSide(cube, CubeSide.RIGHT, 2); }
                    if (j == maxIndex) { cube = setCubeSide(cube, CubeSide.DOWN, 4); }
                    if (k == maxIndex) { cube = setCubeSide(cube, CubeSide.BACK, 6); }
                    bigCube.set(i, j, k, cube);
                }
            }
        }

        return bigCube;
    }

    public PartSideCoords getSelection() {
        return selection;
    }

    public void setSelection(PartSideCoords selection) {
        this.selection = selection;
    }

    public void beginLayerRotation(Rotation rotation, double durationMs, double currentTimeMs) {
        if (isAnimationInProgress()) {
            endAnimation();
        }
        view.beginLayerRotation(rotation, durationMs, currentTimeMs);
        dataCube.rotateLayer(rotation.axis, rotation.layer, rotation.clockwise);
    }

    public void updateAnimation(double absoluteTimeMs) {
        view.updateAnimation(absoluteTimeMs);
    }

    public void endAnimation() {
        view.endAnimation();
    }

    public Rotation createRotationFromSides(
        CubeCoords a, CubeSide sa,
        CubeCoords b, CubeSide sb)
    {
        if (sa == sb && !a.equals(b)) {
            EqualsWay equalsWay = getEqualsWay(sa,
                    a.left, a.top, a.depth,
                    b.left, b.top, b.depth);
            if (equalsWay == null) { return null; }
            Axis rotation = equalsWay.rotation;
            int layer = rotation.getLayerFrom(a);
            return new Rotation(rotation, layer, equalsWay.clockwise);
        } else if (sa != sb && a.equals(b)) {
            int sum = Axis.LEFT.ordinal() + Axis.TOP.ordinal() + Axis.DEPTH.ordinal();
            Axis rotation = Axis.fromOrdinal(sum - sa.axis().ordinal() - sb.axis().ordinal());
            int layer = rotation.getLayerFrom(a);
            boolean clockwise = true;
            for (CubeSide[] rotationRule : rotationRules) {
                if (rotationRule[0] == sa && rotationRule[1] == sb) {
                    clockwise = false;
                    break;
                }
            }
            return new Rotation(rotation, layer, clockwise);
        } else {
            return null;
        }
    }

    private static class EqualsWay {
        public final Axis rotation;
        public final boolean clockwise;

        private EqualsWay(Axis rotation, boolean clockwise) {
            this.rotation = rotation;
            this.clockwise = clockwise;
        }
    }

    private EqualsWay getEqualsWay(CubeSide side,
        int left1, int top1, int depth1,
        int left2, int top2, int depth2)
    {
        Axis rotation = null;
        boolean clockwise = false;

        if (side == CubeSide.LEFT || side == CubeSide.RIGHT) {
            if (top1 == top2) {
                rotation = Axis.TOP;
                clockwise = depth1 < depth2;
            } else if (depth1 == depth2) {
                rotation = Axis.DEPTH;
                clockwise = top2 > top1;
            }
        } else if (side == CubeSide.UP || side == CubeSide.DOWN) {
            if (left1 == left2) {
                rotation = Axis.LEFT;
                clockwise = depth1 > depth2;
            } else if (depth1 == depth2) {
                rotation = Axis.DEPTH;
                clockwise = left1 > left2;
            }
        } else if (side == CubeSide.FRONT || side == CubeSide.BACK) {
            if (left1 == left2) {
                rotation = Axis.LEFT;
                clockwise = top1 < top2;
            } else if (top1 == top2) {
                rotation = Axis.TOP;
                clockwise = left1 > left2;
            }
        }

        clockwise ^= side == CubeSide.RIGHT || side == CubeSide.DOWN || side == CubeSide.BACK;
        return rotation == null ? null : new EqualsWay(rotation, clockwise);
    }

    public void draw(float[] mvp, double absoluteTimeMs) {
        view.draw(mvp, absoluteTimeMs);
    }

    public PartSideCoords locationAtPixel(int x, int y) {
        return view.locationAtPixel(x, y);
    }
}
