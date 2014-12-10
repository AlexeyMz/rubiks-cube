package ru.alexeymz.rubikscube.utils;

import java.util.Arrays;

public final class CollectionUtils {
    private CollectionUtils() {}

    public static String join(String separator, Object... items) {
        if (items == null) { return ""; }
        return join(separator, Arrays.asList(items));
    }

    public static String join(String separator, Iterable<?> items) {
        StringBuilder builder = new StringBuilder();
        boolean firstItem = true;
        for (Object item : items) {
            if (firstItem) { firstItem = false; }
            else { builder.append(separator); }
            builder.append(item);
        }
        return builder.toString();
    }
}
