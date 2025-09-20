package com.reijin.debugstickmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.ArrayList;
import java.util.List;

@Mod(DebugStickMod.MODID)
public class DebugStickMod {
    public static final String MODID = "debugstickmod";

    public DebugStickMod() {
        NeoForge.EVENT_BUS.addListener(Events::onRightClick);
        NeoForge.EVENT_BUS.addListener(Events::onLeftClick);
    }

    public static class Events {
        private static final String TAG_PROP_INDEX = "sds:prop_index";
        private static final String TAG_VALUE_INDEX = "sds:value_index";

        @SubscribeEvent
        public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
            Player player = event.getEntity();
            Level level = event.getLevel();
            ItemStack stack = player.getMainHandItem();

            if (stack.getItem() != Items.DEBUG_STICK) return;

            if (level.isClientSide) {
                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);
                return;
            }

            BlockPos pos = event.getPos();
            BlockState state = level.getBlockState(pos);
            List<Property<?>> props = new ArrayList<>(state.getProperties());

            if (props.isEmpty()) {
                player.displayClientMessage(Component.literal("Нет свойств"), true);
                event.setCanceled(true);
                return;
            }

            CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = data.copyTag();

            int propIndex = tag.getInt(TAG_PROP_INDEX);

            propIndex = Math.floorMod(propIndex, props.size());

            Property<?> prop = props.get(propIndex);
            List<?> values = new ArrayList<>(prop.getPossibleValues());

            if (values.isEmpty()) {
                player.displayClientMessage(Component.literal("Нет значений для свойства"), true);
                event.setCanceled(true);
                return;
            }

            Object currentValue = state.getValue(prop);
            int currentIndex = values.indexOf(currentValue);
            if (currentIndex < 0) currentIndex = 0;

            int valueIndex;
            if (player.isCrouching()) {
                valueIndex = Math.floorMod(currentIndex - 1, values.size());
            } else {
                valueIndex = (currentIndex + 1) % values.size();
            }

            Object chosen = values.get(valueIndex);

            applyProperty(state, level, pos, prop, chosen, player);

            tag.putInt(TAG_PROP_INDEX, propIndex);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        }

        @SubscribeEvent
        public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
            Player player = event.getEntity();
            Level level = event.getLevel();

            if (level.isClientSide) {
                event.setCanceled(true);
                return;
            }

            ItemStack stack = player.getMainHandItem();

            if (stack.getItem() != Items.DEBUG_STICK) return;

            BlockState state = level.getBlockState(event.getPos());
            List<Property<?>> props = new ArrayList<>(state.getProperties());
            if (props.isEmpty()) {
                player.displayClientMessage(Component.literal("Нет свойств"), true);
                event.setCanceled(true);
                return;
            }

            CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag tag = data.copyTag();

            int propIndex = tag.getInt(TAG_PROP_INDEX);

            propIndex = Math.floorMod(propIndex, props.size());

            if (player.isCrouching()) {
                propIndex = Math.floorMod(propIndex - 1, props.size());
            } else {
                propIndex = (propIndex + 1) % props.size();
            }

            Property<?> prop = props.get(propIndex);
            List<?> values = new ArrayList<>(prop.getPossibleValues());
            Object currentValue = state.getValue(prop);
            int currentIndex = values.indexOf(currentValue);
            if (currentIndex < 0) currentIndex = 0;

            tag.putInt(TAG_PROP_INDEX, propIndex);
            tag.putInt(TAG_VALUE_INDEX, currentIndex);

            player.displayClientMessage(Component.literal("Выбрано: " + prop.getName()), true);

            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

            event.setCanceled(true);
        }
    }

    private static <T extends Comparable<T>> void applyProperty(
            BlockState state,
            Level level,
            BlockPos pos,
            Property<T> prop,
            Object value,
            Player player
    ) {
        @SuppressWarnings("unchecked")
        T chosen = (T) value;
        BlockState newState = state.setValue(prop, chosen);
        level.setBlock(pos, newState, 3);
        String valueName = prop.getName(chosen);
        player.displayClientMessage(Component.literal(prop.getName() + " -> " + valueName), true);
    }

}
