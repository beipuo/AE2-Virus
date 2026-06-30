package com.java.beipuo.ae2virus.item;

import appeng.api.behaviors.ContainerItemStrategy;
import appeng.api.config.Actionable;
import appeng.api.stacks.GenericStack;
import com.java.beipuo.ae2virus.registry.AVItems;
import com.java.beipuo.ae2virus.storage.DataStreamKey;
import com.java.beipuo.ae2virus.storage.DataStreamKeyType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public final class DataStreamCapsuleStrategy implements ContainerItemStrategy<DataStreamKey, DataStreamCapsuleStrategy.Context> {
    public static final DataStreamCapsuleStrategy INSTANCE = new DataStreamCapsuleStrategy();

    private DataStreamCapsuleStrategy() {
    }

    @Override
    public @Nullable GenericStack getContainedStack(ItemStack stack) {
        return null;
    }

    @Override
    public @Nullable Context findCarriedContext(Player player, AbstractContainerMenu menu) {
        ItemStack stack = menu.getCarried();
        if (isCapsule(stack)) {
            return new CarriedContext(player, menu);
        }
        return null;
    }

    @Override
    public @Nullable Context findPlayerSlotContext(Player player, int slot) {
        ItemStack stack = player.getInventory().getItem(slot);
        if (isCapsule(stack)) {
            return new PlayerInvContext(player, slot);
        }
        return null;
    }

    @Override
    public long extract(Context context, DataStreamKey what, long amount, Actionable mode) {
        ItemStack stack = context.getStack();
        DataStreamKey contained = DataStreamCapsuleItem.getDataStream(stack, context.player().level().registryAccess());
        if (!what.equals(contained) || amount < DataStreamKeyType.AMOUNT_MB) {
            return 0L;
        }

        if (mode == Actionable.MODULATE) {
            DataStreamCapsuleItem.setDataStream(stack, context.player().level().registryAccess(), null);
            context.setStack(stack);
        }
        return DataStreamKeyType.AMOUNT_MB;
    }

    @Override
    public long insert(Context context, DataStreamKey what, long amount, Actionable mode) {
        ItemStack stack = context.getStack();
        if (DataStreamCapsuleItem.isFilled(stack, context.player().level().registryAccess())
                || amount < DataStreamKeyType.AMOUNT_MB) {
            return 0L;
        }

        if (mode == Actionable.MODULATE) {
            DataStreamCapsuleItem.setDataStream(stack, context.player().level().registryAccess(), what);
            context.setStack(stack);
        }
        return DataStreamKeyType.AMOUNT_MB;
    }

    @Override
    public void playFillSound(Player player, DataStreamKey what) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    @Override
    public void playEmptySound(Player player, DataStreamKey what) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.8F, 1.2F);
    }

    @Override
    public @Nullable GenericStack getExtractableContent(Context context) {
        DataStreamKey key = DataStreamCapsuleItem.getDataStream(
                context.getStack(),
                context.player().level().registryAccess());
        return key == null ? null : new GenericStack(key, DataStreamKeyType.AMOUNT_MB);
    }

    private static boolean isCapsule(ItemStack stack) {
        return stack.is(AVItems.DATA_STREAM_CAPSULE.get());
    }

    public interface Context {
        Player player();

        ItemStack getStack();

        void setStack(ItemStack stack);
    }

    private record CarriedContext(Player player, AbstractContainerMenu menu) implements Context {
        @Override
        public ItemStack getStack() {
            return this.menu.getCarried();
        }

        @Override
        public void setStack(ItemStack stack) {
            this.menu.setCarried(stack);
        }
    }

    private record PlayerInvContext(Player player, int slot) implements Context {
        @Override
        public ItemStack getStack() {
            return this.player.getInventory().getItem(this.slot);
        }

        @Override
        public void setStack(ItemStack stack) {
            this.player.getInventory().setItem(this.slot, stack);
        }
    }
}
