package dev.u9g.minecraftdatagenerator.mixin;

import net.minecraft.item.ItemStack;
import net.minecraft.recipe.ShapelessRecipeType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(ShapelessRecipeType.class)
public interface ShapelessRecipeTypeAccessor {
    @Accessor("stacks")
    List<ItemStack> getStacks();
}
