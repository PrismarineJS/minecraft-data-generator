package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.EmptyBlockView;

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
        Registry<Block> registry = Registry.BLOCK;
        JsonObject blocks = new JsonObject();
        JsonObject shapes = new JsonObject();
        JsonObject offsets = new JsonObject();
        Map<String, Integer> indices = new LinkedHashMap<>();

        for (Block block : registry) {
            JsonArray states = new JsonArray();
            JsonArray stateOffsets = new JsonArray();
            boolean hasOffsets = false;
            for (BlockState state : block.getStateManager().getStates()) {
                VoxelShape outline = state.getOutlineShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                JsonArray boxes = boxesOf(outline);
                BlockPos probe = new BlockPos(17, 100, -13);
                JsonArray otherBoxes = boxesOf(state.getOutlineShape(EmptyBlockView.INSTANCE, probe));
                JsonObject offsetDescriptor = null;
                if (!boxes.equals(otherBoxes)) {
                    var offset = state.getOffsetPos(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                    boxes = boxesOf(outline.offset(-offset.x, -offset.y, -offset.z));
                    for (BlockPos sample : new BlockPos[]{probe, new BlockPos(-37, -10, 23), new BlockPos(12003, 7, -8193)}) {
                        var shift = state.getOffsetPos(EmptyBlockView.INSTANCE, sample);
                        var sampleShape = state.getOutlineShape(EmptyBlockView.INSTANCE, sample);
                        if (!boxes.equals(boxesOf(sampleShape.offset(-shift.x, -shift.y, -shift.z)))) {
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
            blocks.add(registry.getId(block).getPath(), singleShape ? states.get(0) : states);
            if (hasOffsets) offsets.add(registry.getId(block).getPath(), stateOffsets);
        }
        JsonObject result = new JsonObject();
        result.add("blocks", blocks);
        result.add("shapes", shapes);
        result.add("offsets", offsets);
        return result;
    }

    private static JsonArray boxesOf(VoxelShape shape) {
        JsonArray boxes = new JsonArray();
        shape.forEachBox((x1, y1, z1, x2, y2, z2) -> {
            JsonArray box = new JsonArray();
            for (double coordinate : new double[]{x1, y1, z1, x2, y2, z2}) box.add(coordinate);
            boxes.add(box);
        });
        return boxes;
    }
}
