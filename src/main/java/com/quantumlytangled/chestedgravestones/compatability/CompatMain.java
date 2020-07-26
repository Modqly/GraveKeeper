package com.quantumlytangled.chestedgravestones.compatability;

import javax.annotation.Nonnull;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import com.quantumlytangled.chestedgravestones.core.InventoryType;

public class CompatMain implements ICompatInventory {

  private static final CompatMain INSTANCE = new CompatMain();

  public static CompatMain getInstance() {
    return INSTANCE;
  }

  @Override
  public InventoryType getType() {
    return InventoryType.MAIN;
  }

  @Override
  public NonNullList<ItemStack> getAllContents(@Nonnull final EntityPlayerMP player) {
    return player.inventory.mainInventory;
  }

  @Override
  public void removeItem(@Nonnull final EntityPlayerMP player, final int slot) {
    player.inventory.mainInventory.set(slot, ItemStack.EMPTY);
  }

  @Override
  public ItemStack setItemReturnOverflow(@Nonnull final EntityPlayerMP player, final int slot, @Nonnull final ItemStack itemStack) {
    if (player.inventory.mainInventory.get(slot).isEmpty()) {
      player.inventory.mainInventory.set(slot, itemStack);
      return ItemStack.EMPTY;
    }
    return itemStack;
  }

  @Override
  public boolean isSlotEmpty(@Nonnull final EntityPlayerMP player, final int slot) {
    return player.inventory.mainInventory.get(slot).isEmpty();
  }
}
