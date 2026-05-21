package justs_js.cel.client.api.behaviour;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class ClientMoveToTargetSink extends ClientBehavior<Mob> {

    protected static final int MAX_COOLDOWN_BEFORE_RETRYING = 40;
    protected int remainingCooldown;
    @Nullable
    protected Path path;
    @Nullable
    protected BlockPos lastTargetPos;
    protected float speedModifier;

    public ClientMoveToTargetSink() {
        this(150, 250);
    }

    public ClientMoveToTargetSink(int i, int j) {
        super(ImmutableMap.of(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, MemoryStatus.REGISTERED, MemoryModuleType.PATH, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_PRESENT), i, j);
    }

    @Override
    protected boolean checkExtraStartConditions(ClientLevel clientLevel, Mob livingEntity) {
        if (this.remainingCooldown > 0) {
            --this.remainingCooldown;
            return false;
        } else {
            Brain<?> brain = livingEntity.getBrain();
            WalkTarget walkTarget = brain.getMemory(MemoryModuleType.WALK_TARGET).get();
            boolean bl = this.reachedTarget(livingEntity, walkTarget);
            if (!bl && this.tryComputePath(livingEntity, walkTarget, clientLevel.getGameTime())) {
                this.lastTargetPos = walkTarget.getTarget().currentBlockPosition();
                return true;
            } else {
                brain.eraseMemory(MemoryModuleType.WALK_TARGET);
                if (bl) {
                    brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
                }

                return false;
            }
        }
    }

    @Override
    protected boolean canStillUse(ClientLevel clientLevel, Mob livingEntity, long l) {
        if (this.path != null && this.lastTargetPos != null) {
            Optional<WalkTarget> optional = livingEntity.getBrain().getMemory(MemoryModuleType.WALK_TARGET);
            boolean bl = (Boolean)optional.map(ClientMoveToTargetSink::isWalkTargetSpectator).orElse(false);
            PathNavigation pathNavigation = livingEntity.getNavigation();
            return !pathNavigation.isDone() && optional.isPresent() && !this.reachedTarget(livingEntity, (WalkTarget)optional.get()) && !bl;
        } else {
            return false;
        }
    }

    @Override
    protected void stop(ClientLevel clientLevel, Mob livingEntity, long l) {
        if (livingEntity.getBrain().hasMemoryValue(MemoryModuleType.WALK_TARGET) && !this.reachedTarget(livingEntity, (WalkTarget)livingEntity.getBrain().getMemory(MemoryModuleType.WALK_TARGET).get()) && livingEntity.getNavigation().isStuck()) {
            this.remainingCooldown = clientLevel.getRandom().nextInt(MAX_COOLDOWN_BEFORE_RETRYING);
        }

        livingEntity.getNavigation().stop();
        livingEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
        livingEntity.getBrain().eraseMemory(MemoryModuleType.PATH);
        this.path = null;
    }

    @Override
    protected void start(ClientLevel clientLevel, Mob livingEntity, long l) {
        livingEntity.getBrain().setMemory(MemoryModuleType.PATH, this.path);
        livingEntity.getNavigation().moveTo(this.path, (double)this.speedModifier);
    }

    @Override
    protected void tick(ClientLevel clientLevel, Mob livingEntity, long l) {
        Path path = livingEntity.getNavigation().getPath();
        Brain<?> brain = livingEntity.getBrain();
        if (this.path != path) {
            this.path = path;
            brain.setMemory(MemoryModuleType.PATH, path);
        }

        if (path != null && this.lastTargetPos != null) {
            WalkTarget walkTarget = (WalkTarget)brain.getMemory(MemoryModuleType.WALK_TARGET).get();
            if (walkTarget.getTarget().currentBlockPosition().distSqr(this.lastTargetPos) > 4.0 && this.tryComputePath(livingEntity, walkTarget, clientLevel.getGameTime())) {
                this.lastTargetPos = walkTarget.getTarget().currentBlockPosition();
                this.start(clientLevel, livingEntity, l);
            }

        }
    }

    protected boolean tryComputePath(Mob mob, WalkTarget walkTarget, long l) {
        BlockPos blockPos = walkTarget.getTarget().currentBlockPosition();
        this.path = mob.getNavigation().createPath(blockPos, 0);
        this.speedModifier = walkTarget.getSpeedModifier();
        Brain<?> brain = mob.getBrain();
        if (this.reachedTarget(mob, walkTarget)) {
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
        } else {
            boolean bl = this.path != null && this.path.canReach();
            if (bl) {
                brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);
            } else if (!brain.hasMemoryValue(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE)) {
                brain.setMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE, l);
            }

            if (this.path != null) {
                return true;
            }

            Vec3 vec3 = DefaultRandomPos.getPosTowards((PathfinderMob)mob, 10, 7, Vec3.atBottomCenterOf(blockPos), 1.5707963705062866);
            if (vec3 != null) {
                this.path = mob.getNavigation().createPath(vec3.x, vec3.y, vec3.z, 0);
                return this.path != null;
            }
        }

        return false;
    }

    protected boolean reachedTarget(Mob mob, WalkTarget walkTarget) {
        return walkTarget.getTarget().currentBlockPosition().distManhattan(mob.blockPosition()) <= walkTarget.getCloseEnoughDist();
    }

    protected static boolean isWalkTargetSpectator(WalkTarget walkTarget) {
        PositionTracker positionTracker = walkTarget.getTarget();
        if (positionTracker instanceof EntityTracker entityTracker) {
            return entityTracker.getEntity().isSpectator();
        } else {
            return false;
        }
    }
}
