package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import dev.u9g.minecraftdatagenerator.util.EmptyRenderBlockView;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.FoliageColors;

import java.util.*;

public class TintsDataGenerator implements IDataGenerator {
    public static BiomeTintColors generateBiomeTintColors(Registry<Biome> biomeRegistry) {
        BiomeTintColors colors = new BiomeTintColors();

        biomeRegistry.forEach(biome -> {
            int biomeGrassColor = biome.getGrassColorAt(0.0, 0.0);
            int biomeFoliageColor = biome.getFoliageColor();
            int biomeWaterColor = biome.getWaterColor();

            colors.grassColoursMap.computeIfAbsent(biomeGrassColor, k -> new ArrayList<>()).add(biome);
            colors.foliageColoursMap.computeIfAbsent(biomeFoliageColor, k -> new ArrayList<>()).add(biome);
            colors.waterColourMap.computeIfAbsent(biomeWaterColor, k -> new ArrayList<>()).add(biome);
        });
        return colors;
    }

    public static Map<Integer, Integer> generateRedstoneTintColors() {
        Map<Integer, Integer> resultColors = new LinkedHashMap<>();

        for (int redstoneLevel : RedstoneWireBlock.POWER.getValues()) {
            // Remove the unintended alpha channel from the redstone tint color
            int color = removeAlphaChannel(RedstoneWireBlock.getWireColor(redstoneLevel));
            resultColors.put(redstoneLevel, color);
        }
        return resultColors;
    }

    private static int removeAlphaChannel(int color) {
        float r = (float) (color >> 16 & 0xFF) / 255;
        float g = (float) (color >> 8 & 0xFF) / 255;
        float b = (float) (color & 0xFF) / 255;
        return ColorHelper.fromFloats(0, r, g, b);
    }

    private static int getBlockColor(Block block) {
        return BlockColors.create().getColor(block.getDefaultState(), EmptyRenderBlockView.INSTANCE, BlockPos.ORIGIN, 0xFFFFFF);
    }

    public static Map<Block, Integer> generateConstantTintColors() {
        Map<Block, Integer> resultColors = new LinkedHashMap<>();

        resultColors.put(Blocks.BIRCH_LEAVES, FoliageColors.BIRCH);
        resultColors.put(Blocks.SPRUCE_LEAVES, FoliageColors.SPRUCE);

        resultColors.put(Blocks.LILY_PAD, getBlockColor(Blocks.LILY_PAD));
        resultColors.put(Blocks.ATTACHED_MELON_STEM, getBlockColor(Blocks.ATTACHED_MELON_STEM));
        resultColors.put(Blocks.ATTACHED_PUMPKIN_STEM, getBlockColor(Blocks.ATTACHED_PUMPKIN_STEM));

        //not really constant, depend on the block age, but kinda have to be handled since textures are literally white without them
        resultColors.put(Blocks.MELON_STEM, getBlockColor(Blocks.MELON_STEM));
        resultColors.put(Blocks.PUMPKIN_STEM, getBlockColor(Blocks.PUMPKIN_STEM));

        return resultColors;
    }

    private static JsonObject encodeBiomeColorMap(Registry<Biome> biomeRegistry, Map<Integer, List<Biome>> colorsMap) {
        JsonArray resultColorsArray = new JsonArray();
        for (var entry : colorsMap.entrySet()) {
            JsonObject entryObject = new JsonObject();

            JsonArray keysArray = new JsonArray();
            for (Biome biome : entry.getValue()) {
                Identifier registryKey = biomeRegistry.getKey(biome).orElseThrow().getValue();
                keysArray.add(registryKey.getPath());
            }

            entryObject.add("keys", keysArray);
            entryObject.addProperty("color", entry.getKey());
            resultColorsArray.add(entryObject);
        }

        JsonObject resultObject = new JsonObject();
        resultObject.add("data", resultColorsArray);
        return resultObject;
    }

    private static JsonObject encodeRedstoneColorMap(Map<Integer, Integer> colorsMap) {
        JsonArray resultColorsArray = new JsonArray();
        for (var entry : colorsMap.entrySet()) {
            JsonObject entryObject = new JsonObject();

            JsonArray keysArray = new JsonArray();
            keysArray.add(entry.getKey());

            entryObject.add("keys", keysArray);
            entryObject.addProperty("color", entry.getValue());
            resultColorsArray.add(entryObject);
        }

        JsonObject resultObject = new JsonObject();
        resultObject.add("data", resultColorsArray);
        return resultObject;
    }

    private static JsonObject encodeBlocksColorMap(Registry<Block> blockRegistry, Map<Block, Integer> colorsMap) {
        JsonArray resultColorsArray = new JsonArray();
        for (var entry : colorsMap.entrySet()) {
            JsonObject entryObject = new JsonObject();

            JsonArray keysArray = new JsonArray();
            Identifier registryKey = blockRegistry.getKey(entry.getKey()).orElseThrow().getValue();
            keysArray.add(registryKey.getPath());

            entryObject.add("keys", keysArray);
            entryObject.addProperty("color", entry.getValue());
            resultColorsArray.add(entryObject);
        }

        JsonObject resultObject = new JsonObject();
        resultObject.add("data", resultColorsArray);
        return resultObject;
    }

    @Override
    public String getDataName() {
        return "tints";
    }

    @Override
    public JsonObject generateDataJson() {
        DynamicRegistryManager registryManager = DGU.getWorld().getRegistryManager();
        Registry<Biome> biomeRegistry = registryManager.getOrThrow(RegistryKeys.BIOME);
        Registry<Block> blockRegistry = registryManager.getOrThrow(RegistryKeys.BLOCK);

        BiomeTintColors biomeTintColors = generateBiomeTintColors(biomeRegistry);
        Map<Integer, Integer> redstoneColors = generateRedstoneTintColors();
        Map<Block, Integer> constantTintColors = generateConstantTintColors();

        JsonObject resultObject = new JsonObject();

        resultObject.add("grass", encodeBiomeColorMap(biomeRegistry, biomeTintColors.grassColoursMap));
        resultObject.add("foliage", encodeBiomeColorMap(biomeRegistry, biomeTintColors.foliageColoursMap));
        resultObject.add("water", encodeBiomeColorMap(biomeRegistry, biomeTintColors.waterColourMap));

        resultObject.add("redstone", encodeRedstoneColorMap(redstoneColors));
        resultObject.add("constant", encodeBlocksColorMap(blockRegistry, constantTintColors));

        return resultObject;
    }

    public static class BiomeTintColors {
        final Map<Integer, List<Biome>> grassColoursMap = new LinkedHashMap<>();
        final Map<Integer, List<Biome>> foliageColoursMap = new LinkedHashMap<>();
        final Map<Integer, List<Biome>> waterColourMap = new LinkedHashMap<>();
    }
}
