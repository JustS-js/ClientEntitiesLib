package justs_js.cel.client.api.behaviour;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.phys.Vec3;

public class ClientRandomLookAround extends ClientBehavior<Mob> {
    protected final IntProvider interval;
    protected final float maxYaw;
    protected final float minPitch;
    protected final float pitchRange;

    public ClientRandomLookAround(IntProvider intProvider, float f, float g, float h) {
        super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.GAZE_COOLDOWN_TICKS, MemoryStatus.VALUE_ABSENT));
        if (g > h) {
            throw new IllegalArgumentException("Minimum pitch is larger than maximum pitch! " + g + " > " + h);
        } else {
            this.interval = intProvider;
            this.maxYaw = f;
            this.minPitch = g;
            this.pitchRange = h - g;
        }
    }

    @Override
    protected void start(ClientLevel clientLevel, Mob livingEntity, long l) {
        RandomSource randomSource = livingEntity.getRandom();
        float f = Mth.clamp(randomSource.nextFloat() * this.pitchRange + this.minPitch, -90.0F, 90.0F);
        float g = Mth.wrapDegrees(livingEntity.getYRot() + 2.0F * randomSource.nextFloat() * this.maxYaw - this.maxYaw);
        Vec3 vec3 = Vec3.directionFromRotation(f, g);
        livingEntity.getBrain().setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(livingEntity.getEyePosition().add(vec3)));
        livingEntity.getBrain().setMemory(MemoryModuleType.GAZE_COOLDOWN_TICKS, this.interval.sample(randomSource));
    }
}
