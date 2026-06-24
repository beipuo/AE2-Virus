package com.java.beipuo.ae2virus.item;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import com.java.beipuo.ae2virus.infection.IVirusNetworkService;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class VirusStimulatorItem extends Item {
    private static final int STIMULATION_DURATION_TICKS = 20 * 10;

    public VirusStimulatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        IGridNode node = findGridNode(context);
        if (node == null || node.getGrid() == null) {
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        Component.translatable("message.ae2virus.virus_stimulator.no_grid"), true);
            }
            return InteractionResult.FAIL;
        }

        IVirusNetworkService service = node.getGrid().getService(IVirusNetworkService.class);
        if (service == null) {
            return InteractionResult.FAIL;
        }

        service.stimulateViruses(STIMULATION_DURATION_TICKS);
        if (context.getPlayer() != null) {
            if (!context.getPlayer().getAbilities().instabuild) {
                ItemStack stack = context.getItemInHand();
                stack.shrink(1);
            }
            context.getPlayer().displayClientMessage(
                    Component.translatable("message.ae2virus.virus_stimulator.activated"), true);
        }
        return InteractionResult.SUCCESS;
    }

    private static IGridNode findGridNode(UseOnContext context) {
        var host = GridHelper.getNodeHost(context.getLevel(), context.getClickedPos());
        if (host == null) {
            return null;
        }

        IGridNode clickedSideNode = host.getGridNode(context.getClickedFace());
        if (clickedSideNode != null) {
            return clickedSideNode;
        }

        for (Direction direction : Direction.values()) {
            IGridNode node = host.getGridNode(direction);
            if (node != null) {
                return node;
            }
        }
        return null;
    }
}
