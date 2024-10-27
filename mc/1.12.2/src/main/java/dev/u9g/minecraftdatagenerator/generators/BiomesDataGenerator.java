package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.mixin.BiomeAccessor;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.biome.*;

import java.util.Locale;

public class BiomesDataGenerator implements IDataGenerator {

    private static String guessBiomeDimensionFromCategory(Biome biome) {
        if (biome instanceof NetherBiome) {
            return "nether";
        } else if (biome instanceof EndBiome) {
            return "end";
        }
        return "overworld";
    }

    public static JsonObject generateBiomeInfo(SimpleRegistry<Identifier, Biome> registry, Biome biome) {
        JsonObject biomeDesc = new JsonObject();

        String name = String.join("_", ((BiomeAccessor) biome).name().toLowerCase(Locale.ENGLISH).split(" "));
        String displayName = ((BiomeAccessor) biome).name();
        biomeDesc.addProperty("id", registry.getRawId(biome));
        biomeDesc.addProperty("name", name);
        biomeDesc.addProperty("category", category(biome));
        biomeDesc.addProperty("temperature", biome.getTemperature());
        biomeDesc.addProperty("precipitation", precipitation(biome));
        biomeDesc.addProperty("depth", biome.getDepth());
        biomeDesc.addProperty("dimension", guessBiomeDimensionFromCategory(biome));
        biomeDesc.addProperty("displayName", displayName);
        biomeDesc.addProperty("color", biome.getSkyColor(biome.getTemperature()));
        biomeDesc.addProperty("rainfall", biome.getRainfall());

        return biomeDesc;
    }

    private static String category(Biome biome) {
        if (biome instanceof ForestBiome) {
            return "forest";
        } else if (biome instanceof OceanBiome) {
            return "ocean";
        } else if (biome instanceof PlainsBiome) {
            return "plains";
        } else if (biome instanceof DesertBiome) {
            return "desert";
        } else if (biome instanceof ExtremeHillsBiome) {
            return "extreme_hills";
        } else if (biome instanceof TaigaBiome) {
            return "taiga";
        } else if (biome instanceof SwampBiome) {
            return "swamp";
        } else if (biome instanceof RiverBiome) {
            return "river";
        } else if (biome instanceof NetherBiome) {
            return "nether";
        } else if (biome instanceof EndBiome) {
            return "the_end";
        } else if (biome instanceof IceBiome) {
            return "icy";
        } else if (biome instanceof MushroomBiome) {
            return "mushroom";
        } else if (biome instanceof BeachBiome) {
            return "beach";
        } else if (biome instanceof JungleBiome) {
            return "jungle";
        } else if (biome instanceof SavannaBiome) {
            return "savanna";
        } else if (biome instanceof MesaBiome) {
            return "mesa";
        } else if (biome instanceof StoneBeachBiome || biome instanceof VoidBiome) {
            return "none"; // Should StoneBeachBiome be beach too? this is how it is now in mcdata
        }
        throw new IllegalStateException("Unable to find biome category for " + biome.getClass().getName());
    }

    private static String precipitation(Biome biome) {
        float rainfall = biome.getRainfall();
        float temperature = biome.getTemperature();
        if (rainfall == 0) {
            return "none";
        } else if (temperature < 0.2f) {
            return "snow";
        }
        return "rain";
    }

    @Override
    public String getDataName() {
        return "biomes";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray biomesArray = new JsonArray();
        SimpleRegistry<Identifier, Biome> biomeRegistry = Biome.REGISTRY;

        for (Biome biome : biomeRegistry) {
            biomesArray.add(generateBiomeInfo(biomeRegistry, biome));
        }
        return biomesArray;
    }
}
