package com.java.beipuo.ae2virus.item;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import com.java.beipuo.ae2virus.infection.IVirusNetworkService;
import com.java.beipuo.ae2virus.infection.VirusClass;
import com.java.beipuo.ae2virus.network.packet.SyncInfectedKeysPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.network.PacketDistributor;

public class DebugVirusItem extends Item {
    private final VirusClass virusClass;

    public DebugVirusItem(VirusClass virusClass, Properties properties) {
        super(properties);
        this.virusClass = virusClass;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer player)) {
            return InteractionResult.PASS;
        }

        IInWorldGridNodeHost host = GridHelper.getNodeHost(context.getLevel(), context.getClickedPos());
        if (host == null) {
            player.displayClientMessage(Component.translatable("message.ae2virus.debug_virus.no_grid")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        IGridNode node = findNode(host, context.getClickedFace());
        if (node == null || node.getGrid() == null) {
            player.displayClientMessage(Component.translatable("message.ae2virus.debug_virus.no_grid")
                    .withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        IVirusNetworkService service = node.getGrid().getService(IVirusNetworkService.class);
        int infected = service.debugAddVirusAndInfect(this.virusClass);
        PacketDistributor.sendToPlayer(player, new SyncInfectedKeysPacket(service.getInfectedKeys()));
        player.displayClientMessage(Component.translatable("message.ae2virus.debug_virus.infected",
                Component.translatable("virus_class.ae2virus." + this.virusClass.name().toLowerCase()), infected)
                .withStyle(ChatFormatting.GRAY), true);
        return InteractionResult.CONSUME;
    }

    private static IGridNode findNode(IInWorldGridNodeHost host, Direction clickedFace) {
        IGridNode node = host.getGridNode(clickedFace);
        if (node != null) {
            return node;
        }

        for (Direction direction : Direction.values()) {
            node = host.getGridNode(direction);
            if (node != null) {
                return node;
            }
        }
        return null;
    }
}
