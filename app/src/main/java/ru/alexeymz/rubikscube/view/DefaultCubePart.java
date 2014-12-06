package ru.alexeymz.rubikscube.view;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.CubeSide;
import ru.alexeymz.rubikscube.core.RotateUtils;

public class DefaultCubePart extends CubePart {
    private final int[] sides = new int[CubeSide.values().length];

    @Override
    public Integer get(CubeSide side) {
        return sides[side.ordinal()];
    }

    @Override
    public void set(CubeSide side, Integer value) {
        sides[side.ordinal()] = value;
    }

    @Override
    public void rotateAround(Axis axis) {
        RotateUtils.rotate(this, axis, true);
    }
}
