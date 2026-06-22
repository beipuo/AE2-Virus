package com.java.beipuo.ae2virus;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(Ae2virus.MODID)
public class Ae2virus {
    public static final String MODID = "ae2virus";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Ae2virus(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("AE2 Virus loaded.");
    }
}
