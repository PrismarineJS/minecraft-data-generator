package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ItemsDataGenerator implements IDataGenerator {

    private static List<Item> calculateItemsToRepairWith(Registry<Item> itemRegistry, Item sourceItem) {
        ItemStack sourceItemStack = sourceItem.getDefaultStack();
        return itemRegistry.stream()
                .filter(otherItem -> sourceItemStack.canRepairWith(otherItem.getDefaultStack()))
                .collect(Collectors.toList());
    }

    public static JsonObject generateItem(Registry<Item> itemRegistry, Item item) {
        JsonObject itemDesc = new JsonObject();
        Identifier registryKey = itemRegistry.getKey(item).orElseThrow().getValue();

        itemDesc.addProperty("id", itemRegistry.getRawId(item));
        itemDesc.addProperty("name", registryKey.getPath());

        itemDesc.addProperty("displayName", DGU.translateText(item.getTranslationKey()));
        itemDesc.addProperty("stackSize", item.getMaxCount());

        JsonArray enchantCategoriesArray = new JsonArray();
        DGU.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT).stream()
                .map(Enchantment::getApplicableItems)
                .filter(applicableItems -> applicableItems.contains(itemRegistry.getEntry(item)))
                .map(EnchantmentsDataGenerator::getEnchantmentTargetName)
                .distinct()
                .forEach(enchantCategoriesArray::add);

        if (enchantCategoriesArray.size() > 0) {
            itemDesc.add("enchantCategories", enchantCategoriesArray);
        }

        if (item.getComponents().contains(DataComponentTypes.DAMAGE)) {
            List<Item> repairWithItems = calculateItemsToRepairWith(itemRegistry, item);

            JsonArray fixedWithArray = new JsonArray();
            for (Item repairWithItem : repairWithItems) {
                Identifier repairWithName = itemRegistry.getKey(repairWithItem).orElseThrow().getValue();
                fixedWithArray.add(repairWithName.getPath());
            }
            if (fixedWithArray.size() > 0) {
                itemDesc.add("repairWith", fixedWithArray);
            }

            int maxDurability = Objects.requireNonNull(item.getComponents().get(DataComponentTypes.MAX_DAMAGE));
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
        Registry<Item> itemRegistry = DGU.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ITEM);
        itemRegistry.stream().forEach(item -> resultArray.add(generateItem(itemRegistry, item)));
        return resultArray;
    }
}
