package justs_js.cel.client.api.behaviour;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Mob;

public class ClientSwim<T extends Mob> extends ClientBehavior<T> {
    protected final float chance;

    public ClientSwim(float f) {
        super(ImmutableMap.of());
        this.chance = f;
    }

    public static <T extends Mob> boolean shouldSwim(T mob) {
        return mob.isInWater() && mob.getFluidHeight(FluidTags.WATER) > mob.getFluidJumpThreshold() || mob.isInLava();
    }

    @Override
    protected boolean checkExtraStartConditions(ClientLevel clientLevel, T livingEntity) {
        return shouldSwim(livingEntity);
    }

    @Override
    protected boolean canStillUse(ClientLevel clientLevel, T livingEntity, long l) {
        return super.checkExtraStartConditions(clientLevel, livingEntity);
    }

    @Override
    protected void tick(ClientLevel clientLevel, T livingEntity, long l) {
        if (livingEntity.getRandom().nextFloat() < this.chance) {
            livingEntity.getJumpControl().jump();
        }
    }
}
