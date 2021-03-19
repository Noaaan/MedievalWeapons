package net.medievalweapons.mixin;

import com.mojang.authlib.GameProfile;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import org.spongepowered.asm.mixin.injection.At;

import net.medievalweapons.init.ItemInit;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin extends PlayerEntity {

    public AbstractClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile profile) {
        super(world, pos, yaw, profile);
    }

    @Inject(method = "getSpeed", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT, cancellable = true)
    public void getSpeedMixin(CallbackInfoReturnable<Float> info, float f) {
        if (this.isUsingItem() && (this.getActiveItem().getItem() == ItemInit.LONG_BOW_ITEM
                || this.getActiveItem().getItem() == ItemInit.RECURVE_BOW_ITEM)) {
            int i = this.getItemUseTime();
            float g = (float) i / 20.0F;
            if (g > 1.0F) {
                g = 1.0F;
            } else {
                g *= g;
            }

            f *= 1.0F - g * 0.15F;
            info.setReturnValue(MathHelper.lerp(MinecraftClient.getInstance().options.fovEffectScale, 1.0F, f));
        }

    }

}
