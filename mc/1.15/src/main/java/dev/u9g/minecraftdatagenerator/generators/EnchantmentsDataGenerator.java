package dev.u9g.minecraftdatagenerator.generators;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class EnchantmentsDataGenerator implements IDataGenerator {

    private static final ImmutableMap<EnchantmentTarget, String> ENCHANTMENT_TARGET_NAMES = ImmutableMap.<EnchantmentTarget, String>builder()
            .put(EnchantmentTarget.ALL, "vanishable") // according to VanishingCurseEnchantment
            .build();

    public static String getEnchantmentTargetName(EnchantmentTarget target) {
        return ENCHANTMENT_TARGET_NAMES.getOrDefault(target, target.name().toLowerCase(Locale.ROOT));
    }

    // Equation enchantment costs follow is a * level + b, so we can easily retrieve a and b by passing zero level
    private static JsonObject generateEnchantmentMinPowerCoefficients(Enchantment enchantment) {
        int b = enchantment.getMinimumPower(0);
        int a = enchantment.getMinimumPower(1) - b;

        JsonObject resultObject = new JsonObject();
        resultObject.addProperty("a", a);
        resultObject.addProperty("b", b);
        return resultObject;
    }

    private static JsonObject generateEnchantmentMaxPowerCoefficients(Enchantment enchantment) {
        int b = enchantment.getMaximumPower(0);
        int a = enchantment.getMaximumPower(1) - b;

        JsonObject resultObject = new JsonObject();
        resultObject.addProperty("a", a);
        resultObject.addProperty("b", b);
        return resultObject;
    }

    public static JsonObject generateEnchantment(Registry<Enchantment> registry, Enchantment enchantment) {
        JsonObject enchantmentDesc = new JsonObject();
        Identifier registryKey = registry.getId(enchantment);

        enchantmentDesc.addProperty("id", registry.getRawId(enchantment));
        enchantmentDesc.addProperty("name", Objects.requireNonNull(registryKey).getPath());
        enchantmentDesc.addProperty("displayName", DGU.translateText(enchantment.getTranslationKey()));

        enchantmentDesc.addProperty("maxLevel", enchantment.getMaximumLevel());
        enchantmentDesc.add("minCost", generateEnchantmentMinPowerCoefficients(enchantment));
        enchantmentDesc.add("maxCost", generateEnchantmentMaxPowerCoefficients(enchantment));

        enchantmentDesc.addProperty("treasureOnly", enchantment.isTreasure());
        enchantmentDesc.addProperty("curse", enchantment.isCursed());

        List<Enchantment> incompatibleEnchantments = registry.stream()
                .filter(other -> !enchantment.isDifferent(other))
                .filter(other -> other != enchantment).toList();

        JsonArray excludes = new JsonArray();
        for (Enchantment excludedEnchantment : incompatibleEnchantments) {
            Identifier otherKey = registry.getId(excludedEnchantment);
            excludes.add(Objects.requireNonNull(otherKey).getPath());
        }
        enchantmentDesc.add("exclude", excludes);

        enchantmentDesc.addProperty("category", getEnchantmentTargetName(enchantment.type));
        JsonArray supportedItems = new JsonArray();
        var itemRegistry = Registry.ITEM;
        for (var item : itemRegistry) {
            if (enchantment.isAcceptableItem(new net.minecraft.item.ItemStack(item))) {
                supportedItems.add(itemRegistry.getId(item).getPath());
            }
        }
        enchantmentDesc.add("supportedItems", supportedItems);
        enchantmentDesc.addProperty("weight", enchantment.getWeight().getWeight());
        enchantmentDesc.addProperty("rarity", enchantment.getWeight().name().toLowerCase(Locale.ROOT));
        enchantmentDesc.addProperty("tradeable", true); // the first non-tradeable enchant came in 1.16, soul speed
        enchantmentDesc.addProperty("discoverable", true); // the first non-enchantable enchant came in 1.16, soul speed

        return enchantmentDesc;
    }

    @Override
    public String getDataName() {
        return "enchantments";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultsArray = new JsonArray();
        Registry<Enchantment> enchantmentRegistry = Registry.ENCHANTMENT;
        enchantmentRegistry.stream()
                .forEach(enchantment -> resultsArray.add(generateEnchantment(enchantmentRegistry, enchantment)));
        return resultsArray;
    }
}
