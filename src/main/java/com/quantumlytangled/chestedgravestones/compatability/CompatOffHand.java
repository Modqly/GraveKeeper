package com.quantumlytangled.chestedgravestones.compatability;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import com.quantumlytangled.chestedgravestones.core.InventoryType;

public class CompatOffHand implements ICompatInventory {

  private static final CompatOffHand INSTANCE = new CompatOffHand();

  public static CompatOffHand getInstance() {
    return INSTANCE;
  }

  @Override
  public InventoryType getType() {
    return InventoryType.OFFHAND;
  }

  @Override
  public NonNullList<ItemStack> getAllContents(@Nonnull final EntityPlayerMP player) {
    return player.inventory.offHandInventory;
  }

  @Override
  public void removeItem(@Nonnull final EntityPlayerMP player, final int slot) {
    player.inventory.offHandInventory.set(slot, ItemStack.EMPTY);
  }

  @Override
  public ItemStack setItemReturnOverflow(@Nonnull final EntityPlayerMP player, final int slot, @Nonnull final ItemStack itemStack) {
    if (player.inventory.offHandInventory.get(slot).isEmpty()) {
      player.inventory.offHandInventory.set(slot, itemStack);
      return ItemStack.EMPTY;
    }
    return itemStack;
  }

  @Override
  public boolean isSlotEmpty(@Nonnull final EntityPlayerMP player, final int slot) {
    return player.inventory.offHandInventory.get(slot).isEmpty();
  }
}
