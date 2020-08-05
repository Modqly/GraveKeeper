package com.quantumlytangled.gravekeeper.util;

import javax.annotation.Nonnull;
import com.quantumlytangled.gravekeeper.GraveKeeper;
import com.quantumlytangled.gravekeeper.compatability.CompatArmor;
import com.quantumlytangled.gravekeeper.compatability.CompatMain;
import com.quantumlytangled.gravekeeper.compatability.CompatOffHand;
import com.quantumlytangled.gravekeeper.compatability.ICompatInventory;
import com.quantumlytangled.gravekeeper.core.GraveKeeperConfig;
import com.quantumlytangled.gravekeeper.core.InventorySlot;
import com.quantumlytangled.gravekeeper.core.InventoryType;
import com.quantumlytangled.gravekeeper.core.Registration;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraftforge.common.util.Constants.NBT;

public class InventoryHandler {
  
  // Version id
  private static final int VERSION = 1;
  
  // Vanilla wrappers
  public static ICompatInventory compatArmor = CompatArmor.getInstance();
  public static ICompatInventory compatMain = CompatMain.getInstance();
  public static ICompatInventory compatOffHand = CompatOffHand.getInstance();
  
  // Modded wrappers
  public static ICompatInventory compatBaubles = null;
  public static ICompatInventory compatGalacticCraft = null;
  public static ICompatInventory compatTechGuns = null;
  
  @Nonnull
  public static List<InventorySlot> collectOnDeath(@Nonnull final EntityPlayerMP player) {
    final List<InventorySlot> inventorySlots = new ArrayList<>();
    
    // check for charms
    final SoulboundHandler.Mode soulboundMode = computeSoulboundMode(player, compatMain);
    if (GraveKeeperConfig.DEBUG_LOGS) {
      Registration.logger.info(String.format("Soulbound mode is %s",
          soulboundMode));
    }
    
    // collect all items
    collectOnDeath(player, soulboundMode, inventorySlots, compatArmor);
    collectOnDeath(player, soulboundMode, inventorySlots, compatOffHand);
    collectOnDeath(player, soulboundMode, inventorySlots, compatMain);
    
    if (compatBaubles != null) {
      collectOnDeath(player, soulboundMode, inventorySlots, compatBaubles);
    }
    if (compatGalacticCraft != null) {
      collectOnDeath(player, soulboundMode, inventorySlots, compatGalacticCraft);
    }
    if (compatTechGuns != null) {
      collectOnDeath(player, soulboundMode, inventorySlots, compatTechGuns);
    }
    
    // restore soulbound items
    if (GraveKeeperConfig.MOVE_SOULBOUND_ITEMS_TO_MAIN_INVENTORY) {
      for (final InventorySlot inventorySlot : inventorySlots) {
        if ( inventorySlot.isSoulbound
          && inventorySlot.type != InventoryType.ARMOUR
          && inventorySlot.type != InventoryType.MAIN ) {
          if (player.inventory.addItemStackToInventory(inventorySlot.itemStack.copy())) {
            continue;
          }
          // main inventory is overflowing => cancel soulbound so we keep that item in the grave
          Registration.logger.warn(String.format("Failed to move soulbinded item to main inventory (is it full?): %s",
                  inventorySlot.itemStack ));
          inventorySlot.isSoulbound = false;
        }
      }
    }
    
    return inventorySlots;
  }
  
  private static SoulboundHandler.Mode computeSoulboundMode(@Nonnull final EntityPlayerMP player,
      @Nonnull final ICompatInventory compatInventory) {
    final NonNullList<ItemStack> itemStacks = compatInventory.getAllContents(player);
    SoulboundHandler.Mode soulboundMode = null;
    for (final ItemStack itemStack : itemStacks) {
      if (itemStack.isEmpty()) {
        continue;
      }
      soulboundMode = SoulboundHandler.updateMode(soulboundMode, itemStack);
    }
    return soulboundMode;
  }
  
  private static void collectOnDeath(@Nonnull final EntityPlayerMP player,
      final SoulboundHandler.Mode soulboundMode,
      @Nonnull final List<InventorySlot> inventorySlots,
      @Nonnull final ICompatInventory compatInventory) {
    // compute how many further items are allowed for soulbound
    int countSoulboundRemaining = GraveKeeperConfig.KEEP_SOULBOUND_AMOUNT;
    for (final InventorySlot inventorySlot : inventorySlots) {
      if (inventorySlot.isSoulbound) {
        countSoulboundRemaining--;
      }
    }
    
    // scan inventory slots
    final NonNullList<ItemStack> itemStacks = compatInventory.getAllContents(player);
    for (int index = 0; index < itemStacks.size(); index++) {
      final ItemStack itemStack = itemStacks.get(index);
      if (itemStack.isEmpty()) {
        continue;
      }
      final boolean isSoulbound = countSoulboundRemaining > 0
                               && SoulboundHandler.isSoulbinded(soulboundMode, player.inventory.currentItem, compatInventory.getType(), index, itemStack);
      if (isSoulbound) {
        countSoulboundRemaining--;
        if (GraveKeeperConfig.DEBUG_LOGS) {
          Registration.logger.info(String.format("Keeping soulbound item %s with NBT %s",
              itemStack, itemStack.getTagCompound() ));
        }
      }
      final InventorySlot inventorySlot = new InventorySlot(itemStack, index, compatInventory.getType(), isSoulbound);
      inventorySlots.add(inventorySlot);
      if ( !isSoulbound
        || ( GraveKeeperConfig.MOVE_SOULBOUND_ITEMS_TO_MAIN_INVENTORY
          && inventorySlot.type != InventoryType.ARMOUR
          && inventorySlot.type != InventoryType.MAIN )) {
        compatInventory.removeItem(player, index);
      }
    }
  }
  
  @Nonnull
  public static List<ItemStack> restoreOrOverflow(@Nonnull final EntityPlayerMP player, @Nonnull final List<InventorySlot> inventorySlots,
      final boolean doRestoreSoulbound) {
    final List<ItemStack> overflow = new ArrayList<>();
    for (final InventorySlot inventorySlot : inventorySlots) {
      if ( !doRestoreSoulbound
        && inventorySlot.isSoulbound ) {
        continue;
      }
      InventoryHandler.restoreOrOverflow(player, inventorySlot, overflow);
    }
    return overflow;
  }
  
  private static void restoreOrOverflow(@Nonnull final EntityPlayerMP player, @Nonnull final InventorySlot inventorySlot, @Nonnull final List<ItemStack> overflow) {
    // get wrapper, falling back to main inventory in case the related mod was removed
    final ICompatInventory compatInventory;
    switch (inventorySlot.type) {
      case MAIN:
      default:
        compatInventory = compatMain;
        break;

      case ARMOUR:
        compatInventory = compatArmor;
        break;

      case OFFHAND:
        compatInventory = compatOffHand;
        break;

      case BAUBLES:
        compatInventory = compatBaubles != null ? compatBaubles : compatMain;
        break;

      case GALACTICRAFT:
        compatInventory = compatGalacticCraft != null ? compatGalacticCraft : compatMain;
        break;

      case TECHGUNS:
        compatInventory = compatTechGuns != null ? compatTechGuns : compatMain;
        break;
    }
    
    final ItemStack itemStackLeft = compatInventory.setItemReturnOverflow(player, inventorySlot.slot, inventorySlot.itemStack);
    if (!itemStackLeft.isEmpty()) {
      overflow.add(itemStackLeft);
    }
  }
  
  @Nonnull
  public static NBTTagCompound writeToNBT(@Nonnull final List<InventorySlot> inventorySlots) {
    final NBTTagCompound tagContainer = new NBTTagCompound();
    
    tagContainer.setString("modid", GraveKeeper.MODID);
    tagContainer.setString("mod_version", GraveKeeper.VERSION);
    tagContainer.setInteger("format_version", VERSION);
    
    final NBTTagList tagSlots = new NBTTagList();
    for (final InventorySlot inventorySlot : inventorySlots) {
      tagSlots.appendTag(inventorySlot.writeToNBT());
    }
    tagContainer.setTag("inventory_slots", tagSlots);
    
    return tagContainer;
  }
  
  @Nonnull
  public static List<InventorySlot> readFromNBT(@Nonnull final NBTTagCompound tagContainer) {
    final String modid = tagContainer.getString("modid");
    if (!modid.equals(GraveKeeper.MODID)) {
      throw new RuntimeException(String.format("Invalid InventorySlots format: unknown modid %s", modid));
    }
    final int version = tagContainer.getInteger("format_version");
    if (version != VERSION) {
      throw new RuntimeException(String.format("Invalid InventorySlots format: unknown version %d", version));
    }
    
    final NBTTagList tagSlots = tagContainer.getTagList("inventory_slots", NBT.TAG_COMPOUND);
    final List<InventorySlot> inventorySlots = new ArrayList<>(tagSlots.tagCount());
    for (final NBTBase tagSlot : tagSlots) {
      assert tagSlot instanceof NBTTagCompound;
      inventorySlots.add(new InventorySlot((NBTTagCompound) tagSlot));
    }
    
    return inventorySlots;
  }
}