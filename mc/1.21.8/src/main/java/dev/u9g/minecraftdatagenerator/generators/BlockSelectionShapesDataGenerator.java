package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.EmptyBlockGetter;

import java.util.LinkedHashMap;
import java.util.Map;

/** Outline geometry and position-dependent model offsets, separate from collision. */
public class BlockSelectionShapesDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "blockSelectionShapes";
    }

    @Override
    public JsonObject generateDataJson() {
        Registry<Block> registry = DGU.getWorld().registryAccess().lookupOrThrow(Registries.BLOCK);
        JsonObject blocks = new JsonObject();
        JsonObject shapes = new JsonObject();
        JsonObject offsets = new JsonObject();
        Map<String, Integer> indices = new LinkedHashMap<>();

        for (Block block : registry) {
            JsonArray states = new JsonArray();
            JsonArray stateOffsets = new JsonArray();
            boolean hasOffsets = false;
            for (BlockState state : block.getStateDefinition().getPossibleStates()) {
                VoxelShape outline = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
                JsonArray boxes = boxesOf(outline);
                BlockPos probe = new BlockPos(17, 100, -13);
                JsonArray otherBoxes = boxesOf(state.getShape(EmptyBlockGetter.INSTANCE, probe));
                JsonObject offsetDescriptor = null;
                if (!boxes.equals(otherBoxes)) {
                    var offset = state.getOffset(BlockPos.ZERO);
                    boxes = boxesOf(outline.move(-offset.x, -offset.y, -offset.z));
                    for (BlockPos sample : new BlockPos[]{probe, new BlockPos(-37, -10, 23), new BlockPos(12003, 7, -8193)}) {
                        var shift = state.getOffset(sample);
                        var sampleShape = state.getShape(EmptyBlockGetter.INSTANCE, sample);
                        if (!boxes.equals(boxesOf(sampleShape.move(-shift.x, -shift.y, -shift.z)))) {
                            throw new IllegalStateException("Unsupported position-dependent outline: " + state);
                        }
                    }
                    offsetDescriptor = new JsonObject();
                    offsetDescriptor.addProperty("type", "model");
                    offsetDescriptor.addProperty("maxHorizontal", Math.abs(offset.x));
                    offsetDescriptor.addProperty("verticalMultiplier", -offset.y);
                    hasOffsets = true;
                }
                stateOffsets.add(offsetDescriptor);
                String key = boxes.toString();
                Integer index = indices.get(key);
                if (index == null) {
                    index = indices.size();
                    indices.put(key, index);
                    shapes.add(Integer.toString(index), boxes);
                }
                states.add(index);
            }
            boolean singleShape = true;
            for (var state : states) singleShape &= state.equals(states.get(0));
            blocks.add(registry.getKey(block).getPath(), singleShape ? states.get(0) : states);
            if (hasOffsets) offsets.add(registry.getKey(block).getPath(), stateOffsets);
        }
        JsonObject result = new JsonObject();
        result.add("blocks", blocks);
        result.add("shapes", shapes);
        result.add("offsets", offsets);
        return result;
    }

    private static JsonArray boxesOf(VoxelShape shape) {
        JsonArray boxes = new JsonArray();
        shape.forAllBoxes((x1, y1, z1, x2, y2, z2) -> {
            JsonArray box = new JsonArray();
            for (double coordinate : new double[]{x1, y1, z1, x2, y2, z2}) box.add(coordinate);
            boxes.add(box);
        });
        return boxes;
    }
}
