package com.java.beipuo.ae2virus.registry;

import com.java.beipuo.ae2virus.Ae2virus;
import com.java.beipuo.ae2virus.machine.VirusMachineBlockEntity;
import com.java.beipuo.ae2virus.machine.VirusMachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class AVMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Ae2virus.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<VirusMachineMenu>> VIRUS_MACHINE = MENUS.register(
            "virus_machine",
            () -> IMenuTypeExtension.create((id, inventory, data) -> {
                var pos = data.readBlockPos();
                if (inventory.player.level().getBlockEntity(pos) instanceof VirusMachineBlockEntity machine) {
                    return new VirusMachineMenu(id, inventory, machine);
                }
                throw new IllegalStateException("Missing virus machine at " + pos);
            }));

    private AVMenus() {
    }
}
