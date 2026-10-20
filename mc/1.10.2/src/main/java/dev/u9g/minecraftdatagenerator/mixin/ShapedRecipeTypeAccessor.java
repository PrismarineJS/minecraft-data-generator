package dev.u9g.minecraftdatagenerator.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapedRecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ShapedRecipeType.class)
public interface ShapedRecipeTypeAccessor {
    @Accessor("width")
    int getWidth();

    @Accessor("height")
    int getHeight();

    @Accessor("ingredients")
    ItemStack[] getIngredients();
}
