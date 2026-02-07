package justs_js.cel.client.api.behaviour;

import com.google.common.collect.ImmutableMap;
import justs_js.cel.client.api.ClientEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class ClientFollowTargetSink extends ClientBehavior<ClientEntity> {
    private final float speedModifier;

    public ClientFollowTargetSink(float f) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED), Integer.MAX_VALUE);
        this.speedModifier = f;
    }

    @Override
    protected boolean checkExtraStartConditions(ClientLevel clientLevel, ClientEntity livingEntity) {
        Entity entity = livingEntity.getFollowTargetEntity();
        return livingEntity.isAlive() && entity != null && !livingEntity.isInWater() && livingEntity.distanceToSqr(entity) <= 16.0;
    }

    @Override
    protected boolean canStillUse(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        return super.checkExtraStartConditions(clientLevel, livingEntity);
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

    private void followTarget(ClientEntity livingEntity) {
        Brain<?> brain = livingEntity.getBrain();
        if (livingEntity.getFollowTargetEntity() == null) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
            return;
        }
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(new EntityTracker(livingEntity.getFollowTargetEntity(), false), this.speedModifier, 2));
        brain.setMemory(MemoryModuleType.LOOK_TARGET, new EntityTracker(livingEntity.getFollowTargetEntity(), true));
    }

    @Override
    public boolean tryStart(ServerLevel serverLevel, ClientEntity livingEntity, long l) {return false;}

    @Override
    public void tickOrStop(ServerLevel serverLevel, ClientEntity livingEntity, long l) {}

    @Override
    public void doStop(ServerLevel serverLevel, ClientEntity livingEntity, long l) {}
}
