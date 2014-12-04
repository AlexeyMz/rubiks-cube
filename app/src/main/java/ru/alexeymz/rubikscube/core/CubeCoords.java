package ru.alexeymz.rubikscube.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CubeCoords {
    public final int left;
    public final int top;
    public final int depth;

    public CubeCoords(int left, int top, int depth) {
        this.left = left;
        this.top = top;
        this.depth = depth;
    }

    public void getPositionInSpace(float[] vector, int vectorOffset, int cubeSize) {
        if (cubeSize <= 0)
            throw new IllegalArgumentException("cubeSize must be > 0.");

        float shift = ((float)cubeSize - 1) / 2;
        float scale = 2f / cubeSize;

        vector[vectorOffset + 0] = (left - shift) * scale;
        vector[vectorOffset + 1] = (-top + shift) * scale;
        vector[vectorOffset + 2] = (-depth + shift) * scale;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof CubeCoords)) return false;

        CubeCoords that = (CubeCoords) o;

        if (depth != that.depth) return false;
        if (left != that.left) return false;
        if (top != that.top) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = left;
        result = 31 * result + top;
        result = 31 * result + depth;
        return result;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%s; %s; %s", left, top, depth);
    }

    public static List<CubeCoords> enumerateCube(int size) {
        List<CubeCoords> coords = new ArrayList<CubeCoords>();
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                for (int k = 0; k < size; k++) {
                    coords.add(new CubeCoords(i, j, k));
                }
            }
        }
        return coords;
    }
}
