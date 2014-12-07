package ru.alexeymz.rubikscube.view;

import java.util.Locale;

import ru.alexeymz.rubikscube.core.CubeCoords;
import ru.alexeymz.rubikscube.core.CubeSide;

public final class PartSideCoords {

    public final CubeCoords location;
    public final CubeSide side;

    public PartSideCoords(CubeCoords location, CubeSide side) {
        this.location = location;
        this.side = side;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "[%s | %s]", location, side);
    }
}
