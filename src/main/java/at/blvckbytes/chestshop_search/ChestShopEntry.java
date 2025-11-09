package at.blvckbytes.chestshop_search;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.io.StringReader;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChestShopEntry {

  public final ItemStack item;
  public final String owner;
  public final Location signLocation;
  public final int quantity;
  public final double buyPrice;
  public final double sellPrice;

  public int stock;
  public int containerSize;

  public ChestShopEntry(
    ItemStack item,
    String owner,
    Location signLocation,
    int quantity,
    double buyPrice,
    double sellPrice,
    int stock,
    int containerSize
  ) {
    this.item = item;
    this.owner = owner;
    this.signLocation = signLocation;
    this.quantity = quantity;
    this.buyPrice = buyPrice;
    this.sellPrice = sellPrice;
    this.stock = stock;
    this.containerSize = containerSize;
  }

  public int calculateSpace() {
    var maximumCapacity = containerSize * item.getMaxStackSize();
    return Math.max(0, maximumCapacity - stock);
  }

  public @Nullable JsonElement toJson(Logger logger) {
    try {
      var yamlConfig = new YamlConfiguration();

      yamlConfig.set("item", item);
      yamlConfig.set("owner", owner);
      yamlConfig.set("signLocation", signLocation);
      yamlConfig.set("quantity", quantity);
      yamlConfig.set("buyPrice", buyPrice);
      yamlConfig.set("sellPrice", sellPrice);
      yamlConfig.set("stock", stock);
      yamlConfig.set("containerSize", containerSize);

      return new JsonPrimitive(yamlConfig.saveToString());
    } catch (Throwable e) {
      logger.log(Level.WARNING, "An error occurred while trying to stringify a shop to it's YAML-representation", e);
      return null;
    }
  }

  public static @Nullable ChestShopEntry fromJson(JsonElement json, Logger logger) {
    try {
      var yamlConfig = YamlConfiguration.loadConfiguration(new StringReader(json.getAsString()));

      return new ChestShopEntry(
        yamlConfig.getItemStack("item"),
        yamlConfig.getString("owner"),
        yamlConfig.getLocation("signLocation"),
        yamlConfig.getInt("quantity"),
        yamlConfig.getDouble("buyPrice"),
        yamlConfig.getDouble("sellPrice"),
        yamlConfig.getInt("stock"),
        yamlConfig.getInt("containerSize", 0)
      );
    } catch (Throwable e) {
      logger.log(Level.WARNING, "An error occurred while trying to parse a shop from it's YAML-representation", e);
      return null;
    }
  }
}
