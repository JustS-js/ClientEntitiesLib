package justs_js.cel.client.api.sensor;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.sensing.*;

import java.util.function.Supplier;

public class ClientSensorType<U extends ClientSensor<?>> extends SensorType<U> {
    public static final ClientSensorType<ClientNearestLivingEntitySensor<LivingEntity>> NEAREST_LIVING_ENTITIES = register("cl_nearest_living_entities", ClientNearestLivingEntitySensor::new);
    public static final ClientSensorType<ClientPlayerSensor> NEAREST_PLAYERS = register("cl_nearest_players", ClientPlayerSensor::new);

    public ClientSensorType(Supplier supplier) {
        super(supplier);
    }

    private static <U extends ClientSensor<?>> ClientSensorType<U> register(String string, Supplier<U> supplier) {
        return Registry.register(BuiltInRegistries.SENSOR_TYPE, Identifier.withDefaultNamespace(string), new ClientSensorType(supplier));
    }
}
