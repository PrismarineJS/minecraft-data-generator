package dev.u9g.minecraftdatagenerator.generators;

import com.google.common.base.CaseFormat;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.mixin.MiningToolItemAccessor;
import dev.u9g.minecraftdatagenerator.util.DGU;
import dev.u9g.minecraftdatagenerator.util.EmptyBlockView;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.IntProperty;
import net.minecraft.state.property.Property;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class BlocksDataGenerator implements IDataGenerator {
    private static List<Item> getItemsEffectiveForBlock(Block block) {
        return Registry.ITEM.stream()
                .filter(item -> item instanceof ToolItem)
                .filter(item -> ((MiningToolItemAccessor) item).getEffectiveBlocks().contains(block))
                .collect(Collectors.toList());
    }

    private static List<ItemStack> populateDropsIfPossible(BlockState blockState, Item firstToolItem) {
        return new ArrayList<>();
    }

    private static String getPropertyTypeName(Property<?> property) {
        //Explicitly handle default minecraft properties
        if (property instanceof BooleanProperty) {
            return "bool";
        }
        if (property instanceof IntProperty) {
            return "int";
        }
        if (property instanceof EnumProperty) {
            return "enum";
        }

        //Use simple class name as fallback, this code will give something like
        //example_type for ExampleTypeProperty class name
        String rawPropertyName = property.getClass().getSimpleName().replace("Property", "");
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, rawPropertyName);
    }

    private static <T extends Comparable<T>> JsonObject generateStateProperty(Property<T> property) {
        JsonObject propertyObject = new JsonObject();
        Collection<T> propertyValues = property.getValues();

        propertyObject.addProperty("name", property.getName());
        propertyObject.addProperty("type", getPropertyTypeName(property));
        propertyObject.addProperty("num_values", propertyValues.size());

        //Do not add values for vanilla boolean properties, they are known by default
        if (!(property instanceof BooleanProperty)) {
            JsonArray propertyValuesArray = new JsonArray();
            for (T propertyValue : propertyValues) {
                propertyValuesArray.add(property.name(propertyValue));
            }
            propertyObject.add("values", propertyValuesArray);
        }
        return propertyObject;
    }

    public static JsonObject generateBlock(Block block) {
        JsonObject blockDesc = new JsonObject();

        List<BlockState> blockStates = block.getStateManager().getBlockStates();
        BlockState defaultState = block.getDefaultState();
        Identifier registryKey = Registry.BLOCK.getId(block);
        String localizationKey = block.getTranslationKey();
        List<Item> effectiveTools = getItemsEffectiveForBlock(block);

        blockDesc.addProperty("id", Registry.BLOCK.getRawId(block));
        blockDesc.addProperty("name", Objects.requireNonNull(registryKey).getPath());
        blockDesc.addProperty("displayName", DGU.translateText(localizationKey));

        float hardness = block.getDefaultState().getHardness(null, null);

        blockDesc.addProperty("hardness", hardness);
        blockDesc.addProperty("resistance", block.getBlastResistance());
        blockDesc.addProperty("stackSize", block.getItem().getMaxCount());
        blockDesc.addProperty("diggable", hardness != -1.0f && !(block instanceof AirBlock) && !(block instanceof net.minecraft.class_3710));
        JsonObject effTools = new JsonObject();
        effectiveTools.forEach(item -> effTools.addProperty(
                String.valueOf(Registry.ITEM.getRawId(item)), // key
                item.getBlockBreakingSpeed(DGU.stackFor(item), defaultState) // value
        ));
        blockDesc.add("effectiveTools", effTools);

        blockDesc.addProperty("transparent", !defaultState.isFullOpaque(EmptyBlockView.INSTANCE, BlockPos.ORIGIN));
        blockDesc.addProperty("emitLight", defaultState.getLuminance());
        blockDesc.addProperty("filterLight", block.getLightSubtracted(block.getDefaultState(), EmptyBlockView.INSTANCE, BlockPos.ORIGIN));

        blockDesc.addProperty("defaultState", Block.getRawIdFromState(defaultState));
        blockDesc.addProperty("minStateId", Block.getRawIdFromState(blockStates.getFirst()));
        blockDesc.addProperty("maxStateId", Block.getRawIdFromState(blockStates.getLast()));
        JsonArray stateProperties = new JsonArray();
        for (Property<?> property : block.getStateManager().getProperties()) {
            stateProperties.add(generateStateProperty(property));
        }
        blockDesc.add("states", stateProperties);
        blockDesc.add("drops", new JsonArray());
        VoxelShape blockCollisionShape = defaultState.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
        blockDesc.addProperty("boundingBox", blockCollisionShape.isEmpty() ? "empty" : "block");

        return blockDesc;
    }

    @Override
    public String getDataName() {
        return "blocks";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultBlocksArray = new JsonArray();
        Registry<Block> blockRegistry = Registry.BLOCK;
        for (Block block : (Iterable<Block>) blockRegistry) {
            resultBlocksArray.add(generateBlock(block));
        }
        return resultBlocksArray;
    }
}
