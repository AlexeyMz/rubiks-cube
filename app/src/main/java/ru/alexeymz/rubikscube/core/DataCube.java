package ru.alexeymz.rubikscube.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DataCube<T> implements Rotatable {
    private final List<T> data;

    public final int size;

    public DataCube(int size) {
        this.size = size;
        int elementCount = size * size * size;
        this.data = new ArrayList<T>(elementCount);
        for (int i = 0; i < elementCount; i++) {
            data.add(null);
        }
    }

    public T get(int left, int top, int depth) {
        if (left  < 0 || left  >= size ||
            top   < 0 || top   >= size ||
            depth < 0 || depth >= size) {
            throw new IllegalArgumentException("left, top and depth must be > 0 and < size.");
        }
        return data.get(CubeCoords.toIndex(size, left, top, depth));
    }

    public void set(int left, int top, int depth, T value) {
        if (left  < 0 || left  >= size ||
            top   < 0 || top   >= size ||
            depth < 0 || depth >= size) {
            throw new IllegalArgumentException("left, top and depth must be > 0 and < size.");
        }
        data.set(CubeCoords.toIndex(size, left, top, depth), value);
    }

    public T get(CubeCoords coords) {
        return get(coords.left, coords.top, coords.depth);
    }

    public void set(CubeCoords coords, T value) {
        set(coords.left, coords.top, coords.depth, value);
    }

    @Override
    public void rotateAround(Axis axis) {
        for (int i = 0; i < size; i++) {
            rotateLayer(axis, i, true);
        }
    }

    public void rotateLayer(Axis axis, int layer, boolean clockwise) {
        if (layer < 0 || layer >= size)
            throw new IllegalArgumentException("layer must be in [0..size)");

        int rotations = clockwise ? 1 : 3;
        for (int i = 0; i < rotations; i++) {
            switch (axis) {
                case LEFT:
                    rotateLeftLayer(layer);
                    break;
                case TOP:
                    rotateTopLayer(layer);
                    break;
                case DEPTH:
                    rotateDepthLayer(layer);
                    break;
            }
        }
    }

    private void rotateLeftLayer(int left) {
        final int max = size - 1;
        for (int i = 0; i < size / 2; i++) {
            for (int j = i; j < max - i; j++) {
                T temp = get(left, i, j);
                set(left, i, j, get(left, max - j, i));
                set(left, max - j, i, get(left, max - i, max - j));
                set(left, max - i, max - j, get(left, j, max - i));
                set(left, j, max - i, temp);
            }
        }
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Object item = get(left, i, j);
                if (item instanceof Rotatable) {
                    ((Rotatable)item).rotateAround(Axis.LEFT);
                }
            }
        }
    }

    private void rotateTopLayer(int top) {
        final int max = size - 1;
        for (int i = 0; i < size / 2; i++) {
            for (int j = i; j < max - i; j++) {
                T temp = get(j, top, i);
                set(j, top, i, get(i, top, max - j));
                set(i, top, max - j, get(max - j, top, max - i));
                set(max - j, top, max - i, get(max - i, top, j));
                set(max - i, top, j, temp);
            }
        }
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Object item = get(i, top, j);
                if (item instanceof Rotatable) {
                    ((Rotatable)item).rotateAround(Axis.TOP);
                }
            }
        }
    }

    private void rotateDepthLayer(int depth) {
        final int max = size - 1;
        for (int i = 0; i < size / 2; i++) {
            for (int j = i; j < max - i; j++) {
                T temp = get(i, j, depth);
                set(i, j, depth, get(max - j, i, depth));
                set(max - j, i, depth, get(max - i, max - j, depth));
                set(max - i, max - j, depth, get(j, max - i, depth));
                set(j, max - i, depth, temp);
            }
        }
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                Object item = get(i, j, depth);
                if (item instanceof Rotatable) {
                    ((Rotatable)item).rotateAround(Axis.DEPTH);
                }
            }
        }
    }
}
