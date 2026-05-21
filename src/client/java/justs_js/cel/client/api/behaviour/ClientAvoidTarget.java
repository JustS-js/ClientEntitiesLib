package justs_js.cel.client.api.behaviour;

import com.google.common.collect.ImmutableMap;
import justs_js.cel.client.api.ClientEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ClientAvoidTarget extends ClientBehavior<ClientEntity> {
    protected final float walkSpeedModifier;
    protected final float sprintSpeedModifier;
    protected final double maxDist;
    protected Vec3 pos;

    public ClientAvoidTarget(float walkSpeedModifier, float sprintSpeedModifier, double maxDist) {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.AVOID_TARGET, MemoryStatus.REGISTERED), Integer.MAX_VALUE);
        this.walkSpeedModifier = walkSpeedModifier;
        this.sprintSpeedModifier = sprintSpeedModifier;
        this.maxDist = maxDist;
    }

    @Override
    protected boolean checkExtraStartConditions(ClientLevel clientLevel, ClientEntity livingEntity) {
        Brain<?> brain = livingEntity.getBrain();
        Optional<LivingEntity> entity = brain.getMemory(MemoryModuleType.AVOID_TARGET);
        if (!livingEntity.isAlive() || entity.isEmpty()) {
            return false;
        }

        LivingEntity toAvoid = entity.get();
        if (toAvoid.distanceToSqr(livingEntity) > maxDist*maxDist) {
            return false;
        }
        pos = getPosAway(livingEntity, (int)maxDist, (int)maxDist, toAvoid.position());
        if (pos == null) {
            return false;
        } else if (toAvoid.distanceToSqr(pos.x, pos.y, pos.z) < toAvoid.distanceToSqr(livingEntity)) {
            return false;
        } else {
            return livingEntity.getNavigation().createPath(pos.x, pos.y, pos.z, 0) != null;
        }
    }

    @Override
    protected boolean canStillUse(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        return !livingEntity.getNavigation().isDone();
    }

    @Override
    protected void start(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        Brain<?> brain = livingEntity.getBrain();
        brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, walkSpeedModifier, 2));
    }

    @Override
    protected void stop(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        Brain<?> brain = livingEntity.getBrain();
        brain.eraseMemory(MemoryModuleType.WALK_TARGET);
    }

    @Override
    protected void tick(ClientLevel clientLevel, ClientEntity livingEntity, long l) {
        Brain<?> brain = livingEntity.getBrain();
        if (brain.getMemory(MemoryModuleType.AVOID_TARGET).isEmpty()) {
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            return;
        }

        LivingEntity toAvoid = brain.getMemory(MemoryModuleType.AVOID_TARGET).get();
        if (livingEntity.distanceToSqr(toAvoid) < this.maxDist) {
            livingEntity.getNavigation().setSpeedModifier(this.sprintSpeedModifier);
        } else {
            livingEntity.getNavigation().setSpeedModifier(this.walkSpeedModifier);
        }
    }

    @Override
    protected boolean timedOut(long l) {
        return false;
    }

    protected static @Nullable Vec3 getPosAway(PathfinderMob pathfinderMob, int maxHorizontalDistance, int maxVerticalDistance, Vec3 avoidPos) {
        if (pathfinderMob.getNavigation().canFloat()) {
            Vec3 dirAway = pathfinderMob.position().subtract(avoidPos);
            return AirAndWaterRandomPos.getPos(pathfinderMob, maxHorizontalDistance, maxVerticalDistance, -2, dirAway.x, dirAway.z, (double)((float)Math.PI / 2F));
        }
        return DefaultRandomPos.getPosAway(pathfinderMob, maxHorizontalDistance, maxVerticalDistance, avoidPos);
    }

    @Override
    public boolean tryStart(ServerLevel serverLevel, ClientEntity livingEntity, long l) {return false;}

    @Override
    public void tickOrStop(ServerLevel serverLevel, ClientEntity livingEntity, long l) {}

    @Override
    public void doStop(ServerLevel serverLevel, ClientEntity livingEntity, long l) {}
}
