package ru.alexeymz.rubikscube.view;

import ru.alexeymz.rubikscube.core.Axis;
import ru.alexeymz.rubikscube.core.CubeSide;
import ru.alexeymz.rubikscube.core.Rotatable;
import ru.alexeymz.rubikscube.core.RotateUtils;
import ru.alexeymz.rubikscube.core.SixSided;

public abstract class CubePart implements SixSided<Integer>, Rotatable {
    public final float[] world = new float[16];
}
