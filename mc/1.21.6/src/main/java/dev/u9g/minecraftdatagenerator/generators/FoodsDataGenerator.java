package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.u9g.minecraftdatagenerator.util.DGU;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

import java.util.Objects;

public class FoodsDataGenerator implements IDataGenerator {
    public static JsonObject generateFoodDescriptor(Registry<Item> registry, Item foodItem) {
        JsonObject foodDesc = new JsonObject();
        Identifier registryKey = registry.getKey(foodItem).orElseThrow().getValue();

        foodDesc.addProperty("id", registry.getRawId(foodItem));
        foodDesc.addProperty("name", registryKey.getPath());

        foodDesc.addProperty("stackSize", foodItem.getMaxCount());
        foodDesc.addProperty("displayName", DGU.translateText(foodItem.getTranslationKey()));

        FoodComponent foodComponent = Objects.requireNonNull(foodItem.getComponents().get(DataComponentTypes.FOOD));
        float foodPoints = foodComponent.nutrition();
        float saturationRatio = foodComponent.saturation() * 2.0F;
        float saturation = foodPoints * saturationRatio;

        foodDesc.addProperty("foodPoints", foodPoints);
        foodDesc.addProperty("saturation", saturation);

        foodDesc.addProperty("effectiveQuality", foodPoints + saturation);
        foodDesc.addProperty("saturationRatio", saturationRatio);
        return foodDesc;
    }

    @Override
    public String getDataName() {
        return "foods";
    }

    public JsonArray generateDataJson() {
        JsonArray resultsArray = new JsonArray();
        Registry<Item> itemRegistry = DGU.getWorld().getRegistryManager().getOrThrow(RegistryKeys.ITEM);
        itemRegistry.stream()
                .filter(i -> i.getComponents().contains(DataComponentTypes.FOOD))
                .forEach(food -> resultsArray.add(generateFoodDescriptor(itemRegistry, food)));
        return resultsArray;
    }
}
