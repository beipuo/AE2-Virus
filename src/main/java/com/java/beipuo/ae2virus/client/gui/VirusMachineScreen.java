package com.java.beipuo.ae2virus.client.gui;

import com.java.beipuo.ae2virus.machine.VirusMachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class VirusMachineScreen extends AbstractContainerScreen<VirusMachineMenu> {
    public VirusMachineScreen(VirusMachineMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = 73;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF202329);
        graphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF30343C);

        drawSlot(graphics, x + 17, y + 43, 0xFF1B3440);
        drawSlot(graphics, x + 44, y + 24, 0xFF2C2F36);
        drawSlot(graphics, x + 62, y + 24, 0xFF2C2F36);
        drawSlot(graphics, x + 80, y + 24, 0xFF2C2F36);
        drawSlot(graphics, x + 134, y + 43, 0xFF3B3322);

        graphics.fill(x + 108, y + 24, x + 116, y + 64, 0xFF17191D);
        int progressHeight = this.menu.maxProgress() <= 0 ? 0 : this.menu.progress() * 38 / this.menu.maxProgress();
        graphics.fill(x + 109, y + 63 - progressHeight, x + 115, y + 63, 0xFF67D1C8);

        graphics.fill(x + 124, y + 24, x + 128, y + 64, 0xFF17191D);
        int energyHeight = this.menu.maxEnergy() <= 0 ? 0 : this.menu.energy() * 38 / this.menu.maxEnergy();
        graphics.fill(x + 125, y + 63 - energyHeight, x + 127, y + 63, 0xFFF0C95B);

        graphics.drawString(this.font, Component.literal("DS"), x + 19, y + 31, 0xFF9EEAE4, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    private static void drawSlot(GuiGraphics graphics, int x, int y, int fill) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF0F1115);
        graphics.fill(x, y, x + 16, y + 16, fill);
    }
}
