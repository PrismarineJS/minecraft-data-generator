package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.List;

public class EnchantmentsDataGenerator implements IDataGenerator {
    public static String getEnchantmentTargetName(RegistryEntryList<Item> target) {
        TagKey<Item> tagKey = target.getTagKey().orElseThrow();
        return tagKey.id().getPath().split("/")[1];
    }

    private static boolean isEnchantmentInTag(Enchantment enchantment, String tag) {
        return DGU.getWorld()
                .getRegistryManager()
                .getOrThrow(RegistryKeys.ENCHANTMENT)
                .getOrThrow(TagKey.of(RegistryKeys.ENCHANTMENT, Identifier.of(tag)))
                .stream()
                .anyMatch(enchantmentRegistryEntry -> enchantmentRegistryEntry.value() == enchantment);
    }

    //Equation enchantment costs follow is a * level + b, so we can easily retrieve a and b by passing zero level
    private static JsonObject generateEnchantmentMinPowerCoefficients(Enchantment enchantment) {
        int b = enchantment.getMinPower(0);
        int a = enchantment.getMinPower(1) - b;

        JsonObject resultObject = new JsonObject();
        resultObject.addProperty("a", a);
        resultObject.addProperty("b", b);
        return resultObject;
    }

    private static JsonObject generateEnchantmentMaxPowerCoefficients(Enchantment enchantment) {
        int b = enchantment.getMaxPower(0);
        int a = enchantment.getMaxPower(1) - b;

        JsonObject resultObject = new JsonObject();
        resultObject.addProperty("a", a);
        resultObject.addProperty("b", b);
        return resultObject;
    }

    public static JsonObject generateEnchantment(Registry<Enchantment> registry, Enchantment enchantment) {
        JsonObject enchantmentDesc = new JsonObject();
        Identifier registryKey = registry.getKey(enchantment).orElseThrow().getValue();

        enchantmentDesc.addProperty("id", registry.getRawId(enchantment));
        enchantmentDesc.addProperty("name", registryKey.getPath());
        enchantmentDesc.addProperty("displayName", enchantment.description().getString());

        enchantmentDesc.addProperty("maxLevel", enchantment.getMaxLevel());
        enchantmentDesc.add("minCost", generateEnchantmentMinPowerCoefficients(enchantment));
        enchantmentDesc.add("maxCost", generateEnchantmentMaxPowerCoefficients(enchantment));

        enchantmentDesc.addProperty("treasureOnly", isEnchantmentInTag(enchantment, "treasure"));

        enchantmentDesc.addProperty("curse", isEnchantmentInTag(enchantment, "curse"));

        List<Enchantment> incompatibleEnchantments = registry.stream()
                .filter(other -> {
                    RegistryEntry<Enchantment> enchantmentEntry = registry.getEntry(enchantment);
                    RegistryEntry<Enchantment> otherEntry = registry.getEntry(other);
                    return !Enchantment.canBeCombined(enchantmentEntry, otherEntry);
                })
                .filter(other -> other != enchantment)
                .toList();

        JsonArray excludes = new JsonArray();
        for (Enchantment excludedEnchantment : incompatibleEnchantments) {
            Identifier otherKey = registry.getKey(excludedEnchantment).orElseThrow().getValue();
            excludes.add(otherKey.getPath());
        }
        enchantmentDesc.add("exclude", excludes);
        enchantmentDesc.addProperty("category", getEnchantmentTargetName(enchantment.getApplicableItems()));
        enchantmentDesc.addProperty("weight", enchantment.getWeight());
        enchantmentDesc.addProperty("tradeable", isEnchantmentInTag(enchantment, "tradeable"));
        enchantmentDesc.addProperty("discoverable", isEnchantmentInTag(enchantment, "on_random_loot"));

        return enchantmentDesc;
    }

    @Override
    public String getDataName() {
        return "enchantments";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultsArray = new JsonArray();
        Registry<Enchantment> enchantmentRegistry = DGU.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        enchantmentRegistry.stream()
                .forEach(enchantment -> resultsArray.add(generateEnchantment(enchantmentRegistry, enchantment)));
        return resultsArray;
    }
}
