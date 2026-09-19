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
                        // Bounds queries must see the requested state even if onAdded removed it
                        // because the empty extraction world lacks its supporting block.
                        net.minecraft.world.BlockView view = new net.minecraft.world.BlockView() {
                            public BlockState getBlockState(BlockPos pos) { return pos.equals(position) ? state : world.getBlockState(pos); }
                            public boolean isAir(BlockPos pos) { return !pos.equals(position) && world.isAir(pos); }
                            public int getLight(BlockPos pos, int amount) { return world.getLight(pos, amount); }
                            public net.minecraft.world.biome.Biome getBiome(BlockPos pos) { return world.getBiome(pos); }
                            public boolean isEmpty() { return false; }
                            public net.minecraft.world.level.LevelGeneratorType getGeneratorType() { return world.getGeneratorType(); }
                            public net.minecraft.block.entity.BlockEntity getBlockEntity(BlockPos pos) { return world.getBlockEntity(pos); }
                            public int getStrongRedstonePower(BlockPos pos, net.minecraft.util.math.Direction direction) { return world.getStrongRedstonePower(pos, direction); }
                        };
                        block.setBoundingBox(view, position);
                        Box box = block instanceof net.minecraft.block.TorchBlock
                                ? torchBounds(block, state, world, position)
                                : block.getSelectionBox(world, position);
                        if (box != null) {
                            JsonArray values = new JsonArray();
                            for (double value : new double[]{box.minX, box.minY - position.getY(), box.minZ, box.maxX, box.maxY - position.getY(), box.maxZ}) values.add(value);
                            boxes.add(values);
                        }
                    }
                    if (block instanceof net.minecraft.block.StairsBlock) {
                        boxes = new JsonArray();
                        dev.u9g.minecraftdatagenerator.util.RayShapeCapture.begin();
                        java.util.List<double[]> nativeShapes;
                        try {
                            var start = new net.minecraft.util.math.Vec3d(0.5, 100.5, 2);
                            block.rayTrace(world, position, start, start.add(0, 0, -3));
                        } finally {
                            nativeShapes = dev.u9g.minecraftdatagenerator.util.RayShapeCapture.end();
                        }
                        if (nativeShapes.isEmpty()) throw new IllegalStateException("No stair ray shapes captured");
                        for (double[] shape : nativeShapes) {
                            JsonArray values = new JsonArray();
                            for (double value : shape) values.add(value);
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

    // TorchBlock updates its mutable bounds in rayTrace, not setBoundingBox(BlockView, pos).
    private static Box torchBounds(Block block, BlockState state, net.minecraft.world.World world, BlockPos pos) {
        BlockPos[] supports = {pos.add(-1, 0, 0), pos.add(1, 0, 0), pos.add(0, -1, 0), pos.add(0, 0, -1), pos.add(0, 0, 1)};
        BlockState[] previous = new BlockState[supports.length];
        BlockState previousTarget = world.getBlockState(pos);
        for (int i = 0; i < supports.length; i++) previous[i] = world.getBlockState(supports[i]);
        try {
            for (BlockPos support : supports) world.setBlockState(support, Registries.BLOCKS.get(new net.minecraft.util.Identifier("stone")).getDefaultState(), 16);
            world.setBlockState(pos, state, 16);
            if (!world.getBlockState(pos).equals(state)) throw new IllegalStateException("Torch state not installed: " + state);
            var start = new net.minecraft.util.math.Vec3d(pos.getX() + 0.5, pos.getY() + 2, pos.getZ() + 0.5);
            block.rayTrace(world, pos, start, start.add(0, -3, 0));
            return block.getSelectionBox(world, pos);
        } finally {
            world.setBlockState(pos, previousTarget, 16);
            for (int i = 0; i < supports.length; i++) world.setBlockState(supports[i], previous[i], 16);
        }
    }
}
