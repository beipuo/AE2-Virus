package com.java.beipuo.ae2virus.client.gui;

import appeng.api.stacks.AmountFormat;
import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.infection.T2VirusKind;
import com.java.beipuo.ae2virus.network.packet.SyncVirusInfoPacket;
import com.java.beipuo.ae2virus.registry.AVItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class VirusTerminalScreen extends Screen {
    private static final ResourceLocation TERMINAL_TEXTURE = ResourceLocation.fromNamespaceAndPath("ae2",
            "textures/guis/terminal.png");
    private static final int IMAGE_WIDTH = 195;
    private static final int HEADER_HEIGHT = 17;
    private static final int ROW_HEIGHT = 18;
    private static final int BOTTOM_HEIGHT = 99;
    private static final int VISIBLE_ROWS = 5;
    private static final int LIST_LEFT = 7;
    private static final int LIST_TOP = HEADER_HEIGHT;
    private static final int LIST_WIDTH = 162;
    private static final ItemStack T1_VIRUS_ICON = AVItems.T1_BASIC_VIRUS.toStack();
    private static final ItemStack T2_FUSION_ICON = AVItems.T2_FUSION_VIRUS.toStack();
    private static final ItemStack T2_SPECIALIZED_ICON = AVItems.T2_SPECIALIZED_VIRUS.toStack();

    private final List<VirusRow> rows;
    private int scrollOffset;

    public VirusTerminalScreen(SyncVirusInfoPacket viruses) {
        super(Component.translatable("screen." + Ae2virus.MODID + ".virus_terminal"));
        this.rows = buildRows(viruses);
    }

    public static void open(SyncVirusInfoPacket viruses) {
        Minecraft.getInstance().setScreen(new VirusTerminalScreen(viruses));
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int imageHeight = imageHeight();
        int left = (this.width - IMAGE_WIDTH) / 2;
        int top = (this.height - imageHeight) / 2;
        renderTerminalBackground(guiGraphics, left, top);

        guiGraphics.drawString(this.font, this.title, left + 8, top + 6, 0x404040, false);
        guiGraphics.drawString(this.font,
                Component.translatable("screen.ae2virus.virus_terminal.count", this.rows.size()),
                left + 84, top + 6, 0x606060, false);

        if (this.rows.isEmpty()) {
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.ae2virus.virus_terminal.empty").withStyle(ChatFormatting.GRAY),
                    left + LIST_LEFT + 3, top + LIST_TOP + 6, 0x606060, false);
        } else {
            renderVirusRows(guiGraphics, left, top, mouseX, mouseY);
        }

        renderVirusTooltip(guiGraphics, left, top, mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int maxScroll = Math.max(0, this.rows.size() - VISIBLE_ROWS);
        this.scrollOffset = Math.max(0, Math.min(maxScroll, this.scrollOffset - (int) Math.signum(scrollY)));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    protected void renderBlurredBackground(float partialTick) {
    }

    private void renderTerminalBackground(GuiGraphics guiGraphics, int left, int top) {
        guiGraphics.blit(TERMINAL_TEXTURE, left, top, 0, 0, IMAGE_WIDTH, HEADER_HEIGHT);
        int rowTop = top + HEADER_HEIGHT;
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int srcY = row == 0 ? 17 : row == VISIBLE_ROWS - 1 ? 53 : 35;
            guiGraphics.blit(TERMINAL_TEXTURE, left, rowTop + row * ROW_HEIGHT, 0, srcY, IMAGE_WIDTH, ROW_HEIGHT);
        }
        guiGraphics.blit(TERMINAL_TEXTURE, left, rowTop + VISIBLE_ROWS * ROW_HEIGHT, 0, 71, IMAGE_WIDTH, BOTTOM_HEIGHT);
    }

    private void renderVirusRows(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        List<VirusRow> visible = visibleViruses();
        for (int i = 0; i < visible.size(); i++) {
            VirusRow virus = visible.get(i);
            int rowLeft = left + LIST_LEFT;
            int rowTop = top + LIST_TOP + i * ROW_HEIGHT;
            if (isMouseOverRow(rowLeft, rowTop, mouseX, mouseY)) {
                guiGraphics.fill(rowLeft, rowTop, rowLeft + LIST_WIDTH, rowTop + ROW_HEIGHT, 0x55FFFFFF);
            }

            guiGraphics.renderItem(virus.icon(), rowLeft + 1, rowTop + 1);
            guiGraphics.drawString(this.font,
                    virus.typeName(),
                    rowLeft + 21, rowTop + 2, 0x404040, false);
            guiGraphics.drawString(this.font,
                    virus.stats(),
                    rowLeft + 21, rowTop + 11, 0x606060, false);
        }
    }

    private void renderVirusTooltip(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        VirusRow hovered = hoveredVirus(left, top, mouseX, mouseY);
        if (hovered == null) {
            return;
        }

        guiGraphics.renderComponentTooltip(this.font, hovered.tooltip(), mouseX, mouseY);
    }

    private VirusRow hoveredVirus(int left, int top, int mouseX, int mouseY) {
        int rowLeft = left + LIST_LEFT;
        int rowTop = top + LIST_TOP;
        if (mouseX < rowLeft || mouseX >= rowLeft + LIST_WIDTH || mouseY < rowTop) {
            return null;
        }

        int row = (mouseY - rowTop) / ROW_HEIGHT;
        List<VirusRow> visible = visibleViruses();
        if (row < 0 || row >= visible.size()) {
            return null;
        }
        return visible.get(row);
    }

    private List<VirusRow> visibleViruses() {
        int end = Math.min(this.rows.size(), this.scrollOffset + VISIBLE_ROWS);
        return new ArrayList<>(this.rows.subList(this.scrollOffset, end));
    }

    private static boolean isMouseOverRow(int rowLeft, int rowTop, int mouseX, int mouseY) {
        return mouseX >= rowLeft && mouseX < rowLeft + LIST_WIDTH && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
    }

    private static int imageHeight() {
        return HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + BOTTOM_HEIGHT;
    }

    private static List<VirusRow> buildRows(SyncVirusInfoPacket packet) {
        List<VirusRow> rows = new ArrayList<>();
        for (SyncVirusInfoPacket.T1VirusInfo virus : packet.t1Viruses()) {
            rows.add(t1Row(virus));
        }
        for (SyncVirusInfoPacket.T2VirusInfo virus : packet.t2Viruses()) {
            rows.add(t2Row(virus));
        }
        return List.copyOf(rows);
    }

    private static VirusRow t1Row(SyncVirusInfoPacket.T1VirusInfo virus) {
        Component type = Component.translatable("tooltip.ae2virus.virus_terminal.type_t1");
        Component stats = Component.translatable("screen.ae2virus.virus_terminal.t1_stats",
                virus.level(),
                virus.target().formatAmount(virus.blockedAmount(), AmountFormat.FULL),
                virus.experience());
        List<Component> tooltip = List.of(
                type.copy().withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.ae2virus.virus_terminal.infected_item",
                        virus.target().getDisplayName())
                        .withStyle(ChatFormatting.WHITE),
                Component.translatable("tooltip.ae2virus.virus_terminal.level", virus.level())
                        .withStyle(ChatFormatting.AQUA),
                Component.translatable("tooltip.ae2virus.virus_terminal.blocked",
                        virus.target().formatAmount(virus.blockedAmount(), AmountFormat.FULL))
                        .withStyle(ChatFormatting.RED),
                Component.translatable("tooltip.ae2virus.virus_terminal.experience", virus.experience())
                        .withStyle(ChatFormatting.YELLOW),
                Component.translatable("tooltip.ae2virus.virus_terminal.target_id", virus.target().getId().toString())
                        .withStyle(ChatFormatting.DARK_GRAY));
        return new VirusRow(T1_VIRUS_ICON, type, stats, tooltip);
    }

    private static VirusRow t2Row(SyncVirusInfoPacket.T2VirusInfo virus) {
        Component type = Component.translatable(typeKey(virus.kind()));
        ItemStack icon = virus.kind() == T2VirusKind.SPECIALIZED ? T2_SPECIALIZED_ICON : T2_FUSION_ICON;
        Component stats = Component.translatable("screen.ae2virus.virus_terminal.t2_stats",
                virus.level(),
                virus.targets().size(),
                virus.totalBlockedAmount(),
                virus.experience());
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(type.copy().withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.t2_target", virus.targetId().toString())
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.level", virus.level())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.t2_target_count", virus.targets().size())
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.blocked", virus.totalBlockedAmount())
                .withStyle(ChatFormatting.RED));
        tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.experience", virus.experience())
                .withStyle(ChatFormatting.YELLOW));
        int shown = 0;
        for (SyncVirusInfoPacket.T2TargetInfo target : virus.targets()) {
            if (shown >= 3) {
                tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.more_targets",
                        virus.targets().size() - shown).withStyle(ChatFormatting.DARK_GRAY));
                break;
            }
            tooltip.add(Component.translatable("tooltip.ae2virus.virus_terminal.t2_infected_item",
                    target.target().getDisplayName(), target.blockedAmount()).withStyle(ChatFormatting.DARK_GRAY));
            shown++;
        }
        return new VirusRow(icon, type, stats, List.copyOf(tooltip));
    }

    private static String typeKey(T2VirusKind kind) {
        return switch (kind) {
            case FUSION -> "tooltip.ae2virus.virus_terminal.type_t2_fusion";
            case SPECIALIZED -> "tooltip.ae2virus.virus_terminal.type_t2_specialized";
            case SPECIAL_RESOURCE -> "tooltip.ae2virus.virus_terminal.type_t2_special";
        };
    }

    private record VirusRow(ItemStack icon, Component typeName, Component stats, List<Component> tooltip) {
    }
}
