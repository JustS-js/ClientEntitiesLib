package justs_js.cel.client.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import justs_js.cel.CELModLib;
import justs_js.cel.client.api.behaviour.ClientBehaviorControl;
import justs_js.cel.client.api.sensor.ClientSensor;
import justs_js.cel.client.api.sensor.ClientSensorType;
import justs_js.cel.client.mixin.BrainSensorAccessor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.ActivityData;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.memory.MemoryMap;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import net.minecraft.world.entity.schedule.Activity;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ClientBrain extends Brain<ClientEntity> {
    protected ClientBrain(final Collection<? extends MemoryModuleType<?>> memoryTypes, final Collection<? extends SensorType<? extends Sensor<? super ClientEntity>>> sensorTypes, final List<ActivityData<ClientEntity>> activities, final MemoryMap memories, final RandomSource randomSource) {
        super(memoryTypes, sensorTypes, activities, memories, randomSource);
    }

    public void stopAll(ClientLevel clientLevel, ClientEntity livingEntity) {
        long l = clientLevel.getGameTime();
        Iterator var5 = this.getRunningBehaviors().iterator();

        while(var5.hasNext()) {
            ClientBehaviorControl<? super ClientEntity> behaviorControl = (ClientBehaviorControl<? super ClientEntity>) var5.next();
            behaviorControl.doStop(clientLevel, livingEntity, l);
        }
    }

    public static Provider clientProvider(final Collection<? extends ClientSensorType<? extends ClientSensor<? super ClientEntity>>> sensorTypes, final ActivitySupplier<ClientEntity> activities) {
        return new ClientBrain.Provider(ImmutableList.of(), sensorTypes, activities);
    }

    public void tick(ClientLevel clientLevel, ClientEntity livingEntity) {
        this.forgetOutdatedMemories();
        this.tickSensors(clientLevel, livingEntity);
        this.startEachNonRunningBehavior(clientLevel, livingEntity);
        this.tickEachRunningBehavior(clientLevel, livingEntity);
    }

    private void tickSensors(ClientLevel clientLevel, ClientEntity livingEntity) {
        Map<SensorType<? extends ClientSensor<? super ClientEntity>>, ClientSensor<? super ClientEntity>> sensors = ((BrainSensorAccessor)this).cel$getSensors();
        for (ClientSensor<? super ClientEntity> sensor : sensors.values()) {
            sensor.tick(clientLevel, livingEntity);
        }
    }

    private void startEachNonRunningBehavior(ClientLevel clientLevel, ClientEntity livingEntity) {
        long time = clientLevel.getGameTime();

        for (Map<Activity, Set<BehaviorControl<? super ClientEntity>>> map : this.availableBehaviorsByPriority.values()) {
            for (Map.Entry<Activity, Set<BehaviorControl<? super ClientEntity>>> behavioursForActivity : map.entrySet()) {
                Activity activity = behavioursForActivity.getKey();
                if (this.getActiveActivities().contains(activity)) {
                    for (BehaviorControl<? super ClientEntity> behavior : behavioursForActivity.getValue()) {
                        if (behavior.getStatus() == Behavior.Status.STOPPED) {
                            ((ClientBehaviorControl<? super ClientEntity>)behavior).tryStart(clientLevel, livingEntity, time);
                        }
                    }
                }
            }
        }
    }

    private void tickEachRunningBehavior(ClientLevel clientLevel, ClientEntity livingEntity) {
        long time = clientLevel.getGameTime();

        for(BehaviorControl<? super ClientEntity> behavior : this.getRunningBehaviors()) {
            ((ClientBehaviorControl<? super ClientEntity>)behavior).tickOrStop(clientLevel, livingEntity, time);
        }
    }

    public static final class Provider {
        private final Collection<? extends MemoryModuleType<?>> memoryTypes;
        private final Collection<? extends ClientSensorType<? extends ClientSensor<? super ClientEntity>>> sensorTypes;
        private final ActivitySupplier<ClientEntity> activities;

        Provider(final Collection<? extends MemoryModuleType<?>> memoryTypes, final Collection<? extends ClientSensorType<? extends ClientSensor<? super ClientEntity>>> sensorTypes, final ActivitySupplier<ClientEntity> activities) {
            this.memoryTypes = memoryTypes;
            this.sensorTypes = sensorTypes;
            this.activities = activities;
        }

        public @NotNull ClientBrain makeBrain(final ClientEntity body, final Packed packed) {
            List<ActivityData<ClientEntity>> activities = this.activities.createActivities(body);
            return new ClientBrain(this.memoryTypes, this.sensorTypes, activities, packed.memories(), body.getRandom());
        }
    }
}
