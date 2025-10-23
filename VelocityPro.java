package cn.xiaojie.mtf.module.impl.combat;

import cn.xiaojie.mtf.event.EventTarget;
import cn.xiaojie.mtf.module.Category;
import cn.xiaojie.mtf.module.Module;
import cn.xiaojie.mtf.value.impl.ModeValue;
import cn.xiaojie.mtf.value.impl.NumberValue;
import cn.xiaojie.mtf.event.packet.PacketEvent;
import cn.xiaojie.mtf.event.player.UpdateEvent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;

@ModuleInfo(name = "Velocity", category = Category.COMBAT)
public class VelocityPro extends Module {
    public final ModeValue mode = new ModeValue("Mode", "GrimReduce", "GrimReduce");
    public final ModeValue grimReduceMode = new ModeValue("GrimReduce Mode", "PreTick", "OneTime","PerTick");
    public final NumberValue attacks = new NumberValue("Attack Count", 2, 1, 5, 1);

    private Entity target;
    private boolean velocityInput = false;
    private int attackQueue = 0;

    @Override
    public void onDisable() {
        velocityInput = false;
        targetEntity = null;
        attackQueue = 0;
    }

    @EventTarget
    public void onPacket(PacketEvent event) {
        if (mc.level == null || mc.player == null) return;

        Packet<?> packet = event.getPacket();

        if (packet instanceof ClientboundDamageEventPacket damagePacket) {
            if (damagePacket.entityId() == mc.player.getId()) {
                velocityInput = true;
            }
        }

        if (packet instanceof ClientboundSetEntityMotionPacket velocityPacket) {
            if (velocityPacket.getId() != mc.player.getId()) {
                return;
            }

            velocityInput = true;
            targetEntity = KillAura.instance.getTarget();

            if (mode.is("GrimReduce")) {
                if (velocityInput) {
                    velocityInput = false;
                    attackQueue = attacks.getValue().intValue();
                }
            }
        }
    }

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.player == null) return;

        setSuffix(mode.getValue());

        if (mc.player.hurtTime == 0) {
            velocityInput = false;
        }

        if (mode.is("GrimReduce") && targetEntity != null && attackQueue > 0) {
            if (grimReduceMode.is("OneTime")) {
                for (; attackQueue >= 1; attackQueue--) {
                    mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(targetEntity, false));
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1, 0.6));
                    mc.player.setSprinting(false);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            } else if (grimReduceMode.is("PerTick")) {
                if (attackQueue >= 1) {
                    mc.getConnection().send(ServerboundInteractPacket.createAttackPacket(targetEntity, false));
                    mc.player.setDeltaMovement(mc.player.getDeltaMovement().multiply(0.6, 1, 0.6));
                    mc.player.setSprinting(false);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
                attackQueue--;
            }
        }
    }
}
