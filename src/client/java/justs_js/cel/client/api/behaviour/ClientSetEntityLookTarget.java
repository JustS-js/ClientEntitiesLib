package justs_js.cel.client.api.behaviour;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.OneShot;
import net.minecraft.world.entity.ai.behavior.declarative.BehaviorBuilder;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;

import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class ClientSetEntityLookTarget {
    ClientSetEntityLookTarget() {}

    public static ClientBehaviorControl<LivingEntity> create(MobCategory mobCategory, float f) {
        return create((livingEntity) -> {
            return mobCategory.equals(livingEntity.getType().getCategory());
        }, f);
    }

    public static ClientOneShot<LivingEntity> create(EntityType<?> entityType, float f) {
        return create((livingEntity) -> {
            return entityType.equals(livingEntity.getType());
        }, f);
    }

    public static ClientOneShot<LivingEntity> create(float f) {
        return create((livingEntity) -> {
            return true;
        }, f);
    }

    public static ClientOneShot<LivingEntity> create(Predicate<LivingEntity> predicate, float f) {
        float g = f * f;
        return new ClientOneShot<>() {
            @Override
            public Set<MemoryModuleType<?>> getRequiredMemories() {
                return Set.of(MemoryModuleType.LOOK_TARGET);
            }

            @Override
            public boolean trigger(ClientLevel clientLevel, LivingEntity livingEntity, long l) {
                Brain<?> brain = livingEntity.getBrain();

                if (brain.hasMemoryValue(MemoryModuleType.LOOK_TARGET)) {
                    return false;
                }

                if (!brain.hasMemoryValue(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES)) {
                    return false;
                }
                NearestVisibleLivingEntities nearestEntities =
                        brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES).get();

                Optional<LivingEntity> target = nearestEntities.findClosest(
                        livingEntity2 -> predicate.test(livingEntity2) &&
                                livingEntity2.distanceToSqr(livingEntity) <= g &&
                                !livingEntity.hasPassenger(livingEntity2)
                );

                if (target.isEmpty()) {
                    return false;
                }

                brain.setMemory(MemoryModuleType.LOOK_TARGET,
                        new EntityTracker(target.get(), true));
                return true;
            }
        };
    }
}
