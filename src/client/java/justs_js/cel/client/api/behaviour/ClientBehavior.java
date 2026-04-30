package justs_js.cel.client.api.behaviour;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class ClientBehavior<E extends LivingEntity> implements ClientBehaviorControl<E> {
    public static final int DEFAULT_DURATION = 60;
    protected final Map<MemoryModuleType<?>, MemoryStatus> entryCondition;
    private Behavior.Status status;
    private long endTimestamp;
    private final int minDuration;
    private final int maxDuration;

    public ClientBehavior(Map<MemoryModuleType<?>, MemoryStatus> map) {
        this(map, DEFAULT_DURATION);
    }

    public ClientBehavior(Map<MemoryModuleType<?>, MemoryStatus> map, int i) {
        this(map, i, i);
    }

    public ClientBehavior(Map<MemoryModuleType<?>, MemoryStatus> map, int i, int j) {
        this.status = Behavior.Status.STOPPED;
        this.minDuration = i;
        this.maxDuration = j;
        this.entryCondition = map;
    }

    public Behavior.@NotNull Status getStatus() {
        return this.status;
    }

    public @NonNull Set<MemoryModuleType<?>> getRequiredMemories() {
        return this.entryCondition.keySet();
    }

    public final boolean tryStart(ClientLevel clientLevel, E livingEntity, long l) {
        if (this.hasRequiredMemories(livingEntity) && this.checkExtraStartConditions(clientLevel, livingEntity)) {
            this.status = Behavior.Status.RUNNING;
            int i = this.minDuration + clientLevel.getRandom().nextInt(this.maxDuration + 1 - this.minDuration);
            this.endTimestamp = l + (long)i;
            this.start(clientLevel, livingEntity, l);
            return true;
        } else {
            return false;
        }
    }

    protected void start(ClientLevel clientLevel, E livingEntity, long l) {
    }

    public final void tickOrStop(ClientLevel clientLevel, E livingEntity, long l) {
        if (!this.timedOut(l) && this.canStillUse(clientLevel, livingEntity, l)) {
            this.tick(clientLevel, livingEntity, l);
        } else {
            this.doStop(clientLevel, livingEntity, l);
        }

    }

    protected void tick(ClientLevel clientLevel, E livingEntity, long l) {
    }

    public final void doStop(ClientLevel clientLevel, E livingEntity, long l) {
        this.status = Behavior.Status.STOPPED;
        this.stop(clientLevel, livingEntity, l);
    }

    protected void stop(ClientLevel clientLevel, E livingEntity, long l) {
    }

    protected boolean canStillUse(ClientLevel clientLevel, E livingEntity, long l) {
        return false;
    }

    protected boolean timedOut(long l) {
        return l > this.endTimestamp;
    }

    protected boolean checkExtraStartConditions(ClientLevel clientLevel, E livingEntity) {
        return true;
    }

    public String debugString() {
        return this.getClass().getSimpleName();
    }

    protected boolean hasRequiredMemories(E livingEntity) {
        Iterator var2 = this.entryCondition.entrySet().iterator();

        MemoryModuleType memoryModuleType;
        MemoryStatus memoryStatus;
        do {
            if (!var2.hasNext()) {
                return true;
            }

            Map.Entry<MemoryModuleType<?>, MemoryStatus> entry = (Map.Entry)var2.next();
            memoryModuleType = entry.getKey();
            memoryStatus = entry.getValue();
        } while(livingEntity.getBrain().checkMemory(memoryModuleType, memoryStatus));

        return false;
    }

    public boolean tryStart(ServerLevel serverLevel, Mob livingEntity, long l) {return false;}

    public void tickOrStop(ServerLevel serverLevel, Mob livingEntity, long l) {}

    public void doStop(ServerLevel serverLevel, Mob livingEntity, long l) {}
}
