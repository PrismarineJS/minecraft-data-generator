package dev.u9g.minecraftdatagenerator.mixin;

import dev.u9g.minecraftdatagenerator.util.RayShapeCapture;
import net.minecraft.block.Block;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockRayShapeMixin {
    @Shadow protected double boundingBoxMinX;
    @Shadow protected double boundingBoxMinY;
    @Shadow protected double boundingBoxMinZ;
    @Shadow protected double boundingBoxMaxX;
    @Shadow protected double boundingBoxMaxY;
    @Shadow protected double boundingBoxMaxZ;

    @Inject(method = "rayTrace", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/Block;setBoundingBox(Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)V", shift = At.Shift.AFTER))
    private void captureRayShape(World world, BlockPos pos, Vec3d start, Vec3d end, CallbackInfoReturnable<BlockHitResult> callback) {
        RayShapeCapture.add(boundingBoxMinX, boundingBoxMinY, boundingBoxMinZ, boundingBoxMaxX, boundingBoxMaxY, boundingBoxMaxZ);
    }
}
