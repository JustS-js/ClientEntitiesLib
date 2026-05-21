package justs_js.cel.client.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import justs_js.cel.client.api.behaviour.*;
import justs_js.cel.client.api.sensor.ClientSensor;
import justs_js.cel.client.api.sensor.ClientSensorType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.util.valueproviders.BiasedToBottomInt;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.schedule.Activity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public abstract class ClientEntity extends PathfinderMob {
    protected static ClientBrain.Provider BRAIN_PROVIDER;
    private static final ImmutableList<ClientSensorType<? extends ClientSensor<? super ClientEntity>>> SENSOR_TYPES =
            ImmutableList.of(
                    ClientSensorType.NEAREST_PLAYERS,
                    ClientSensorType.NEAREST_LIVING_ENTITIES
            );

    protected ClientBrain brain;

    public ClientEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.brain = this.makeBrain(Brain.Packed.EMPTY);
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
        ClientBrain brain = this.getBrain();
        brain.tick(clientLevel, this);
        profilerFiller.pop();
    }

    protected ClientBrain.@NotNull Provider clientBrainProvider() {
        if (BRAIN_PROVIDER == null) {
            BRAIN_PROVIDER = ClientBrain.clientProvider(
                    this.getSensors(),
                    this::getActivities
            );
        }
        return BRAIN_PROVIDER;
    }


    @Override
    public @NotNull ClientBrain getBrain() {
        return this.brain;
    }

    @Override
    protected @NotNull ClientBrain makeBrain(final ClientBrain.Packed packedBrain) {
        return this.clientBrainProvider().makeBrain(this, packedBrain);
    }

    /**
     * Override this to create your own sensors
     * */
    protected List<ClientSensorType<? extends ClientSensor<? super ClientEntity>>> getSensors() {
        return SENSOR_TYPES;
    }

    /**
     * Override this to create your own behaviors
     * */
    protected List<ActivityData<ClientEntity>> getActivities(ClientEntity entity) {
        return List.of(
                ActivityData.create(
                        Activity.CORE,
                        0,
                        ImmutableList.of(
                                new ClientSwim<>(0.8F),
                                new ClientLookAtTargetSink(30, 60),
                                new ClientMoveToTargetSink()
                        )
                ),
                ActivityData.create(
                        Activity.IDLE,
                        ImmutableList.of(
                                Pair.of(0, ClientSetEntityLookTarget.create(4.0F)),
                                Pair.of(0, new ClientAvoidTarget(0.8F, 1.33F, 8)),
                                Pair.of(1, new ClientRunOne<>(
                                        ImmutableMap.of(
                                                MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED,
                                                MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, MemoryStatus.REGISTERED,
                                                MemoryModuleType.GAZE_COOLDOWN_TICKS, MemoryStatus.REGISTERED
                                        ),
                                        ImmutableList.of(
                                                Pair.of(new ClientDoNothing(30, 60), 1),
                                                Pair.of(new ClientRandomLookAround(BiasedToBottomInt.of(100, 200), 60, 30, 90), 1),
                                                Pair.of(ClientRandomStroll.stroll(0.8f), 1),
                                                Pair.of(new ClientJumpOnSpot(), 1)
                                        )
                                ))
                        )
                )
        );
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MOVEMENT_SPEED, 0.5);
    }
}
