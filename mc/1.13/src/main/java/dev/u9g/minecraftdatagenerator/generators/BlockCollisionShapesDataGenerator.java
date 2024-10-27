package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.u9g.minecraftdatagenerator.util.EmptyBlockView;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.shapes.VoxelShape;

import java.util.*;

public class BlockCollisionShapesDataGenerator implements IDataGenerator {

    @Override
    public String getDataName() {
        return "blockCollisionShapes";
    }

    @Override
    public JsonObject generateDataJson() {
        Registry<Block> blockRegistry = Registry.BLOCK;
        BlockShapesCache blockShapesCache = new BlockShapesCache();
        for (Block block : (Iterable<Block>) blockRegistry) {
            blockShapesCache.processBlock(block);
        }

        JsonObject resultObject = new JsonObject();

        resultObject.add("blocks", blockShapesCache.dumpBlockShapeIndices(blockRegistry));
        resultObject.add("shapes", blockShapesCache.dumpShapesObject());

        return resultObject;
    }

    private static class BlockShapesCache {
        public final Map<VoxelShape, Integer> uniqueBlockShapes = new LinkedHashMap<>();
        public final Map<Block, List<Integer>> blockCollisionShapes = new LinkedHashMap<>();
        private int lastCollisionShapeId = 0;

        public void processBlock(Block block) {
            List<BlockState> blockStates = block.getStateManager().getBlockStates();
            List<Integer> blockCollisionShapes = new ArrayList<>();

            for (BlockState blockState : blockStates) {
                VoxelShape blockShape = blockState.getCollisionShape(EmptyBlockView.INSTANCE, BlockPos.ORIGIN);
                Integer blockShapeIndex = uniqueBlockShapes.get(blockShape);

                if (blockShapeIndex == null) {
                    blockShapeIndex = lastCollisionShapeId++;
                    uniqueBlockShapes.put(blockShape, blockShapeIndex);
                }
                blockCollisionShapes.add(blockShapeIndex);
            }

            this.blockCollisionShapes.put(block, blockCollisionShapes);
        }

        public JsonObject dumpBlockShapeIndices(Registry<Block> blockRegistry) {
            JsonObject resultObject = new JsonObject();

            for (Map.Entry<Block, List<Integer>> entry : blockCollisionShapes.entrySet()) {
                List<Integer> blockCollisions = entry.getValue();
                long distinctShapesCount = blockCollisions.stream().distinct().count();
                JsonElement blockCollision;
                if (distinctShapesCount == 1L) {
                    blockCollision = new JsonPrimitive(blockCollisions.getFirst());
                } else {
                    blockCollision = new JsonArray();
                    for (int collisionId : blockCollisions) {
                        ((JsonArray) blockCollision).add(collisionId);
                    }
                }

                Identifier registryKey = blockRegistry.getId(entry.getKey());
                resultObject.add(Objects.requireNonNull(registryKey).getPath(), blockCollision);
            }

            return resultObject;
        }

        public JsonObject dumpShapesObject() {
            JsonObject shapesObject = new JsonObject();

            for (Map.Entry<VoxelShape, Integer> entry : uniqueBlockShapes.entrySet()) {
                JsonArray boxesArray = new JsonArray();
                entry.getKey().forEachBox((x1, y1, z1, x2, y2, z2) -> {
                    JsonArray oneBoxJsonArray = new JsonArray();

                    oneBoxJsonArray.add(x1);
                    oneBoxJsonArray.add(y1);
                    oneBoxJsonArray.add(z1);

                    oneBoxJsonArray.add(x2);
                    oneBoxJsonArray.add(y2);
                    oneBoxJsonArray.add(z2);

                    boxesArray.add(oneBoxJsonArray);
                });
                shapesObject.add(Integer.toString(entry.getValue()), boxesArray);
            }
            return shapesObject;
        }
    }
}
