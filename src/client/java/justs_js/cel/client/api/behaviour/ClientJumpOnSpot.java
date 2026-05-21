package justs_js.cel.client.api.behaviour;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class ClientJumpOnSpot extends ClientBehavior<Mob> {
    protected static final int MIN_JUMPS = 1;
    protected static final int MAX_JUMPS = 8;
    protected static final int COOLDOWN_BETWEEN_JUMPS = 5;
    protected int remainingJumps;
    protected int remainingCooldownUntilNextJump;

    public ClientJumpOnSpot() {
        super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT));
    }

    @Override
    protected boolean checkExtraStartConditions(ClientLevel clientLevel, Mob mob) {
        return !clientLevel.collidesWithSuffocatingBlock(mob, mob.getBoundingBox().expandTowards(0, 1, 0));
    }

    @Override
    protected void start(ClientLevel clientLevel, Mob livingEntity, long l) {
        super.start(clientLevel, livingEntity, l);
        this.remainingJumps = MIN_JUMPS + clientLevel.getRandom().nextInt(MAX_JUMPS - MIN_JUMPS + 1);
        this.remainingCooldownUntilNextJump = 0;
    }

    @Override
    protected void stop(ClientLevel clientLevel, Mob livingEntity, long l) {
        super.stop(clientLevel, livingEntity, l);
        this.remainingJumps = 0;
        this.remainingCooldownUntilNextJump = 0;
    }

    @Override
    protected boolean timedOut(long l) {
        return false;
    }

    @Override
    protected void tick(ClientLevel clientLevel, Mob livingEntity, long l) {
        if (this.remainingCooldownUntilNextJump > 0) {
            --this.remainingCooldownUntilNextJump;
        } else {
            if (isOnGround(clientLevel, livingEntity)) {
                livingEntity.getJumpControl().jump();
                --this.remainingJumps;
                this.remainingCooldownUntilNextJump = COOLDOWN_BETWEEN_JUMPS;
            }
        }
    }

    @Override
    protected boolean canStillUse(ClientLevel clientLevel, Mob livingEntity, long l) {
        return !tiredOfJumping();
    }

    private boolean isOnGround(ClientLevel clientLevel, Mob livingEntity) {
        return clientLevel.collidesWithSuffocatingBlock(livingEntity, livingEntity.getBoundingBox().expandTowards(0,  - 1.0E-6, 0));
    }

    private boolean tiredOfJumping() {
        return this.remainingJumps <= 0;
    }
}
