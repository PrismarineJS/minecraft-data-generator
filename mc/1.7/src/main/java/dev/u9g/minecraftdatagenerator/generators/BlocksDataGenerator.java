package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.mixin.accessor.BlockAccessor;
import dev.u9g.minecraftdatagenerator.mixin.accessor.MiningToolItemAccessor;
import dev.u9g.minecraftdatagenerator.util.DGU;
import dev.u9g.minecraftdatagenerator.util.Registries;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.TransparentBlock;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BlocksDataGenerator implements IDataGenerator {
    private static List<Item> getItemsEffectiveForBlock(Block block) {
        List<Item> items = new ArrayList<>();
        for (Item item : Registries.ITEMS) {
            if (item instanceof ToolItem && ((MiningToolItemAccessor) item).getEffectiveBlocks().contains(block)) {
                items.add(item);
            }
        }
        return items;
    }

    private static List<ItemStack> populateDropsIfPossible(Item firstToolItem) {
        return new ArrayList<>();
    }

    public static JsonObject generateBlock(Block block) {
        JsonObject blockDesc = new JsonObject();

        String registryKey = Registries.BLOCKS.getId(block);
        List<Item> effectiveTools = getItemsEffectiveForBlock(block);

        blockDesc.addProperty("id", Registries.BLOCKS.getRawId(block));
        blockDesc.addProperty("name", Objects.requireNonNull(registryKey));
        if (!block.getTranslatedName().startsWith("tile.")) {
            blockDesc.addProperty("displayName", block.getTranslatedName());
        }

        float hardness = block.method_471(null, 0, 0, 0);

        blockDesc.addProperty("hardness", hardness);
        blockDesc.addProperty("resistance", ((BlockAccessor) block).getBlastResistance());
        blockDesc.addProperty("diggable", hardness != -1.0f && !(block instanceof AirBlock) && !block.getMaterial().isFluid());
        JsonObject effTools = new JsonObject();
        effectiveTools.forEach(item -> effTools.addProperty(
                String.valueOf(Registries.ITEMS.getRawId(item)), // key
                item.getMiningSpeedMultiplier(DGU.stackFor(item), block) // value
        ));
        blockDesc.add("effectiveTools", effTools);
        blockDesc.addProperty("transparent", block instanceof TransparentBlock);
        blockDesc.addProperty("emitLight", block.getLightLevel());
        blockDesc.addProperty("filterLight", block.getOpacity());

        blockDesc.addProperty("boundingBox", boundingBox(block));

        return blockDesc;
    }

    private static String boundingBox(Block block) {
        if (block.getBoundingBox(DGU.getWorld(), 0, 0, 0) == null) {
            return "empty";
        }
        return "block";
    }

    private static Item getItemFromBlock(Block block) {
        return Registries.ITEMS.get(Registries.BLOCKS.getId(block));
    }

    @Override
    public String getDataName() {
        return "blocks";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultBlocksArray = new JsonArray();
        for (Block block : Registries.BLOCKS) {
            resultBlocksArray.add(generateBlock(block));
        }
        return resultBlocksArray;
    }
}
