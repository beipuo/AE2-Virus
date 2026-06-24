package com.java.beipuo.ae2virus.part;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractDisplayPart;
import appeng.parts.reporting.ItemTerminalPart;
import appeng.util.InteractionUtil;
import com.java.beipuo.ae2virus.infection.IVirusNetworkService;
import com.java.beipuo.ae2virus.network.packet.OpenVirusTerminalPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public class VirusTerminalPart extends AbstractDisplayPart {
    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE,
            ItemTerminalPart.MODEL_OFF,
            MODEL_STATUS_OFF);
    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE,
            ItemTerminalPart.MODEL_ON,
            MODEL_STATUS_ON);
    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
            ItemTerminalPart.MODEL_ON,
            MODEL_STATUS_HAS_CHANNEL);

    public VirusTerminalPart(IPartItem<?> partItem) {
        super(partItem, true);
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (InteractionUtil.canWrenchRotate(player.getInventory().getSelected())) {
            return super.onUseWithoutItem(player, pos);
        }

        if (isClientSide()) {
            return true;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            getMainNode().ifPresent(grid -> {
                IVirusNetworkService service = grid.getService(IVirusNetworkService.class);
                if (service != null) {
                    PacketDistributor.sendToPlayer(serverPlayer,
                            OpenVirusTerminalPacket.fromStates(service.t1Viruses(), service.t2Viruses()));
                }
            });
        }
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }
}
