package dev.u9g.minecraftdatagenerator.generators;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.u9g.minecraftdatagenerator.mixin.ShapedRecipeTypeAccessor;
import dev.u9g.minecraftdatagenerator.mixin.ShapelessRecipeTypeAccessor;
import dev.u9g.minecraftdatagenerator.util.Registries;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.itemgroup.ItemGroup;
import net.minecraft.recipe.RecipeDispatcher;
import net.minecraft.recipe.RecipeType;
import net.minecraft.recipe.ShapedRecipeType;
import net.minecraft.recipe.ShapelessRecipeType;

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
        for (RecipeType recipe : RecipeDispatcher.getInstance().getAllRecipes()) {
            ItemStack output = recipe.getOutput();
            if (isEmpty(output)) continue;
            JsonObject json;
            // Subclasses (map extending) compute their real output at craft time.
            if (recipe.getClass() == ShapedRecipeType.class) {
                json = generateShaped((ShapedRecipeType) recipe);
            } else if (recipe.getClass() == ShapelessRecipeType.class) {
                json = generateShapeless((ShapelessRecipeType) recipe);
            } else {
                continue;
            }
            int resultId = Registries.ITEMS.getRawId(output.getItem());
            recipesByResult.computeIfAbsent(resultId, k -> new JsonArray()).add(json);
        }
        JsonObject result = new JsonObject();
        recipesByResult.forEach((id, recipes) -> result.add(id.toString(), recipes));
        return result;
    }

    private static JsonObject generateShaped(ShapedRecipeType recipe) {
        ShapedRecipeTypeAccessor accessor = (ShapedRecipeTypeAccessor) recipe;
        int width = accessor.getWidth();
        int height = accessor.getHeight();
        ItemStack[] ingredients = accessor.getIngredients();
        // Crafting leaves each ingredient's recipe remainder in its slot (milk bucket leaves a bucket).
        JsonArray inShape = new JsonArray();
        JsonArray outShape = new JsonArray();
        boolean hasRemainder = false;
        for (int y = 0; y < height; y++) {
            JsonArray row = new JsonArray();
            JsonArray outRow = new JsonArray();
            for (int x = 0; x < width; x++) {
                ItemStack stack = ingredients[y * width + x];
                row.add(cellFor(stack));
                Item remainder = isEmpty(stack) ? null : stack.getItem().getRecipeRemainder();
                hasRemainder |= remainder != null;
                outRow.add(remainder == null ? JsonNull.INSTANCE : new JsonPrimitive(Registries.ITEMS.getRawId(remainder)));
            }
            inShape.add(row);
            outShape.add(outRow);
        }
        JsonObject json = new JsonObject();
        json.add("inShape", inShape);
        if (hasRemainder) json.add("outShape", outShape);
        json.add("result", resultFor(recipe.getOutput()));
        return json;
    }

    private static JsonObject generateShapeless(ShapelessRecipeType recipe) {
        JsonArray ingredients = new JsonArray();
        for (ItemStack stack : ((ShapelessRecipeTypeAccessor) recipe).getStacks()) {
            if (!isEmpty(stack)) ingredients.add(cellFor(stack));
        }
        JsonObject json = new JsonObject();
        json.add("ingredients", ingredients);
        json.add("result", resultFor(recipe.getOutput()));
        return json;
    }

    private static JsonObject resultFor(ItemStack output) {
        JsonObject result = new JsonObject();
        result.addProperty("id", Registries.ITEMS.getRawId(output.getItem()));
        result.addProperty("metadata", output.getDamage());
        result.addProperty("count", output.count);
        return result;
    }

    // A bare id means any metadata of that item: the vanilla wildcard damage, or an item
    // that only exists with one metadata value.
    private static JsonElement cellFor(ItemStack stack) {
        if (isEmpty(stack)) return JsonNull.INSTANCE;
        int id = Registries.ITEMS.getRawId(stack.getItem());
        if (stack.getDamage() == WILDCARD_DAMAGE || variantsOf(stack.getItem()).size() <= 1 && stack.getDamage() == 0) {
            return new JsonPrimitive(id);
        }
        JsonObject cell = new JsonObject();
        cell.addProperty("id", id);
        cell.addProperty("metadata", stack.getDamage());
        return cell;
    }

    private static Set<Integer> variantsOf(Item item) {
        List<ItemStack> stacks = new ArrayList<>();
        item.appendItemStacks(item, ItemGroup.SEARCH, stacks);
        Set<Integer> variants = new HashSet<>();
        for (ItemStack stack : stacks) variants.add(stack.getDamage());
        return variants;
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null;
    }
}
