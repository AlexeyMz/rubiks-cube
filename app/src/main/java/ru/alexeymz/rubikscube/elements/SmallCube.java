package ru.alexeymz.rubikscube.elements;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.CubeSide;
import ru.alexeymz.rubikscube.core.Rotatable;
import ru.alexeymz.rubikscube.core.RotateUtils;
import ru.alexeymz.rubikscube.core.SixSided;

public class SmallCube implements SixSided<Integer>, Rotatable {
    private int[] data = new int[6];

    @Override
    public Integer get(CubeSide side) {
        return data[side.ordinal()];
    }

    @Override
    public void set(CubeSide side, Integer value) {
        data[side.ordinal()] = value;
    }

    @Override
    public void rotateAround(Axis axis) {
        RotateUtils.rotate(this, axis, false);
    }
}
