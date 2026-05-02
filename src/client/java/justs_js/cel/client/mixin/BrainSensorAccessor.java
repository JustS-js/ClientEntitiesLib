package justs_js.cel.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.sensing.Sensor;
import net.minecraft.world.entity.ai.sensing.SensorType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(Brain.class)
@Environment(EnvType.CLIENT)
public interface BrainSensorAccessor<E extends LivingEntity> {
    @Accessor("sensors")
    Map<SensorType<? extends Sensor<? super E>>, Sensor<? super E>> cel$getSensors();
}
