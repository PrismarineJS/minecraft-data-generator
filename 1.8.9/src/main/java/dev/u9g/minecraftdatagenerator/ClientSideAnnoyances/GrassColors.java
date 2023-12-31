package dev.u9g.minecraftdatagenerator.ClientSideAnnoyances;

import net.minecraft.util.math.MathHelper;
import net.minecraft.world.biome.Biome;

public class GrassColors {
    private static int[] colorMap = new int[65536];

    public static void setColorMap(int[] map) {
        colorMap = map;
    }

    public static int getColor(double temperature, double humidity) {
        humidity *= temperature;
        int i = (int) ((1.0D - temperature) * 255.0D);
        int j = (int) ((1.0D - humidity) * 255.0D);
        int k = j << 8 | i;
        return k > colorMap.length ? -65281 : colorMap[k];
    }

    public static int getGrassColor(Biome biome) {
        double d = MathHelper.clamp(biome.temperature, 0.0f, 1.0f);
        double e = MathHelper.clamp(biome.downfall, 0.0f, 1.0f);
        return GrassColors.getColor(d, e);
    }
}



