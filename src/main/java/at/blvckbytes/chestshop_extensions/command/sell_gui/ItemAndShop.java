package at.blvckbytes.chestshop_extensions.command.sell_gui;

import at.blvckbytes.chestshop_extensions.ChestShopEntry;
import at.blvckbytes.chestshop_extensions.MutableInt;
import org.bukkit.inventory.ItemStack;

public record ItemAndShop(
  ItemStack item,
  ChestShopEntry shop,
  MutableInt count
) implements ItemHolder {}
