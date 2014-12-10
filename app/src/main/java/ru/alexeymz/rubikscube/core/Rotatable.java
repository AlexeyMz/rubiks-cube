package ru.alexeymz.rubikscube.core;

public interface Rotatable {
    /**
     * Counter-clockwise rotate around axis.
     */
    void rotateAround(Axis axis);
}
