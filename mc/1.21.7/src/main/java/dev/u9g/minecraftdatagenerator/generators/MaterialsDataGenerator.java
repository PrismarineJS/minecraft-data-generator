package dev.u9g.minecraftdatagenerator.generators;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

//TODO entire idea of linking materials to tool speeds is obsolete and just wrong now,
//TODO but we kinda have to support it to let old code work for computing digging times,
//TODO so for now we will handle materials as "virtual" ones based on which tools can break blocks
public class MaterialsDataGenerator implements IDataGenerator {

    private static final List<ImmutableList<String>> COMPOSITE_MATERIALS = ImmutableList.<ImmutableList<String>>builder()
            .add(ImmutableList.of("plant", makeMaterialNameForTag(BlockTags.MINEABLE_WITH_AXE)))
            .add(ImmutableList.of("gourd", makeMaterialNameForTag(BlockTags.MINEABLE_WITH_AXE)))
            .add(ImmutableList.of("plant", makeMaterialNameForTag(BlockTags.MINEABLE_WITH_HOE)))
            .add(ImmutableList.of(makeMaterialNameForTag(BlockTags.SWORD_INSTANTLY_MINES), makeMaterialNameForTag(BlockTags.MINEABLE_WITH_AXE)))
            .add(ImmutableList.of(makeMaterialNameForTag(BlockTags.LEAVES), makeMaterialNameForTag(BlockTags.MINEABLE_WITH_HOE)))
            .add(ImmutableList.of(makeMaterialNameForTag(BlockTags.LEAVES), makeMaterialNameForTag(BlockTags.MINEABLE_WITH_AXE), makeMaterialNameForTag(BlockTags.MINEABLE_WITH_HOE)))
            .add(ImmutableList.of("vine_or_glow_lichen", "plant", makeMaterialNameForTag(BlockTags.MINEABLE_WITH_AXE)
            )).build();

    private static String makeMaterialNameForTag(TagKey<Block> tag) {
        return tag.location().getPath();
    }

    private static void createCompositeMaterialInfo(List<MaterialInfo> allMaterials, List<String> combinedMaterials) {
        String compositeMaterialName = String.join(";", combinedMaterials);

        List<MaterialInfo> mappedMaterials = combinedMaterials.stream()
                .map(otherName -> allMaterials.stream()
                        .filter(other -> other.getMaterialName().equals(otherName))
                        .findFirst().orElseThrow(() -> new RuntimeException("Material not found with name " + otherName)))
                .collect(Collectors.toList());

        Predicate<BlockState> compositePredicate = blockState ->
                mappedMaterials.stream().allMatch(it -> it.getPredicate().test(blockState));

        MaterialInfo materialInfo = new MaterialInfo(compositeMaterialName, compositePredicate).includes(mappedMaterials);
        allMaterials.addFirst(materialInfo);
    }

    public static List<MaterialInfo> getGlobalMaterialInfo() {
        ArrayList<MaterialInfo> resultList = new ArrayList<>();

        resultList.add(new MaterialInfo("vine_or_glow_lichen", blockState -> blockState.is(Blocks.VINE) || blockState.is(Blocks.GLOW_LICHEN)));
        resultList.add(new MaterialInfo("coweb", blockState -> blockState.is(Blocks.COBWEB)));

        resultList.add(new MaterialInfo("leaves", blockState -> blockState.is(BlockTags.LEAVES)));
        resultList.add(new MaterialInfo("wool", blockState -> blockState.is(BlockTags.WOOL)));

        // Block Materials were removed in 1.20 in favor of block tags
        resultList.add(new MaterialInfo("gourd", blockState -> blockState.is(Blocks.MELON) || blockState.is(Blocks.PUMPKIN) || blockState.is(Blocks.JACK_O_LANTERN)));
        // 'sword_efficient' tag is for all plants, and includes everything from the old PLANT and REPLACEABLE_PLANT materials (see https://minecraft.fandom.com/wiki/Tag#Blocks)
        resultList.add(new MaterialInfo("plant", blockState -> blockState.is(BlockTags.SWORD_EFFICIENT)));

        HashSet<String> uniqueMaterialNames = new HashSet<>();

        Registry<Item> itemRegistry = DGU.getWorld().registryAccess().lookupOrThrow(Registries.ITEM);
        itemRegistry.forEach(item -> {
            if (item.components().get(DataComponents.TOOL) != null) {
                item.components().get(DataComponents.TOOL).rules()
                        .stream().map(rule -> rule.blocks())
                        .forEach(blocks -> {
                            Optional<TagKey<Block>> tagKey = blocks.unwrapKey();
                            if (tagKey.isPresent()) {
                                String materialName = makeMaterialNameForTag((tagKey.get()));

                                if (!uniqueMaterialNames.contains(materialName)) {
                                    uniqueMaterialNames.add(materialName);
                                    resultList.add(new MaterialInfo(materialName, blockState -> blockState.is(blocks)));
                                }
                            }
                        });
            }
        });

        COMPOSITE_MATERIALS.forEach(values -> createCompositeMaterialInfo(resultList, values));
        return resultList;
    }

    @Override
    public String getDataName() {
        return "materials";
    }

    private static Map<Block, String> blockMaterials;
    private static Map<String, Map<Item, Float>> materialSpeeds;

    // The speed of every item on a block, as the game gives it, leaving out speeds of 1.
    private static Map<Item, Float> measureToolSpeeds(Registry<Item> itemRegistry, BlockState state) {
        Map<Item, Float> speeds = new LinkedHashMap<>();
        for (Item item : itemRegistry) {
            float speed = item.getDefaultInstance().getDestroySpeed(state);
            if (speed != 1.0f) {
                speeds.put(item, speed);
            }
        }
        return speeds;
    }

    // The material blocks.json names for a block.
    public static String getBlockMaterial(Block block) {
        assignMaterials();
        return blockMaterials.get(block);
    }

    // Tool speeds are read from the game on every block. A block keeps the material its tags give it while
    // all blocks of that material have the same speeds. Where they differ, the blocks that match more than
    // the material covers (a block in two tool tags with no composite for them) get a material named after
    // everything they match.
    private static void assignMaterials() {
        if (blockMaterials != null) {
            return;
        }
        Registry<Item> itemRegistry = DGU.getWorld().registryAccess().lookupOrThrow(Registries.ITEM);
        Registry<Block> blockRegistry = DGU.getWorld().registryAccess().lookupOrThrow(Registries.BLOCK);
        List<MaterialInfo> materials = getGlobalMaterialInfo();

        Map<String, List<Block>> blocksByMaterial = new LinkedHashMap<>();
        Map<Block, Map<Item, Float>> speedsByBlock = new HashMap<>();
        Map<Block, String> fullNames = new HashMap<>();
        for (Block block : blockRegistry) {
            BlockState state = block.defaultBlockState();
            blocksByMaterial.computeIfAbsent(BlocksDataGenerator.findMatchingBlockMaterial(state, materials), k -> new ArrayList<>()).add(block);
            speedsByBlock.put(block, measureToolSpeeds(itemRegistry, state));
            List<String> matching = materials.stream()
                    .filter(m -> !m.getMaterialName().contains(";") && !m.getMaterialName().startsWith("incorrect_for_"))
                    .filter(m -> m.getPredicate().test(state))
                    .map(MaterialInfo::getMaterialName)
                    .collect(Collectors.toList());
            fullNames.put(block, matching.isEmpty() ? "default" : String.join(";", matching));
        }

        Map<Block, String> assigned = new LinkedHashMap<>();
        Map<String, Map<Item, Float>> speeds = new LinkedHashMap<>();
        speeds.put("default", new LinkedHashMap<>());
        for (var entry : blocksByMaterial.entrySet()) {
            String material = entry.getKey();
            Set<String> covered = new HashSet<>(Arrays.asList(material.split(";")));
            Map<Map<Item, Float>, List<Block>> groups = new LinkedHashMap<>();
            for (Block block : entry.getValue()) {
                groups.computeIfAbsent(speedsByBlock.get(block), k -> new ArrayList<>()).add(block);
            }
            for (var group : groups.entrySet()) {
                String name = material;
                Block first = group.getValue().get(0);
                boolean keepsName = group.getValue().stream()
                        .anyMatch(block -> covered.equals(new HashSet<>(Arrays.asList(fullNames.get(block).split(";")))));
                if (groups.size() > 1 && !keepsName) {
                    name = fullNames.get(first);
                }
                Map<Item, Float> known = speeds.putIfAbsent(name, group.getKey());
                if (known != null && !known.equals(group.getKey())) {
                    throw new IllegalStateException("Material " + name + " has other tool speeds on " + first + " than on other blocks of it");
                }
                for (Block block : group.getValue()) {
                    assigned.put(block, name);
                }
            }
        }
        materialSpeeds = speeds;
        blockMaterials = assigned;
    }

    @Override
    public JsonElement generateDataJson() {
        assignMaterials();
        Registry<Item> itemRegistry = DGU.getWorld().registryAccess().lookupOrThrow(Registries.ITEM);

        JsonObject resultObject = new JsonObject();

        for (var entry : materialSpeeds.entrySet()) {
            JsonObject toolSpeedsObject = new JsonObject();

            for (var toolEntry : entry.getValue().entrySet()) {
                int rawItemId = itemRegistry.getId(toolEntry.getKey());
                toolSpeedsObject.addProperty(Integer.toString(rawItemId), toolEntry.getValue());
            }
            resultObject.add(entry.getKey(), toolSpeedsObject);
        }

        return resultObject;
    }

    public static class MaterialInfo {
        private final String materialName;
        private final Predicate<BlockState> predicate;
        private final List<MaterialInfo> includedMaterials = new ArrayList<>();

        public MaterialInfo(String materialName, Predicate<BlockState> predicate) {
            this.materialName = materialName;
            this.predicate = predicate;
        }

        protected MaterialInfo includes(List<MaterialInfo> otherMaterials) {
            this.includedMaterials.addAll(otherMaterials);
            return this;
        }

        public String getMaterialName() {
            return materialName;
        }

        public Predicate<BlockState> getPredicate() {
            return predicate;
        }

        public boolean includesMaterial(MaterialInfo materialInfo) {
            return includedMaterials.contains(materialInfo);
        }

        @Override
        public String toString() {
            return materialName;
        }
    }
}
