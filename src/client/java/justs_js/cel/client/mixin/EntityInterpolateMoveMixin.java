package justs_js.cel.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import justs_js.cel.client.api.ClientEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityInterpolateMoveMixin {

    @Shadow
    public abstract float getXRot();

    @Shadow
    public abstract float getYRot();

    @ModifyArg(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;setPos(Lnet/minecraft/world/phys/Vec3;)V"
            )
    )
    private Vec3 cel$removeSetPosArgument(Vec3 vec3) {
        if (!((Entity)(Object)this instanceof ClientEntity)) {
            return vec3;
        }
        return ((ClientEntity)(Object)this).position();
    }

    @Inject(
            method = "move",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;setPos(Lnet/minecraft/world/phys/Vec3;)V",
                    shift = At.Shift.BY
            )
    )
    private void cel$interpolateSetPos(MoverType moverType, Vec3 vec3, CallbackInfo ci, @Local(ordinal = 3) Vec3 vec35) {
        if (!((Entity)(Object)this instanceof ClientEntity)) {
            return;
        }
        ((ClientEntity)(Object)this).moveOrInterpolateTo(vec35, getXRot(), getYRot());
    }
}
