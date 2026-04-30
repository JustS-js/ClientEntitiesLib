package justs_js.cel.client;

import justs_js.cel.CELModLib;
import justs_js.cel.client.api.ClientEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.entity.EntityTickList;

import java.util.*;
import java.util.function.Consumer;

public class ClientEntitiesController {
    private final EntityTickList entityTickList = new EntityTickList();

    private int clientEntitiesId = Integer.MAX_VALUE;

    public void forEach(Consumer<Entity> consumer) {
        entityTickList.forEach(consumer);
    }

    public void addEntity(ClientEntity entity) {
        entity.setId(--clientEntitiesId);
        entityTickList.add(entity);
        Minecraft.getInstance().level.addEntity(entity);
    }

    public void tick(ClientLevel clientLevel) {
        if (clientLevel.getGameTime() % 5 != 0) return;
        ProfilerFiller profilerFiller = Profiler.get();
        Set<Entity> removed = new HashSet<>();
        this.entityTickList.forEach((entity) -> {
            if (entity.isRemoved()) {
                removed.add(entity);
                return;
            }
            profilerFiller.push("checkDespawn");
            entity.checkDespawn();
            profilerFiller.pop();
            Entity entity2 = entity.getVehicle();
            if (entity2 != null) {
                if (!entity2.isRemoved() && entity2.hasPassenger(entity)) {
                    return;
                }

                entity.stopRiding();
            }

            profilerFiller.push("tick");
            clientLevel.guardEntityTick(this::tickNonPassenger, entity);
            profilerFiller.pop();
        });
        for (Entity entity : removed) {
            this.entityTickList.remove(entity);
        }
    }

    public void tickNonPassenger(Entity entity) {
        entity.setOldPosAndRot();
        ProfilerFiller profilerFiller = Profiler.get();
        ++entity.tickCount;
        profilerFiller.push(() -> {
            return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        });
        profilerFiller.incrementCounter("tickNonPassenger");
        entity.tick();
        profilerFiller.pop();

        for (Entity entity2 : entity.getPassengers()) {
            this.tickPassenger(entity, entity2);
        }
    }

    private void tickPassenger(Entity entity, Entity entity2) {
        if (!entity2.isRemoved() && entity2.getVehicle() == entity) {
            if (entity2 instanceof Player || this.entityTickList.contains(entity2)) {
                entity2.setOldPosAndRot();
                ++entity2.tickCount;
                ProfilerFiller profilerFiller = Profiler.get();
                profilerFiller.push(() -> {
                    return BuiltInRegistries.ENTITY_TYPE.getKey(entity2.getType()).toString();
                });
                profilerFiller.incrementCounter("tickPassenger");
                entity2.rideTick();
                profilerFiller.pop();
                Iterator var4 = entity2.getPassengers().iterator();

                while(var4.hasNext()) {
                    Entity entity3 = (Entity)var4.next();
                    this.tickPassenger(entity2, entity3);
                }

            }
        } else {
            entity2.stopRiding();
        }
    }

    private <T extends Entity> EntityType<T> register(ResourceKey<EntityType<?>> resourceKey, EntityType.Builder<T> builder) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, resourceKey, builder.build(resourceKey));
    }

    private ResourceKey<EntityType<?>> cellEntityId(String string) {
        return ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(CELModLib.MOD_ID, string));
    }

    public <T extends Entity> EntityType<T> register(String string, EntityType.Builder<T> builder) {
        return register(cellEntityId(string), builder);
    }
}
