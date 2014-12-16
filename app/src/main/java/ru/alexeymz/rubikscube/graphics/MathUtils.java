package ru.alexeymz.rubikscube.graphics;

import android.opengl.Matrix;

public final class MathUtils {
    private MathUtils() {}

    public static final int X = 12;
    public static final int Y = 13;
    public static final int Z = 14;
    public static final int W = 15;

    public static final int RIGHT_X = 0, RIGHT_Y = 4, RIGHT_Z = 8;
    public static final int UP_X = 1, UP_Y = 5, UP_Z = 9;
    public static final int LOOK_X = 2, LOOK_Y = 6, LOOK_Z = 10;

    private static final ThreadLocal<float[]> TEMP = new ThreadLocal<float[]>() {
        @Override
        protected float[] initialValue() {
            return new float[32];
        }
    };

    public static void copy(float[] source, float[] target, int itemCount) {
        copy(source, 0, target, 0, itemCount);
    }

    public static void copy(
        float[] source, int sourceOffset,
        float[] target, int targetOffset, int itemCount)
    {
        System.arraycopy(source, sourceOffset, target, targetOffset, itemCount);
    }

    public static void zero(float[] data, int offset, int itemCount) {
        int end = offset + itemCount;
        for (int i = offset; i < end; i++) {
            data[i] = 0;
        }
    }

    public static float length(float x, float y) {
        return (float)Math.sqrt(x * x + y * y);
    }

    public static float length(float x, float y, float z) {
        return (float)Math.sqrt(x * x + y * y + z * z);
    }

    public static void setPosition(float[] world, float x, float y, float z) {
        world[X] = x;
        world[Y] = y;
        world[Z] = z;
    }

    /**
     * Counter-clockwise rotation around point (x, y, z).
     * Accepts rotation angles in degrees.
     */
    public static void rotateAround(float[] world,
                                    float x, float y, float z,
                                    float xAngle, float yAngle, float zAngle) {
        world[X] -= x;
        world[Y] -= y;
        world[Z] -= z;
        float[] temp = TEMP.get();
        Matrix.setIdentityM(temp, 0);
        Matrix.rotateM(temp, 0, yAngle, 0, 1, 0);
        Matrix.rotateM(temp, 0, xAngle, 1, 0, 0);
        Matrix.rotateM(temp, 0, zAngle, 0, 0, 1);
        Matrix.multiplyMM(temp, 16, temp, 0, world, 0);
        copy(temp, 16, world, 0, 16);
        world[X] += x;
        world[Y] += y;
        world[Z] += z;
    }

    public static void relativeRotate(float[] world, float x, float y, float z) {
        float ox = world[X];
        float oy = world[Y];
        float oz = world[Z];
        setPosition(world, 0, 0, 0);
        Matrix.rotateM(world, 0, y, world[UP_X],    world[UP_Y],    world[UP_Z]);
        Matrix.rotateM(world, 0, x, world[RIGHT_X], world[RIGHT_Y], world[RIGHT_Z]);
        Matrix.rotateM(world, 0, z, world[LOOK_X],  world[LOOK_Y],  world[LOOK_Z]);
        float upR    = 1 / length(world[UP_X],    world[UP_Y],    world[UP_Z]);
        float rightR = 1 / length(world[RIGHT_X], world[RIGHT_Y], world[RIGHT_Z]);
        float lookR  = 1 / length(world[LOOK_X],  world[LOOK_Y],  world[LOOK_Z]);
        world[UP_X]    *= upR;    world[UP_Y]    *= upR;    world[UP_Z]    *= upR;
        world[RIGHT_X] *= rightR; world[RIGHT_Y] *= rightR; world[RIGHT_Z] *= rightR;
        world[LOOK_X]  *= lookR;  world[LOOK_Y]  *= lookR;  world[LOOK_Z]  *= lookR;
        setPosition(world, ox, oy, oz);
    }

    public static float toRadians(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    public static float toDegrees(float radians) {
        return (float) Math.toDegrees(radians);
    }

    public static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
