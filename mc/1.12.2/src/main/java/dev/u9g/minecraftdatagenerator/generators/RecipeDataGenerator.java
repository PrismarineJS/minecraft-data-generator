package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.u9g.minecraftdatagenerator.util.Registries;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.RecipeDispatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipeType;
import net.minecraft.recipe.ShapelessRecipeType;
import net.minecraft.util.collection.DefaultedList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class RecipeDataGenerator implements IDataGenerator {
    private static final int WILDCARD_DAMAGE = 32767;

    @Override
    public String getDataName() {
        return "recipes";
    }

    @Override
    public JsonElement generateDataJson() {
        Map<Integer, JsonArray> recipesByResult = new TreeMap<>();
        for (RecipeType recipe : RecipeDispatcher.REGISTRY) {
            ItemStack output = recipe.getOutput();
            if (output.isEmpty()) continue;
            List<JsonObject> generated;
            // Subclasses (map extending) compute their real output at craft time.
            if (recipe.getClass() == ShapedRecipeType.class) {
                generated = generateShaped((ShapedRecipeType) recipe);
            } else if (recipe.getClass() == ShapelessRecipeType.class) {
                generated = generateShapeless((ShapelessRecipeType) recipe);
            } else {
                continue;
            }
            int resultId = Registries.ITEMS.getRawId(output.getItem());
            recipesByResult.computeIfAbsent(resultId, k -> new JsonArray());
            for (JsonObject json : generated) recipesByResult.get(resultId).add(json);
        }
        JsonObject result = new JsonObject();
        recipesByResult.forEach((id, recipes) -> result.add(id.toString(), recipes));
        return result;
    }

    private static List<JsonObject> generateShaped(ShapedRecipeType recipe) {
        int width = recipe.method_14272();
        int height = recipe.method_14273();
        List<List<JsonElement>> slots = new ArrayList<>();
        for (Ingredient ingredient : recipe.method_14252()) slots.add(alternativesFor(ingredient));
        List<JsonObject> recipes = new ArrayList<>();
        for (List<JsonElement> cells : cartesianProduct(slots)) {
            JsonArray inShape = new JsonArray();
            for (int y = 0; y < height; y++) {
                JsonArray row = new JsonArray();
                for (int x = 0; x < width; x++) row.add(cells.get(y * width + x));
                inShape.add(row);
            }
            JsonObject json = new JsonObject();
            json.add("inShape", inShape);
            json.add("result", resultFor(recipe.getOutput()));
            recipes.add(json);
        }
        return recipes;
    }

    private static List<JsonObject> generateShapeless(ShapelessRecipeType recipe) {
        List<List<JsonElement>> slots = new ArrayList<>();
        for (Ingredient ingredient : recipe.method_14252()) {
            List<JsonElement> alternatives = alternativesFor(ingredient);
            if (!alternatives.get(0).isJsonNull()) slots.add(alternatives);
        }
        List<JsonObject> recipes = new ArrayList<>();
        for (List<JsonElement> cells : cartesianProduct(slots)) {
            JsonArray ingredients = new JsonArray();
            cells.forEach(ingredients::add);
            JsonObject json = new JsonObject();
            json.add("ingredients", ingredients);
            json.add("result", resultFor(recipe.getOutput()));
            recipes.add(json);
        }
        return recipes;
    }

    private static JsonObject resultFor(ItemStack output) {
        JsonObject result = new JsonObject();
        result.addProperty("id", Registries.ITEMS.getRawId(output.getItem()));
        result.addProperty("metadata", output.getDamage());
        result.addProperty("count", output.getCount());
        return result;
    }

    // An ingredient is a list of accepted stacks. A bare id means any metadata of that item;
    // it is used when the stacks cover every variant of one item, matching how vanilla
    // accepts each slot independently (mixed plank types craft a crafting table).
    private static List<JsonElement> alternativesFor(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.method_14244();
        List<JsonElement> alternatives = new ArrayList<>();
        if (stacks.length == 0) {
            alternatives.add(JsonNull.INSTANCE);
            return alternatives;
        }
        Item item = stacks[0].getItem();
        boolean sameItem = true;
        Set<Integer> damages = new HashSet<>();
        for (ItemStack stack : stacks) {
            sameItem &= stack.getItem() == item;
            damages.add(stack.getDamage());
        }
        if (sameItem && (damages.contains(WILDCARD_DAMAGE) || damages.containsAll(variantsOf(item)))) {
            alternatives.add(new JsonPrimitive(Registries.ITEMS.getRawId(item)));
            return alternatives;
        }
        for (ItemStack stack : stacks) alternatives.add(cellFor(stack));
        return alternatives;
    }

    private static JsonElement cellFor(ItemStack stack) {
        int id = Registries.ITEMS.getRawId(stack.getItem());
        if (stack.getDamage() == WILDCARD_DAMAGE || variantsOf(stack.getItem()).size() <= 1 && stack.getDamage() == 0) {
            return new JsonPrimitive(id);
        }
        JsonObject cell = new JsonObject();
        cell.addProperty("id", id);
        cell.addProperty("metadata", stack.getDamage());
        return cell;
    }

    // Metadata values the item exists with; an item without variants is matched by id alone.
    private static Set<Integer> variantsOf(Item item) {
        DefaultedList<ItemStack> stacks = DefaultedList.of();
        item.appendToItemGroup(ItemGroup.SEARCH, stacks);
        Set<Integer> variants = new HashSet<>();
        for (ItemStack stack : stacks) variants.add(stack.getDamage());
        return variants;
    }

    private static <T> List<List<T>> cartesianProduct(List<List<T>> slots) {
        List<List<T>> combinations = new ArrayList<>();
        combinations.add(new ArrayList<>());
        for (List<T> alternatives : slots) {
            List<List<T>> next = new ArrayList<>();
            for (List<T> prefix : combinations) {
                for (T alternative : alternatives) {
                    List<T> extended = new ArrayList<>(prefix);
                    extended.add(alternative);
                    next.add(extended);
                }
            }
            combinations = next;
        }
        return combinations;
    }
}
