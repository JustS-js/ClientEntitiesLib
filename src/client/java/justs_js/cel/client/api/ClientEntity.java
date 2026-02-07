package justs_js.cel.client.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JavaOps;
import justs_js.cel.client.api.behaviour.*;
import justs_js.cel.client.api.sensor.ClientSensor;
import justs_js.cel.client.api.sensor.ClientSensorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.entity.schedule.Schedule;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public abstract class ClientEntity extends PathfinderMob {
    private static final ImmutableList<MemoryModuleType<?>> MEMORY_TYPES =
            ImmutableList.of(
                    MemoryModuleType.NEAREST_LIVING_ENTITIES,
                    MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES,
                    MemoryModuleType.NEAREST_PLAYERS,
                    MemoryModuleType.NEAREST_VISIBLE_PLAYER,
                    MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER,
                    MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYERS,
                    MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM,
                    MemoryModuleType.LOOK_TARGET,
                    MemoryModuleType.GAZE_COOLDOWN_TICKS,
                    MemoryModuleType.WALK_TARGET,
                    MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE,
                    MemoryModuleType.PATH

            );
    private static final ImmutableList<ClientSensorType<? extends ClientSensor<? super ClientEntity>>> SENSOR_TYPES =
            ImmutableList.of(
                    ClientSensorType.NEAREST_PLAYERS,
                    ClientSensorType.NEAREST_LIVING_ENTITIES
            );

    @Nullable
    private Entity followTargetEntity;
    private ClientBrain<? extends ClientEntity> brain;

    public ClientEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.brain = this.makeBrain(new Dynamic<>(JavaOps.INSTANCE, Map.of("memories", Map.of())));
    }

    @Override
    public boolean isEffectiveAi() {
        return true;
    }

    @Override
    public boolean canSimulateMovement() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        this.updateControlFlags();
    }

    @Override
    public void checkDespawn() {
        if (this.level() != Minecraft.getInstance().level) {
            this.discard();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        ProfilerFiller profilerFiller = Profiler.get();
        profilerFiller.push("newAi");
        this.clientAiStep();
        profilerFiller.pop();
    }

    protected final void clientAiStep() {
        ++this.noActionTime;
        ProfilerFiller profilerFiller = Profiler.get();
        profilerFiller.push("sensing");
        this.getSensing().tick();
        profilerFiller.pop();
        int i = this.tickCount + this.getId();
        if (i % 2 != 0 && this.tickCount > 1) {
            profilerFiller.push("targetSelector");
            this.targetSelector.tickRunningGoals(false);
            profilerFiller.pop();
            profilerFiller.push("goalSelector");
            this.goalSelector.tickRunningGoals(false);
            profilerFiller.pop();
        } else {
            profilerFiller.push("targetSelector");
            this.targetSelector.tick();
            profilerFiller.pop();
            profilerFiller.push("goalSelector");
            this.goalSelector.tick();
            profilerFiller.pop();
        }

        profilerFiller.push("navigation");
        this.navigation.tick();
        profilerFiller.pop();
        profilerFiller.push("mob tick");
        this.customClientAiStep((ClientLevel) this.level());
        profilerFiller.pop();
        profilerFiller.push("controls");
        profilerFiller.push("move");
        this.moveControl.tick();
        profilerFiller.popPush("look");
        this.lookControl.tick();
        profilerFiller.popPush("jump");
        this.jumpControl.tick();
        profilerFiller.pop();
        profilerFiller.pop();
    }

    protected void customClientAiStep(ClientLevel clientLevel) {
        ProfilerFiller profilerFiller = Profiler.get();
        profilerFiller.push("clientEntityBrain");
        ClientBrain<ClientEntity> brain = (ClientBrain<ClientEntity>)this.getBrain();
        brain.tick(clientLevel, this);
        profilerFiller.pop();
    }

    protected ClientBrain.@NotNull Provider<ClientEntity> clientBrainProvider() {
        return ClientBrain.clientProvider(MEMORY_TYPES, SENSOR_TYPES);
    }

    @Override
    public @NotNull ClientBrain<? extends ClientEntity> getBrain() {
        return this.brain;
    }

    @Override
    protected @NotNull ClientBrain<? extends ClientEntity> makeBrain(Dynamic<?> dynamic) {
        ClientBrain<? extends ClientEntity> brain = this.clientBrainProvider().makeBrain(dynamic);
        this.registerBrainGoals(brain);
        return brain;
    }

    protected void registerBrainGoals(ClientBrain<? extends ClientEntity> brain) {
        brain.setSchedule(Schedule.EMPTY);

        brain.addActivity(
                Activity.IDLE,
                ImmutableList.of(
                        Pair.of(0, new ClientRunOne<>(
                                ImmutableMap.of(
                                        MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                                        MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.REGISTERED,
                                        MemoryModuleType.GAZE_COOLDOWN_TICKS, MemoryStatus.REGISTERED
                                ),
                                ImmutableList.of(
                                        Pair.of(new ClientDoNothing(30, 60), 1),
                                        Pair.of(ClientSetEntityLookTarget.create(4.0F), 1),
                                        Pair.of(new ClientRandomLookAround(BiasedToBottomInt.of(100, 200), 60, 30, 90), 1),
                                        Pair.of(new ClientLookAtTargetSink(30, 60), 1)
                                )
                        )),
                        Pair.of(2, new ClientRunOne<>(
                                ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED),
                                ImmutableList.of(
                                        Pair.of(new ClientDoNothing(60, 120), 1),
                                        Pair.of(ClientRandomStroll.stroll(0.75f), 1),
                                        Pair.of(new ClientJumpOnSpot(), 1),
                                        Pair.of(new ClientMoveToTargetSink(), 1)
                                )
                        )),
                        Pair.of(1, new ClientFollowTargetSink(0.9F))
                )
        );

        brain.addActivity(
                Activity.CORE,
                ImmutableList.of(
                        Pair.of(0, new ClientSwim<>(0.8F))
                )
        );
        brain.setCoreActivities(ImmutableSet.of(Activity.CORE));
        brain.setDefaultActivity(Activity.IDLE);
        brain.setActiveActivityIfPossible(Activity.IDLE);
    }

    public void setFollowTargetEntity(@Nullable Entity followTargetEntity) {
        this.followTargetEntity = followTargetEntity;
    }

    public @Nullable Entity getFollowTargetEntity() {
        return followTargetEntity;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5);
    }
}
