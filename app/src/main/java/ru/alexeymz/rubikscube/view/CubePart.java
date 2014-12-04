package ru.alexeymz.rubikscube.view;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.CubeSide;
import ru.alexeymz.rubikscube.core.Rotatable;
import ru.alexeymz.rubikscube.core.RotateUtils;
import ru.alexeymz.rubikscube.core.SixSided;

public abstract class CubePart implements SixSided<Integer>, Rotatable {
    public final float[] world = new float[16];

    @Override
    public Integer get(CubeSide side) {
        return null;
    }

    @Override
    public void set(CubeSide side, Integer value) {

    }

    @Override
    public void rotateAround(Axis axis) {
        RotateUtils.rotate(this, axis, true);
    }
}
