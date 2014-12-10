package ru.alexeymz.rubikscube.core;

/**
 * Represents an axis in cube.
 * In cube:
 *
 *    depth
 *    /
 *   o--left
 *   |
 *  top
 *
 * In 3D-space:
 *
 *            *     *        Y
 *            |    /         |
 *           top  depth      o---X
 *            | /           /
 *   *--left--o            Z
 */
public enum Axis {
    /**
     * Axis that goes from left to right.
     */
    LEFT {
        @Override
        public int getLayerFrom(CubeCoords coords) {
            return coords.left;
        }
    },
    /**
     * Axis that goes from top to depth.
     */
    TOP {
        @Override
        public int getLayerFrom(CubeCoords coords) {
            return coords.top;
        }
    },
    /**
     * Axis that goes from front to back.
     */
    DEPTH {
        @Override
        public int getLayerFrom(CubeCoords coords) {
            return coords.depth;
        }
    };

    public abstract int getLayerFrom(CubeCoords coords);

    private static final Axis[] values = values();

    public static Axis fromOrdinal(int ordinal) {
        return values[ordinal];
    }
}
