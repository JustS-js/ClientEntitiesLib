package justs_js.cel.client.api.sensor;

import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap;
import justs_js.cel.CELModLib;
import justs_js.cel.client.mixin.NearestVisibleLivingEntitiesAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.AABB;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ClientNearestLivingEntitySensor<T extends LivingEntity> extends ClientSensor<T> {

    public void doTick(ClientLevel clientLevel, T livingEntity) {
        double followRange = livingEntity.getAttributeValue(Attributes.FOLLOW_RANGE);
        AABB boundingBox = livingEntity.getBoundingBox().inflate(followRange, followRange, followRange);
        List<LivingEntity> livingEntities = clientLevel.getEntitiesOfClass(
                LivingEntity.class,
                boundingBox,
                (entity) -> entity != livingEntity && entity.isAlive()
        );
        Objects.requireNonNull(livingEntity);
        livingEntities.sort(Comparator.comparingDouble(livingEntity::distanceToSqr));
        Brain<?> brain = livingEntity.getBrain();
        brain.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, livingEntities);
        NearestVisibleLivingEntities nearestVisibleLivingEntities = new NearestVisibleLivingEntities(null, livingEntity, livingEntities);
        Object2BooleanOpenHashMap<LivingEntity> object2BooleanOpenHashMap = new Object2BooleanOpenHashMap<>(livingEntities.size());
        ((NearestVisibleLivingEntitiesAccessor)nearestVisibleLivingEntities).cel$setLineOfSightTest(
                (otherEntity) -> object2BooleanOpenHashMap.computeIfAbsent(
                        otherEntity,
                        (targetEntity) -> isEntityTargetable(clientLevel, livingEntity, (LivingEntity) targetEntity)
                )
        );
        brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, nearestVisibleLivingEntities);
    }

    @Override
    public Set<MemoryModuleType<?>> requires() {
        return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
    }
}
