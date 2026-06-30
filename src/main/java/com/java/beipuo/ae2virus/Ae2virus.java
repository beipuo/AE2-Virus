package com.java.beipuo.ae2virus;

import appeng.api.stacks.AEKeyType;
import com.java.beipuo.ae2virus.infection.AVGridServices;
import com.java.beipuo.ae2virus.network.AVNetwork;
import com.java.beipuo.ae2virus.registry.AVRegistries;
import com.java.beipuo.ae2virus.storage.AVKeyTypes;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

@Mod(Ae2virus.MODID)
public class Ae2virus {
    public static final String MODID = "ae2virus";

    private static final Logger LOGGER = LogUtils.getLogger();

    public Ae2virus(IEventBus modEventBus, ModContainer modContainer) {
        AVGridServices.register();
        AVRegistries.register(modEventBus);
        modEventBus.addListener(this::register);
        modEventBus.addListener(AVNetwork::register);
        modEventBus.addListener(this::commonSetup);
    }

    private void register(RegisterEvent event) {
        if (event.getRegistryKey() == AEKeyType.REGISTRY_KEY) {
            AVKeyTypes.register();
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        AVKeyTypes.registerContainerStrategies();
        LOGGER.info("AE2 Virus loaded.");
    }
}
