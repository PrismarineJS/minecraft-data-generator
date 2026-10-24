package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.Biome;

public class BiomesDataGenerator implements IDataGenerator {
    private static String guessBiomeDimensionFromCategory(Biome biome) {
        var biomeRegistry = DGU.getWorld().getRegistryManager().getOrThrow(RegistryKeys.BIOME);
        if (biomeRegistry.getEntry(biome).isIn(BiomeTags.IS_NETHER)) {
            return "nether";
        } else if (biomeRegistry.getEntry(biome).isIn(BiomeTags.IS_END)) {
            return "end";
        } else {
            return "overworld";
        }
    }

    private static String guessCategoryBasedOnName(String name, String dimension) {
        if (dimension.equals("nether")) {
            return "nether";
        } else if (dimension.equals("end")) {
            return "the_end";
        }

        if (name.contains("end")) {
            System.out.println();
        }

        if (name.contains("hills")) {
            return "extreme_hills";
        } else if (name.contains("ocean")) {
            return "ocean";
        } else if (name.contains("plains")) {
            return "plains";
        } else if (name.contains("ice") || name.contains("frozen")) {
            return "ice";
        } else if (name.contains("jungle")) {
            return "jungle";
        } else if (name.contains("desert")) {
            return "desert";
        } else if (name.contains("forest") || name.contains("grove")) {
            return "forest";
        } else if (name.contains("taiga")) {
            return "taiga";
        } else if (name.contains("swamp")) {
            return "swamp";
        } else if (name.contains("river")) {
            return "river";
        } else if (name.equals("the_end")) {
            return "the_end";
        } else if (name.contains("mushroom")) {
            return "mushroom";
        } else if (name.contains("beach") || name.equals("stony_shore")) {
            return "beach";
        } else if (name.contains("savanna")) {
            return "savanna";
        } else if (name.contains("badlands")) {
            return "mesa";
        } else if (name.contains("peaks") || name.equals("snowy_slopes") || name.equals("meadow")) {
            return "mountain";
        } else if (name.equals("the_void")) {
            return "none";
        } else if (name.contains("cave") || name.equals("deep_dark")) {
            return "underground";
        } else {
            System.out.println("Unable to find biome category for biome with name: '" + name + "'");
            return "none";
        }
    }

    public static JsonObject generateBiomeInfo(Registry<Biome> registry, Biome biome) {
        JsonObject biomeDesc = new JsonObject();
        Identifier registryKey = registry.getKey(biome).orElseThrow().getValue();
        String localizationKey = String.format("biome.%s.%s", registryKey.getNamespace(), registryKey.getPath());
        String name = registryKey.getPath();
        biomeDesc.addProperty("id", registry.getRawId(biome));
        biomeDesc.addProperty("name", name);
        String dimension = guessBiomeDimensionFromCategory(biome);
        biomeDesc.addProperty("category", guessCategoryBasedOnName(name, dimension));
        biomeDesc.addProperty("temperature", biome.getTemperature());
        //biomeDesc.addProperty("precipitation", biome.getPrecipitation().getName());// - removed in 1.19.4
        biomeDesc.addProperty("has_precipitation", biome.hasPrecipitation());
        //biomeDesc.addProperty("depth", biome.getDepth()); - Doesn't exist anymore in minecraft source
        biomeDesc.addProperty("dimension", dimension);
        biomeDesc.addProperty("displayName", DGU.translateText(localizationKey));
        biomeDesc.addProperty("color", biome.getSkyColor());
        //biomeDesc.addProperty("rainfall", biome.getDownfall());// - removed in 1.19.4

        return biomeDesc;
    }

    @Override
    public String getDataName() {
        return "biomes";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray biomesArray = new JsonArray();
        DynamicRegistryManager registryManager = DGU.getWorld().getRegistryManager();
        Registry<Biome> biomeRegistry = registryManager.getOrThrow(RegistryKeys.BIOME);

        biomeRegistry.stream()
                .map(biome -> generateBiomeInfo(biomeRegistry, biome))
                .forEach(biomesArray::add);
        return biomesArray;
    }
}
