package com.quantumlytangled.gravekeeper.compatibility;

import com.quantumlytangled.gravekeeper.util.InventoryType;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;

public class CompatOffhand implements ICompatInventory {
  
  private static final CompatOffhand INSTANCE = new CompatOffhand();
  
  public static CompatOffhand getInstance() {
    return INSTANCE;
  }
  
  @Override
  public InventoryType getType() {
    return InventoryType.OFFHAND;
  }
  
  @Override
  public NonNullList<ItemStack> getAllContents(@Nonnull final ServerPlayerEntity player) {
    return player.inventory.offHandInventory;
  }
  
  @Override
  public void removeItem(@Nonnull final ServerPlayerEntity player, final int slot) {
    player.inventory.offHandInventory.set(slot, ItemStack.EMPTY);
  }
  
  @Override
  public ItemStack setItemReturnOverflow(@Nonnull final ServerPlayerEntity player, final int slot, @Nonnull final ItemStack itemStack) {
    if ( slot >= 0
      && slot < player.inventory.offHandInventory.size()
      && player.inventory.offHandInventory.get(slot).isEmpty()
      /* items are always valid */ ) {
      player.inventory.offHandInventory.set(slot, itemStack);
      return ItemStack.EMPTY;
    }
    return itemStack;
  }
  
}
