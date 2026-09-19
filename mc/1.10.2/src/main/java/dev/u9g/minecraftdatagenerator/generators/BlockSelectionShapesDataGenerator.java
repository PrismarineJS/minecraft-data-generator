package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import dev.u9g.minecraftdatagenerator.util.Registries;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import java.util.LinkedHashMap;
import java.util.Map;

/** Legacy state-order extraction; metadata is explicit because it is not a state index. */
public class BlockSelectionShapesDataGenerator implements IDataGenerator {
    public String getDataName() { return "blockSelectionShapes"; }

    private static <T extends Comparable<T>> String propertyValue(BlockState state, net.minecraft.state.property.Property<T> property) {
        return property.name(state.get(property));
    }

    public JsonObject generateDataJson() {
        JsonObject blocks = new JsonObject();
        JsonObject shapes = new JsonObject();
        JsonObject metadata = new JsonObject();
        JsonObject properties = new JsonObject();
        JsonObject blockIds = new JsonObject();
        Map<String, Integer> indices = new LinkedHashMap<>();
        var world = DGU.getWorld();
        BlockPos position = new BlockPos(0, 100, 0);
        BlockState previous = world.getBlockState(position);
        try {
            for (Block block : Registries.BLOCKS) {
                JsonArray states = new JsonArray();
                JsonArray stateMetadata = new JsonArray();
                JsonArray stateProperties = new JsonArray();
                for (BlockState state : block.getStateManager().getBlockStates().reverse()) {
                    world.setBlockState(position, state, 16);
                    JsonArray boxes = new JsonArray();
                    if (block.canCollide(state, false)) {
                        
                        // In these legacy mappings getCollisionBox is the outline used by method_414.
                        Box box = block.getCollisionBox(state, (net.minecraft.world.BlockView) world, position);
                        if (box != null) {
                            JsonArray values = new JsonArray();
                            for (double value : new double[]{box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ}) values.add(value);
                            boxes.add(values);
                        }
                    }
                    if (block instanceof net.minecraft.block.StairsBlock) {
                        boxes = new JsonArray();
                        for (Box box : dev.u9g.minecraftdatagenerator.mixin.StairsBlockInvoker.rayShapes(state)) {
                            JsonArray values = new JsonArray();
                            for (double value : new double[]{box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ}) values.add(value);
                            boxes.add(values);
                        }
                    }
                    JsonObject values = new JsonObject();
                    for (var property : state.getProperties()) values.addProperty(property.getName(), propertyValue(state, property));
                    stateProperties.add(values);
                    String key = boxes.toString();
                    Integer index = indices.get(key);
                    if (index == null) {
                        index = indices.size();
                        indices.put(key, index);
                        shapes.add(Integer.toString(index), boxes);
                    }
                    states.add(index);
                    stateMetadata.add(block.getData(state));
                }
                String name = Registries.BLOCKS.getIdentifier(block).getPath();
                blockIds.addProperty(name, Registries.BLOCKS.getRawId(block));
                blocks.add(name, states);
                metadata.add(name, stateMetadata);
                properties.add(name, stateProperties);
            }
        } finally {
            world.setBlockState(position, previous, 16);
        }
        JsonObject result = new JsonObject();
        result.add("blocks", blocks);
        result.add("shapes", shapes);
        result.add("stateMetadata", metadata);
        result.add("stateProperties", properties);
        result.add("blockIds", blockIds);
        return result;
    }
}
