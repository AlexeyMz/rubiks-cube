package ru.alexeymz.rubikscube.core;

public final class RotateUtils {
    private RotateUtils() {}

    public static <T> void rotate(SixSided<T> cube, Axis axis, boolean clockwise) {
        int iterations = clockwise ? 3 : 1;
        for (int i = 0; i < iterations; i++) {
            switch (axis) {
                case LEFT:
                    rotateAroundLeft(cube);
                    break;
                case TOP:
                    rotateAroundTop(cube);
                    break;
                case DEPTH:
                    rotateAroundDepth(cube);
                    break;
            }
        }
    }

    /**
     * Counter-clockwise rotation around left axis.
     */
    private static <T> void rotateAroundLeft(SixSided<T> cube) {
        T temp = cube.get(CubeSide.UP);
        cube.set(CubeSide.UP, cube.get(CubeSide.FRONT));
        cube.set(CubeSide.FRONT, cube.get(CubeSide.DOWN));
        cube.set(CubeSide.DOWN, cube.get(CubeSide.BACK));
        cube.set(CubeSide.BACK, temp);
    }

    /**
     * Counter-clockwise rotation around top axis.
     */
    private static <T> void rotateAroundTop(SixSided<T> cube) {
        T temp = cube.get(CubeSide.FRONT);
        cube.set(CubeSide.FRONT, cube.get(CubeSide.LEFT));
        cube.set(CubeSide.LEFT, cube.get(CubeSide.BACK));
        cube.set(CubeSide.BACK, cube.get(CubeSide.RIGHT));
        cube.set(CubeSide.RIGHT, temp);
    }

    /**
     * Counter-clockwise rotation around depth axis.
     */
    private static <T> void rotateAroundDepth(SixSided<T> cube) {
        T temp = cube.get(CubeSide.UP);
        cube.set(CubeSide.UP, cube.get(CubeSide.LEFT));
        cube.set(CubeSide.LEFT, cube.get(CubeSide.DOWN));
        cube.set(CubeSide.DOWN, cube.get(CubeSide.RIGHT));
        cube.set(CubeSide.RIGHT, temp);
    }
}
