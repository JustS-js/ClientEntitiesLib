package justs_js.cel.client.api.behaviour;

import com.google.common.collect.ImmutableMap;
import justs_js.cel.client.api.ClientEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

public class ClientAvoidTarget extends ClientBehavior<ClientEntity> {
    protected final float speedModifier;
    protected final double maxDist;

    public ClientAvoidTarget(float speedModifier, double maxDist) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.AVOID_TARGET, MemoryStatus.REGISTERED), Integer.MAX_VALUE);
        this.speedModifier = speedModifier;
        this.maxDist = maxDist;
    }

    @Override
    protected boolean checkExtraStartConditions(ClientLevel clientLevel, ClientEntity livingEntity) {
        Optional<LivingEntity> entity = livingEntity.getBrain().getMemory(MemoryModuleType.AVOID_TARGET);
        return livingEntity.isAlive() && entity.isPresent() && livingEntity.distanceToSqr(entity.get()) <= this.maxDist*this.maxDist && !livingEntity.getNavigation().isInProgress();
    }

    @Override
    protected boolean canStillUse(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        return this.checkExtraStartConditions(clientLevel, livingEntity);
    }

    @Override
    protected void start(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        this.avoidTarget(livingEntity);
    }

    @Override
    protected void stop(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        Brain<?> brain = livingEntity.getBrain();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void tick(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        this.avoidTarget(livingEntity);
    }

    @Override
    protected boolean timedOut(long l) {
        return false;
    }

    private void avoidTarget(ClientEntity livingEntity) {
        Brain<?> brain = livingEntity.getBrain();
        if (brain.getMemory(MemoryModuleType.AVOID_TARGET).isEmpty()) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            return;
        }

        float speedModifier = this.speedModifier;
        LivingEntity toAvoid = brain.getMemory(MemoryModuleType.AVOID_TARGET).get();
        if (livingEntity.distanceToSqr(toAvoid) < this.maxDist) {
            speedModifier *= 2;
        }

        Vec3 pos;
        for (int i = 0; i < 16; i++) {
            pos = DefaultRandomPos.getPosAway(livingEntity, (int)maxDist, (int)maxDist, toAvoid.position());
            if (pos != null) {
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, speedModifier, 2));
                break;
            }
        }
    }

    @Override
    public boolean tryStart(ServerLevel serverLevel, ClientEntity livingEntity, long l) {return false;}

    @Override
    public void tickOrStop(ServerLevel serverLevel, ClientEntity livingEntity, long l) {}

    @Override
    public void doStop(ServerLevel serverLevel, ClientEntity livingEntity, long l) {}
}
