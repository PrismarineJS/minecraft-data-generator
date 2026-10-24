package dev.u9g.minecraftdatagenerator.mixin;

import java.util.List;
import net.minecraft.block.BlockState;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(StairsBlock.class)
public interface StairsBlockInvoker {
    // The same per-state boxes are visited by the native stair ray routine.
    @Invoker("method_11634")
    static List<Box> rayShapes(BlockState state) {
        throw new IllegalStateException();
    }
}
