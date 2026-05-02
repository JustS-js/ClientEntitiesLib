package justs_js.cel.client.api.sensor;

import justs_js.cel.CELModLib;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.sensing.Sensor;
import org.jetbrains.annotations.Nullable;

public abstract class ClientSensor<E extends LivingEntity> extends Sensor<E> {
    private final int scanRate;
    private long timeToTick;

    @Override
    protected void doTick(ServerLevel serverLevel, E livingEntity) {}

    public ClientSensor(int i) {
        this.scanRate = i;
    }

    public void randomlyDelayStart(final RandomSource randomSource) {
        this.timeToTick = (long)randomSource.nextInt(this.scanRate);
    }

    public ClientSensor() {
        this(20);
    }

    public final void tick(ClientLevel clientLevel, E livingEntity) {
        if (--this.timeToTick <= 0L) {
            this.timeToTick = (long)this.scanRate;
            this.updateTargetingConditionRanges(livingEntity);
            this.doTick(clientLevel, livingEntity);
        }
    }

    protected void doTick(ClientLevel serverLevel, E livingEntity) {}

    public static boolean isEntityTargetable(ClientLevel level, LivingEntity livingEntity, LivingEntity livingEntity2) {
        return livingEntity.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, livingEntity2)
                ? test(false, false, level, livingEntity, livingEntity2)
                : test(true, false, level, livingEntity, livingEntity2);
    }

    public static boolean isEntityAttackable(ClientLevel level, LivingEntity livingEntity, LivingEntity livingEntity2) {
        return livingEntity.getBrain().isMemoryValue(MemoryModuleType.ATTACK_TARGET, livingEntity2)
                ? test(false, true, level, livingEntity, livingEntity2)
                : test(true, true, level, livingEntity, livingEntity2);
    }

    public static boolean test(boolean testInvisible, boolean isCombat, ClientLevel clientLevel, @Nullable LivingEntity livingEntity, LivingEntity livingEntity2) {
        if (livingEntity == livingEntity2) {
            return false;
        } else if (!livingEntity2.canBeSeenByAnyone()) {
            return false;
        } else {
            if (livingEntity == null) {
                return !isCombat || (livingEntity2.canBeSeenAsEnemy() && clientLevel.getDifficulty() != Difficulty.PEACEFUL);
            } else {
                if (isCombat && (!livingEntity.canAttack(livingEntity2) || !livingEntity.canAttack(livingEntity2) || livingEntity.isAlliedTo(livingEntity2))) {
                    return false;
                }

                double d = testInvisible ? livingEntity2.getVisibilityPercent(livingEntity) : 1.0;
                double e = Math.max(16 * d, 2.0);
                double f = livingEntity.distanceToSqr(livingEntity2.getX(), livingEntity2.getY(), livingEntity2.getZ());
                if (f > e * e) {
                    return false;
                }
                if (livingEntity instanceof Mob mob) {
                    return mob.getSensing().hasLineOfSight(livingEntity2);
                }
            }
            return true;
        }
    }
}
