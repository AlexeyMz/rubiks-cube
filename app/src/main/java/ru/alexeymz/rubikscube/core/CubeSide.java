package ru.alexeymz.rubikscube.core;

public enum CubeSide {
    LEFT  { @Override public Axis axis() { return Axis.LEFT; } },
    RIGHT { @Override public Axis axis() { return Axis.LEFT; } },
    UP    { @Override public Axis axis() { return Axis.TOP; } },
    DOWN  { @Override public Axis axis() { return Axis.TOP; } },
    FRONT { @Override public Axis axis() { return Axis.DEPTH; } },
    BACK  { @Override public Axis axis() { return Axis.DEPTH; } };

    public abstract Axis axis();

    private static final CubeSide[] values = values();

    public static CubeSide fromOrdinal(int ordinal) {
        return values[ordinal];
    }
}
