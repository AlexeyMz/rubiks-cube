package ru.alexeymz.rubikscube.core;

public interface SixSided<T> {
    T get(CubeSide side);
    void set(CubeSide side, T value);
}
