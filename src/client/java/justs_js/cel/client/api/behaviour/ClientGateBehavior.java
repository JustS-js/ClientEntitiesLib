package justs_js.cel.client.api.behaviour;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.ShufflingList;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ClientGateBehavior<E extends LivingEntity> implements ClientBehaviorControl<E> {
    private final Map<MemoryModuleType<?>, MemoryStatus> entryCondition;
    private final Set<MemoryModuleType<?>> exitErasedMemories;
    private final OrderPolicy orderPolicy;
    private final RunningPolicy runningPolicy;
    private final ShufflingList<ClientBehaviorControl<? super E>> behaviors = new ShufflingList<>();
    private Behavior.Status status;

    public ClientGateBehavior(Map<MemoryModuleType<?>, MemoryStatus> map, Set<MemoryModuleType<?>> set, OrderPolicy orderPolicy, RunningPolicy runningPolicy, List<Pair<? extends ClientBehaviorControl<? super E>, Integer>> list) {
        this.status = Behavior.Status.STOPPED;
        this.entryCondition = map;
        this.exitErasedMemories = set;
        this.orderPolicy = orderPolicy;
        this.runningPolicy = runningPolicy;
        list.forEach((pair) -> {
            this.behaviors.add(pair.getFirst(), pair.getSecond());
        });
    }

    public Behavior.Status getStatus() {
        return this.status;
    }

    @Override
    public Set<MemoryModuleType<?>> getRequiredMemories() {
        Set<MemoryModuleType<?>> memories = new HashSet(this.entryCondition.keySet());

        for(BehaviorControl<? super E> behavior : this.behaviors) {
            memories.addAll(behavior.getRequiredMemories());
        }

        return memories;
    }

    @Override
    public boolean tryStart(ServerLevel serverLevel, E livingEntity, long l) {return false;}

    @Override
    public void tickOrStop(ServerLevel serverLevel, E livingEntity, long l) {}

    @Override
    public void doStop(ServerLevel serverLevel, E livingEntity, long l) {}

    private boolean hasRequiredMemories(E livingEntity) {
        for(Map.Entry<MemoryModuleType<?>, MemoryStatus> entry : this.entryCondition.entrySet()) {
            MemoryModuleType<?> memoryType = entry.getKey();
            MemoryStatus requiredStatus = entry.getValue();
            if (!livingEntity.getBrain().checkMemory(memoryType, requiredStatus)) {
                return false;
            }
        }

        return true;
    }

    public final boolean tryStart(ClientLevel serverLevel, E livingEntity, long l) {
        if (this.hasRequiredMemories(livingEntity)) {
            this.status = Behavior.Status.RUNNING;
            this.orderPolicy.apply(this.behaviors);
            this.runningPolicy.apply(this.behaviors.stream(), serverLevel, livingEntity, l);
            return true;
        } else {
            return false;
        }
    }

    public final void tickOrStop(ClientLevel serverLevel, E livingEntity, long l) {
        this.behaviors.stream().filter((behaviorControl) -> {
            return behaviorControl.getStatus() == Behavior.Status.RUNNING;
        }).forEach((behaviorControl) -> {
            ((ClientBehaviorControl<E>)behaviorControl).tickOrStop(serverLevel, livingEntity, l);
        });
        if (this.behaviors.stream().noneMatch((behaviorControl) -> {
            return behaviorControl.getStatus() == Behavior.Status.RUNNING;
        })) {
            this.doStop(serverLevel, livingEntity, l);
        }

    }

    public final void doStop(ClientLevel serverLevel, E livingEntity, long l) {
        this.status = Behavior.Status.STOPPED;
        this.behaviors.stream().filter((behaviorControl) -> {
            return behaviorControl.getStatus() == Behavior.Status.RUNNING;
        }).forEach((behaviorControl) -> {
            ((ClientBehaviorControl<E>)behaviorControl).doStop(serverLevel, livingEntity, l);
        });
        Brain<?> var10001 = livingEntity.getBrain();
        Objects.requireNonNull(var10001);
        this.exitErasedMemories.forEach(var10001::eraseMemory);
    }

    public String debugString() {
        return this.getClass().getSimpleName();
    }

    public String toString() {
        Set<? extends BehaviorControl<? super E>> set = this.behaviors.stream().filter((behaviorControl) -> {
            return behaviorControl.getStatus() == Behavior.Status.RUNNING;
        }).collect(Collectors.toSet());
        String var10000 = this.getClass().getSimpleName();
        return "(" + var10000 + "): " + String.valueOf(set);
    }

    public static enum OrderPolicy {
        ORDERED((shufflingList) -> {
        }),
        SHUFFLED((shufflingList) -> {
            ((ShufflingList<?>)shufflingList).shuffle();
        });

        private final Consumer<ShufflingList<?>> consumer;

        private OrderPolicy(final Consumer consumer) {
            this.consumer = consumer;
        }

        public void apply(ShufflingList<?> shufflingList) {
            this.consumer.accept(shufflingList);
        }
    }

    public static enum RunningPolicy {
        RUN_ONE {
            public <E extends LivingEntity> void apply(Stream<ClientBehaviorControl<? super E>> stream, ClientLevel serverLevel, E livingEntity, long l) {
                stream.filter((behaviorControl) -> {
                    return behaviorControl.getStatus() == Behavior.Status.STOPPED;
                }).filter((behaviorControl) -> {
                    return behaviorControl.tryStart(serverLevel, livingEntity, l);
                }).findFirst();
            }
        },
        TRY_ALL {
            public <E extends LivingEntity> void apply(Stream<ClientBehaviorControl<? super E>> stream, ClientLevel serverLevel, E livingEntity, long l) {
                stream.filter((behaviorControl) -> {
                    return behaviorControl.getStatus() == Behavior.Status.STOPPED;
                }).forEach((behaviorControl) -> {
                    behaviorControl.tryStart(serverLevel, livingEntity, l);
                });
            }
        };

        RunningPolicy() {
        }

        public abstract <E extends LivingEntity> void apply(Stream<ClientBehaviorControl<? super E>> stream, ClientLevel serverLevel, E livingEntity, long l);
    }
}
