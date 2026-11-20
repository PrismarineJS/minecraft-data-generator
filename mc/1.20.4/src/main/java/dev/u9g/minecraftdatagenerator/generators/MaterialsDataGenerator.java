package dev.u9g.minecraftdatagenerator.generators;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.mixin.MiningToolItemAccessor;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.block.BambooBlock;
import net.minecraft.block.BambooShootBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.MiningToolItem;
import net.minecraft.item.SwordItem;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

//TODO entire idea of linking materials to tool speeds is obsolete and just wrong now,
//TODO but we kinda have to support it to let old code work for computing digging times,
//TODO so for now we will handle materials as "virtual" ones based on which tools can break blocks
public class MaterialsDataGenerator implements IDataGenerator {

    private static final List<ImmutableList<String>> COMPOSITE_MATERIALS = ImmutableList.<ImmutableList<String>>builder()
            .add(ImmutableList.of("plant", makeMaterialNameForTag(BlockTags.AXE_MINEABLE)))
            .add(ImmutableList.of("gourd", makeMaterialNameForTag(BlockTags.AXE_MINEABLE)))
            .add(ImmutableList.of("sword_instantly_mines", makeMaterialNameForTag(BlockTags.AXE_MINEABLE)))
            .add(ImmutableList.of(makeMaterialNameForTag(BlockTags.LEAVES), makeMaterialNameForTag(BlockTags.HOE_MINEABLE)))
            .add(ImmutableList.of(makeMaterialNameForTag(BlockTags.LEAVES), makeMaterialNameForTag(BlockTags.AXE_MINEABLE), makeMaterialNameForTag(BlockTags.HOE_MINEABLE)))
            .add(ImmutableList.of("vine_or_glow_lichen", "plant", makeMaterialNameForTag(BlockTags.AXE_MINEABLE)
            )).build();

    private static String makeMaterialNameForTag(TagKey<Block> tag) {
        return tag.id().getPath();
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

        resultList.add(new MaterialInfo("vine_or_glow_lichen", blockState -> blockState.isOf(Blocks.VINE) || blockState.isOf(Blocks.GLOW_LICHEN)));
        resultList.add(new MaterialInfo("coweb", blockState -> blockState.isOf(Blocks.COBWEB)));

        resultList.add(new MaterialInfo("leaves", blockState -> blockState.isIn(BlockTags.LEAVES)));
        resultList.add(new MaterialInfo("wool", blockState -> blockState.isIn(BlockTags.WOOL)));

        // Block Materials were removed in 1.20 in favor of block tags
        resultList.add(new MaterialInfo("gourd", blockState -> blockState.isOf(Blocks.MELON) || blockState.isOf(Blocks.PUMPKIN) || blockState.isOf(Blocks.JACK_O_LANTERN)));
        // 'sword_efficient' tag is for all plants, and includes everything from the old PLANT and REPLACEABLE_PLANT materials (see https://minecraft.fandom.com/wiki/Tag#Blocks)
        resultList.add(new MaterialInfo("plant", blockState -> blockState.isIn(BlockTags.SWORD_EFFICIENT)));
        // Bamboo, which a sword breaks at once; named after the block tag that does this from 1.21.5
        resultList.add(new MaterialInfo("sword_instantly_mines", blockState -> blockState.isOf(Blocks.BAMBOO) || blockState.isOf(Blocks.BAMBOO_SAPLING)));

        HashSet<String> uniqueMaterialNames = new HashSet<>();

        Registry<Item> itemRegistry = DGU.getWorld().getRegistryManager().get(RegistryKeys.ITEM);
        itemRegistry.forEach(item -> {
            if (item instanceof MiningToolItem toolItem) {
                TagKey<Block> effectiveBlocks = ((MiningToolItemAccessor) toolItem).getEffectiveBlocks();
                String materialName = makeMaterialNameForTag(effectiveBlocks);

                if (!uniqueMaterialNames.contains(materialName)) {
                    uniqueMaterialNames.add(materialName);
                    resultList.add(new MaterialInfo(materialName, blockState -> blockState.isIn(effectiveBlocks)));
                }
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
            float speed = item.getDefaultStack().getMiningSpeedMultiplier(state);
            // Up to 1.21.4 BambooBlock and BambooShootBlock break at once for a sword
            // (calcBlockBreakingDelta returns 1), whatever the sword's speed on them is.
            if (item instanceof SwordItem && (state.getBlock() instanceof BambooBlock || state.getBlock() instanceof BambooShootBlock)) {
                speed = Float.MAX_VALUE;
            }
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
        Registry<Item> itemRegistry = DGU.getWorld().getRegistryManager().get(RegistryKeys.ITEM);
        Registry<Block> blockRegistry = DGU.getWorld().getRegistryManager().get(RegistryKeys.BLOCK);
        List<MaterialInfo> materials = getGlobalMaterialInfo();

        Map<String, List<Block>> blocksByMaterial = new LinkedHashMap<>();
        Map<Block, Map<Item, Float>> speedsByBlock = new HashMap<>();
        Map<Block, String> fullNames = new HashMap<>();
        for (Block block : blockRegistry) {
            BlockState state = block.getDefaultState();
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
        Registry<Item> itemRegistry = DGU.getWorld().getRegistryManager().get(RegistryKeys.ITEM);

        JsonObject resultObject = new JsonObject();

        for (var entry : materialSpeeds.entrySet()) {
            JsonObject toolSpeedsObject = new JsonObject();

            for (var toolEntry : entry.getValue().entrySet()) {
                int rawItemId = itemRegistry.getRawId(toolEntry.getKey());
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
