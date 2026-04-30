package justs_js.cel.client.api.behaviour;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.RandomStroll;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.behavior.declarative.Trigger;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.AirAndWaterRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public class ClientRandomStroll {
    private static final int MAX_XZ_DIST = 10;
    private static final int MAX_Y_DIST = 7;
    private static final int[][] SWIM_XY_DISTANCE_TIERS = new int[][]{{1, 1}, {3, 3}, {5, 5}, {6, 5}, {7, 7}, {10, 7}};

    public ClientRandomStroll() {
    }

    public static ClientOneShot<PathfinderMob> stroll(float f) {
        return stroll(f, true);
    }

    public static ClientOneShot<PathfinderMob> stroll(float f, boolean bl) {
        return strollFlyOrSwim(f, (pathfinderMob) -> {
            return LandRandomPos.getPos(pathfinderMob, MAX_XZ_DIST, MAX_Y_DIST);
        }, bl ? (pathfinderMob) -> {
            return true;
        } : (pathfinderMob) -> {
            return !pathfinderMob.isInWater();
        });
    }

    public static ClientBehaviorControl<PathfinderMob> stroll(float f, int i, int j) {
        return strollFlyOrSwim(f, (pathfinderMob) -> {
            return LandRandomPos.getPos(pathfinderMob, i, j);
        }, (pathfinderMob) -> {
            return true;
        });
    }

    public static ClientBehaviorControl<PathfinderMob> fly(float f) {
        return strollFlyOrSwim(f, (pathfinderMob) -> {
            return getTargetFlyPos(pathfinderMob, MAX_XZ_DIST, MAX_Y_DIST);
        }, (pathfinderMob) -> {
            return true;
        });
    }

    public static ClientBehaviorControl<PathfinderMob> swim(float f) {
        return strollFlyOrSwim(f, ClientRandomStroll::getTargetSwimPos, Entity::isInWater);
    }

    private static ClientOneShot<PathfinderMob> strollFlyOrSwim(float f, Function<PathfinderMob, Vec3> function, Predicate<PathfinderMob> predicate) {
        return new ClientOneShot<>() {
            @Override
            public Set<MemoryModuleType<?>> getRequiredMemories() {
                return Set.of(MemoryModuleType.LOOK_TARGET);
            }

            @Override
            public boolean trigger(ClientLevel serverLevel, PathfinderMob livingEntity, long l) {
                if (!predicate.test(livingEntity)) {
                    return false;
                }
                Optional<Vec3> targetPosition = Optional.ofNullable(function.apply(livingEntity));
                if (targetPosition.isPresent()) {
                    livingEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET,
                            new WalkTarget(targetPosition.get(), f, 0));
                } else {
                    livingEntity.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);
                }
                return true;
            }
        };
    }

    @Nullable
    private static Vec3 getTargetSwimPos(PathfinderMob pathfinderMob) {
        Vec3 vec3 = null;
        Vec3 vec32 = null;
        int[][] var3 = SWIM_XY_DISTANCE_TIERS;
        int var4 = var3.length;

        for(int var5 = 0; var5 < var4; ++var5) {
            int[] is = var3[var5];
            if (vec3 == null) {
                vec32 = BehaviorUtils.getRandomSwimmablePos(pathfinderMob, is[0], is[1]);
            } else {
                vec32 = pathfinderMob.position().add(pathfinderMob.position().vectorTo(vec3).normalize().multiply((double)is[0], (double)is[1], (double)is[0]));
            }

            if (vec32 == null || pathfinderMob.level().getFluidState(BlockPos.containing(vec32)).isEmpty()) {
                return vec3;
            }

            vec3 = vec32;
        }

        return vec32;
    }

    @Nullable
    private static Vec3 getTargetFlyPos(PathfinderMob pathfinderMob, int i, int j) {
        Vec3 vec3 = pathfinderMob.getViewVector(0.0F);
        return AirAndWaterRandomPos.getPos(pathfinderMob, i, j, -2, vec3.x, vec3.z, 1.5707963705062866);
    }
}
