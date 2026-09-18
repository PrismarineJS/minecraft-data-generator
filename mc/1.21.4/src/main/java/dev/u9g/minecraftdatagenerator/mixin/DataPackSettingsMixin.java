/*
 * SoulFire
 * Copyright (C) 2024  AlexProgrammerDE
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package dev.u9g.minecraftdatagenerator.mixin;

import net.minecraft.resource.DataPackSettings;
import net.minecraft.resource.featuretoggle.FeatureFlags;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(DataPackSettings.class)
public class DataPackSettingsMixin {
  @Inject(method = "getEnabled", at = @At("HEAD"), cancellable = true)
  public void getEnabled(CallbackInfoReturnable<List<String>> cir) {
    cir.setReturnValue(FeatureFlags.FEATURE_MANAGER.toId(FeatureFlags.FEATURE_MANAGER.getFeatureSet()).stream().map(Identifier::getPath).toList());
  }
}
