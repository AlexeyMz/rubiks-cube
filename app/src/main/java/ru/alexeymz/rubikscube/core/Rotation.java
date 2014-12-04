package ru.alexeymz.rubikscube.core;

public final class Rotation {
    public final Axis axis;
    public final int layer;
    public final boolean clockwise;

    public Rotation(Axis axis, int layer, boolean clockwise) {
        if (layer < 0)
            throw new IllegalArgumentException("layer must be >= 0.");
        this.axis = axis;
        this.layer = layer;
        this.clockwise = clockwise;
    }
}
