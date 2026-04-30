package justs_js.cel.client.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import justs_js.cel.client.api.behaviour.ClientBehaviorControl;
import justs_js.cel.client.api.sensor.ClientSensor;
import justs_js.cel.client.api.sensor.ClientSensorType;
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

    private final Map<ClientSensorType<? extends ClientSensor<? super ClientEntity>>, ClientSensor<? super ClientEntity>> sensors = Maps.newLinkedHashMap();

    public void tick(ClientLevel clientLevel, ClientEntity livingEntity) {
        this.forgetOutdatedMemories();
        this.tickSensors(clientLevel, livingEntity);
        this.startEachNonRunningBehavior(clientLevel, livingEntity);
        this.tickEachRunningBehavior(clientLevel, livingEntity);
    }

    private void tickEachRunningBehavior(ClientLevel clientLevel, ClientEntity livingEntity) {
        long l = clientLevel.getGameTime();
        Iterator var5 = this.getRunningBehaviors().iterator();

        while(var5.hasNext()) {
            BehaviorControl<? super ClientEntity> behaviorControl = (BehaviorControl)var5.next();
            ((ClientBehaviorControl<? super ClientEntity>)behaviorControl).tickOrStop(clientLevel, livingEntity, l);
        }
    }

    private void tickSensors(ClientLevel clientLevel, ClientEntity livingEntity) {
        for (ClientSensor<? super ClientEntity> sensor : this.sensors.values()) {
            sensor.tick(clientLevel, livingEntity);
        }
    }

    private void startEachNonRunningBehavior(ClientLevel clientLevel, ClientEntity livingEntity) {
        long l = clientLevel.getGameTime();
        Iterator var5 = this.availableBehaviorsByPriority.values().iterator();

        label34:
        while(var5.hasNext()) {
            Map<Activity, Set<BehaviorControl<? super ClientEntity>>> map = (Map)var5.next();
            Iterator var7 = map.entrySet().iterator();

            while(true) {
                Map.Entry entry;
                Activity activity;
                do {
                    if (!var7.hasNext()) {
                        continue label34;
                    }

                    entry = (Map.Entry)var7.next();
                    activity = (Activity)entry.getKey();
                } while(!this.getActiveActivities().contains(activity));

                Set<BehaviorControl<? super ClientEntity>> set = (Set)entry.getValue();
                Iterator var11 = set.iterator();

                while(var11.hasNext()) {
                    BehaviorControl<? super ClientEntity> behaviorControl = (BehaviorControl)var11.next();
                    if (behaviorControl.getStatus() == Behavior.Status.STOPPED) {
                        ((ClientBehaviorControl<? super ClientEntity>)behaviorControl).tryStart(clientLevel, livingEntity, l);
                    }
                }
            }
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
