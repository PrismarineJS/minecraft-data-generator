package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.registry.RegistryKeys;

import java.util.Objects;

public class AttributesDataGenerator implements IDataGenerator {
    @Override
    public String getDataName() {
        return "attributes";
    }

    @Override
    public JsonElement generateDataJson() {
        JsonArray arr = new JsonArray();
        var registry = DGU.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ATTRIBUTE);
        for (EntityAttribute attribute : registry) {
            JsonObject obj = new JsonObject();
            String name = Objects.requireNonNull(registry.getId(attribute)).getPath();
            while(name.contains("_")) {
                name = name.replaceFirst("_[a-z]", String.valueOf(Character.toUpperCase(name.charAt(name.indexOf("_") + 1))));
            }
            obj.addProperty("name", name);
            obj.addProperty("resource", Objects.requireNonNull(registry.getId(attribute)).toString());
            obj.addProperty("min", ((ClampedEntityAttribute) attribute).getMinValue());
            obj.addProperty("max", ((ClampedEntityAttribute) attribute).getMaxValue());
            obj.addProperty("default", attribute.getDefaultValue());
            arr.add(obj);
        }
        return arr;
    }
}
