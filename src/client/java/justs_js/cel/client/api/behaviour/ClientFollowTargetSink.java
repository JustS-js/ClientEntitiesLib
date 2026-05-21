package justs_js.cel.client.api.behaviour;

import com.google.common.collect.ImmutableMap;
import justs_js.cel.client.api.ClientEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

import java.util.Optional;

public class ClientFollowTargetSink extends ClientBehavior<ClientEntity> {
    protected final float speedModifier;
    protected final double maxDist;

    public ClientFollowTargetSink(float speedModifier, double maxDist) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.INTERACTION_TARGET, MemoryStatus.REGISTERED), Integer.MAX_VALUE);
        this.speedModifier = speedModifier;
        this.maxDist = maxDist;
    }

    @Override
    protected boolean checkExtraStartConditions(ClientLevel clientLevel, ClientEntity livingEntity) {
        Optional<LivingEntity> entity = livingEntity.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET);
        return livingEntity.isAlive() && entity.isPresent() && livingEntity.distanceToSqr(entity.get()) <= this.maxDist*this.maxDist;
    }

    @Override
    protected boolean canStillUse(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        return this.checkExtraStartConditions(clientLevel, livingEntity);
    }

    @Override
    protected void start(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        this.followTarget(livingEntity);
    }

    @Override
    protected void stop(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        Brain<?> brain = livingEntity.getBrain();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
        brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
    }

    @Override
    protected void tick(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        this.followTarget(livingEntity);
    }

    @Override
    protected boolean timedOut(long l) {
        return false;
    }

    protected void followTarget(ClientEntity livingEntity) {
        Brain<?> brain = livingEntity.getBrain();
        if (brain.getMemory(MemoryModuleType.INTERACTION_TARGET).isEmpty()) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            return;
        }

        LivingEntity toFollow = brain.getMemory(MemoryModuleType.INTERACTION_TARGET).get();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(toFollow, false), this.speedModifier, 2));
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(toFollow, true));
    }

    @Override
    public boolean tryStart(ServerLevel serverLevel, ClientEntity livingEntity, long l) {return false;}

    @Override
    public void tickOrStop(ServerLevel serverLevel, ClientEntity livingEntity, long l) {}

    @Override
    public void doStop(ServerLevel serverLevel, ClientEntity livingEntity, long l) {}
}
