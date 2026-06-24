package com.java.beipuo.ae2virus.infection;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import com.java.beipuo.ae2virus.Ae2virus;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class T2VirusTargets {
    private static final String FUSION_PREFIX = "fusion/";

    private T2VirusTargets() {
    }

    public static ResourceLocation fusionTargetId(List<AEKey> targets) {
        List<String> ids = new ArrayList<>();
        for (AEKey target : targets) {
            ids.add(target.getId().toString());
        }
        ids.sort(Comparator.naturalOrder());
        int hash = ids.hashCode();
        return ResourceLocation.fromNamespaceAndPath(Ae2virus.MODID, FUSION_PREFIX + Integer.toHexString(hash));
    }

    public static List<ResourceLocation> itemTagIds(AEKey key) {
        if (!(key instanceof AEItemKey itemKey)) {
            return List.of();
        }

        return itemKey.getItem().builtInRegistryHolder().tags()
                .map(TagKey::location)
                .filter(T2VirusTargets::isSpecializedTagAllowed)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
    }

    public static boolean hasTag(AEKey key, ResourceLocation tagId) {
        if (!(key instanceof AEItemKey itemKey)) {
            return false;
        }

        for (TagKey<Item> tag : itemKey.getItem().builtInRegistryHolder().tags().toList()) {
            if (tag.location().equals(tagId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isSpecializedTagAllowed(ResourceLocation tagId) {
        String namespace = tagId.getNamespace();
        String path = tagId.getPath();
        return ("c".equals(namespace) || "forge".equals(namespace))
                && (path.equals("ingots") || path.startsWith("ingots/"));
    }
}
