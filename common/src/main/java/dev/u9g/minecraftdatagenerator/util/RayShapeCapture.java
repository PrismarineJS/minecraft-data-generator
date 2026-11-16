package dev.u9g.minecraftdatagenerator.util;

import java.util.ArrayList;
import java.util.List;

/** Captures the boxes visited by a legacy block's native ray routine. */
public final class RayShapeCapture {
    private static final ThreadLocal<List<double[]>> ACTIVE = new ThreadLocal<>();

    public static void begin() {
        if (ACTIVE.get() != null) throw new IllegalStateException("Nested ray shape capture");
        ACTIVE.set(new ArrayList<>());
    }

    public static void add(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        List<double[]> shapes = ACTIVE.get();
        if (shapes != null) shapes.add(new double[]{minX, minY, minZ, maxX, maxY, maxZ});
    }

    public static List<double[]> end() {
        List<double[]> shapes = ACTIVE.get();
        ACTIVE.remove();
        if (shapes == null) throw new IllegalStateException("No active ray shape capture");
        return shapes;
    }

    private RayShapeCapture() {}
}
