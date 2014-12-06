package ru.alexeymz.rubikscube.view;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.CubeSide;

public class EmptyCubePart extends CubePart {
    @Override
    public void rotateAround(Axis axis) {}

    @Override
    public Integer get(CubeSide side) { return null; }

    @Override
    public void set(CubeSide side, Integer value) {}
}
