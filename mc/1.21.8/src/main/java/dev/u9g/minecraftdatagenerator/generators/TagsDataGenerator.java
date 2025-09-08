package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.List;
import java.util.stream.Collectors;

public class TagsDataGenerator implements IDataGenerator {
    
    @Override
    public String getDataName() {
        return "tags";
    }

    @Override
    public JsonElement generateDataJson() {
        JsonObject root = new JsonObject();
        
        // Generate block tags
        JsonObject blockTags = new JsonObject();
        generateTagsForRegistry(DGU.getWorld().registryAccess().lookupOrThrow(Registries.BLOCK), blockTags);
        root.add("block", blockTags);
        
        // Generate item tags
        JsonObject itemTags = new JsonObject();
        generateTagsForRegistry(DGU.getWorld().registryAccess().lookupOrThrow(Registries.ITEM), itemTags);
        root.add("item", itemTags);
        
        // Generate fluid tags
        JsonObject fluidTags = new JsonObject();
        generateTagsForRegistry(DGU.getWorld().registryAccess().lookupOrThrow(Registries.FLUID), fluidTags);
        root.add("fluid", fluidTags);
        
        // Generate entity type tags
        JsonObject entityTypeTags = new JsonObject();
        generateTagsForRegistry(DGU.getWorld().registryAccess().lookupOrThrow(Registries.ENTITY_TYPE), entityTypeTags);
        root.add("entity_type", entityTypeTags);
        
        return root;
    }
    
    private <T> void generateTagsForRegistry(Registry<T> registry, JsonObject output) {
        registry.getTags().forEach(namedSet -> {
            TagKey<T> tagKey = namedSet.key();
            String tagName = tagKey.location().toString();
            
            JsonArray tagContents = new JsonArray();
            
            // Get all holders in this tag
            List<Holder<T>> holders = namedSet.stream().collect(Collectors.toList());
            
            for (Holder<T> holder : holders) {
                // Get the resource location of the entry
                ResourceLocation location = registry.getKey(holder.value());
                if (location != null) {
                    tagContents.add(location.toString());
                }
            }
            
            output.add(tagName, tagContents);
        });
    }
}