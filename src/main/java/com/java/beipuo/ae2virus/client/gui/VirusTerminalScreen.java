package com.java.beipuo.ae2virus.client.gui;

import appeng.api.stacks.AmountFormat;
import com.java.beipuo.ae2virus.Ae2virus;
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

    private final List<SyncVirusInfoPacket.T1VirusInfo> viruses;
    private int scrollOffset;

    public VirusTerminalScreen(List<SyncVirusInfoPacket.T1VirusInfo> viruses) {
        super(Component.translatable("screen." + Ae2virus.MODID + ".virus_terminal"));
        this.viruses = List.copyOf(viruses);
    }

    public static void open(List<SyncVirusInfoPacket.T1VirusInfo> viruses) {
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
                Component.translatable("screen.ae2virus.virus_terminal.count", this.viruses.size()),
                left + 84, top + 6, 0x606060, false);

        if (this.viruses.isEmpty()) {
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
        int maxScroll = Math.max(0, this.viruses.size() - VISIBLE_ROWS);
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
        List<SyncVirusInfoPacket.T1VirusInfo> visible = visibleViruses();
        for (int i = 0; i < visible.size(); i++) {
            SyncVirusInfoPacket.T1VirusInfo virus = visible.get(i);
            int rowLeft = left + LIST_LEFT;
            int rowTop = top + LIST_TOP + i * ROW_HEIGHT;
            if (isMouseOverRow(rowLeft, rowTop, mouseX, mouseY)) {
                guiGraphics.fill(rowLeft, rowTop, rowLeft + LIST_WIDTH, rowTop + ROW_HEIGHT, 0x55FFFFFF);
            }

            guiGraphics.renderItem(T1_VIRUS_ICON, rowLeft + 1, rowTop + 1);
            guiGraphics.drawString(this.font,
                    Component.translatable("tooltip.ae2virus.virus_terminal.type_t1"),
                    rowLeft + 21, rowTop + 2, 0x404040, false);
            guiGraphics.drawString(this.font,
                    Component.translatable("screen.ae2virus.virus_terminal.t1_stats",
                            virus.level(),
                            virus.target().formatAmount(virus.blockedAmount(), AmountFormat.FULL),
                            virus.experience()),
                    rowLeft + 21, rowTop + 11, 0x606060, false);
        }
    }

    private void renderVirusTooltip(GuiGraphics guiGraphics, int left, int top, int mouseX, int mouseY) {
        SyncVirusInfoPacket.T1VirusInfo hovered = hoveredVirus(left, top, mouseX, mouseY);
        if (hovered == null) {
            return;
        }

        guiGraphics.renderComponentTooltip(this.font, List.of(
                Component.translatable("tooltip.ae2virus.virus_terminal.type_t1")
                        .withStyle(ChatFormatting.GRAY),
                Component.translatable("tooltip.ae2virus.virus_terminal.infected_item",
                        hovered.target().getDisplayName())
                        .withStyle(ChatFormatting.WHITE),
                Component.translatable("tooltip.ae2virus.virus_terminal.level", hovered.level())
                        .withStyle(ChatFormatting.AQUA),
                Component.translatable("tooltip.ae2virus.virus_terminal.blocked",
                        hovered.target().formatAmount(hovered.blockedAmount(), AmountFormat.FULL))
                        .withStyle(ChatFormatting.RED),
                Component.translatable("tooltip.ae2virus.virus_terminal.experience", hovered.experience())
                        .withStyle(ChatFormatting.YELLOW),
                Component.translatable("tooltip.ae2virus.virus_terminal.target_id", hovered.target().getId().toString())
                        .withStyle(ChatFormatting.DARK_GRAY)), mouseX, mouseY);
    }

    private SyncVirusInfoPacket.T1VirusInfo hoveredVirus(int left, int top, int mouseX, int mouseY) {
        int rowLeft = left + LIST_LEFT;
        int rowTop = top + LIST_TOP;
        if (mouseX < rowLeft || mouseX >= rowLeft + LIST_WIDTH || mouseY < rowTop) {
            return null;
        }

        int row = (mouseY - rowTop) / ROW_HEIGHT;
        List<SyncVirusInfoPacket.T1VirusInfo> visible = visibleViruses();
        if (row < 0 || row >= visible.size()) {
            return null;
        }
        return visible.get(row);
    }

    private List<SyncVirusInfoPacket.T1VirusInfo> visibleViruses() {
        int end = Math.min(this.viruses.size(), this.scrollOffset + VISIBLE_ROWS);
        return new ArrayList<>(this.viruses.subList(this.scrollOffset, end));
    }

    private static boolean isMouseOverRow(int rowLeft, int rowTop, int mouseX, int mouseY) {
        return mouseX >= rowLeft && mouseX < rowLeft + LIST_WIDTH && mouseY >= rowTop && mouseY < rowTop + ROW_HEIGHT;
    }

    private static int imageHeight() {
        return HEADER_HEIGHT + VISIBLE_ROWS * ROW_HEIGHT + BOTTOM_HEIGHT;
    }
}
