package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.u9g.minecraftdatagenerator.util.DGU;
import dev.u9g.minecraftdatagenerator.util.Registries;
import net.minecraft.enchantment.EnchantmentTarget;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import java.util.*;
import java.util.stream.Collectors;

public class ItemsDataGenerator implements IDataGenerator {

    private static List<Item> calculateItemsToRepairWith(Item sourceItem) {
        List<Item> items = new ArrayList<>();
        for (Item otherItem : Registries.ITEMS) {
            if (sourceItem.canRepair(DGU.stackFor(sourceItem), DGU.stackFor(otherItem))) {
                items.add(otherItem);
            }
        }
        return items;
    }

    private static Set<String> getApplicableEnchantmentTargets(Item sourceItem) {
        return Arrays.stream(EnchantmentTarget.values())
                .filter(target -> target.isCompatible(sourceItem))
                .map(EnchantmentsDataGenerator::getEnchantmentTargetName)
                .sorted()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static JsonObject generateItem(Item item) {
        JsonObject itemDesc = new JsonObject();
        Identifier registryKey = Registries.ITEMS.getIdentifier(item);

        itemDesc.addProperty("id", Registries.ITEMS.getRawId(item));
        itemDesc.addProperty("name", Objects.requireNonNull(registryKey).getPath());

        itemDesc.addProperty("displayName", item.getDisplayName(DGU.stackFor(item)));
        itemDesc.addProperty("stackSize", item.getMaxCount());

        JsonArray enchantCategoriesArray = new JsonArray();
        getApplicableEnchantmentTargets(item).forEach(enchantCategoriesArray::add);
        if (!enchantCategoriesArray.isEmpty()) {
            itemDesc.add("enchantCategories", enchantCategoriesArray);
        }

        if (item.isDamageable()) {
            List<Item> repairWithItems = calculateItemsToRepairWith(item);

            JsonArray fixedWithArray = new JsonArray();
            for (Item repairWithItem : repairWithItems) {
                Identifier repairWithName = Registries.ITEMS.getIdentifier(repairWithItem);
                fixedWithArray.add(new JsonPrimitive(Objects.requireNonNull(repairWithName).getPath()));
            }
            if (!fixedWithArray.isEmpty()) {
                itemDesc.add("repairWith", fixedWithArray);
            }

            int maxDurability = item.getMaxDamage();
            itemDesc.addProperty("maxDurability", maxDurability);
        }
        return itemDesc;
    }

    @Override
    public String getDataName() {
        return "items";
    }

    @Override
    public JsonArray generateDataJson() {
        JsonArray resultArray = new JsonArray();
        for (Item item : Registries.ITEMS) {
            resultArray.add(generateItem(item));
        }
        return resultArray;
    }
}
