package justs_js.cel.client.api.behaviour;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class ClientDoNothing implements ClientBehaviorControl<LivingEntity> {
    protected final int minDuration;
    protected final int maxDuration;
    private Behavior.Status status;
    protected long endTimestamp;

    public ClientDoNothing(int i, int j) {
        this.status = Behavior.Status.STOPPED;
        this.minDuration = i;
        this.maxDuration = j;
    }

    public Set<MemoryModuleType<?>> getRequiredMemories() {
        return Set.of();
    }

    @Override
    public boolean tryStart(ClientLevel clientLevel, LivingEntity livingEntity, long l) {
        this.status = Behavior.Status.RUNNING;
        int i = this.minDuration + clientLevel.getRandom().nextInt(this.maxDuration + 1 - this.minDuration);
        this.endTimestamp = l + (long)i;
        return true;
    }

    @Override
    public void tickOrStop(ClientLevel clientLevel, LivingEntity livingEntity, long l) {
        if (l > this.endTimestamp) {
            this.doStop(clientLevel, livingEntity, l);
        }
    }

    @Override
    public void doStop(ClientLevel clientLevel, LivingEntity livingEntity, long l) {
        this.status = Behavior.Status.STOPPED;
    }

    @Override
    public Behavior.@NotNull Status getStatus() {
        return this.status;
    }

    @Override
    public boolean tryStart(ServerLevel serverLevel, LivingEntity livingEntity, long l) {return false;}

    @Override
    public void tickOrStop(ServerLevel serverLevel, LivingEntity livingEntity, long l) {}

    @Override
    public void doStop(ServerLevel serverLevel, LivingEntity livingEntity, long l) {}

    @Override
    public @NotNull String debugString() {
        return this.getClass().getSimpleName();
    }
}
