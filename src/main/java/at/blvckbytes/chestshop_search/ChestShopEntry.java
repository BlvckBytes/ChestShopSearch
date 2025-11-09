package at.blvckbytes.chestshop_search;

import at.blvckbytes.chestshop_search.config.MainSection;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import me.blvckbytes.bukkitevaluable.IItemBuildable;
import me.blvckbytes.bukkitevaluable.ItemBuilder;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;
import me.blvckbytes.gpeee.interpreter.IEvaluationEnvironment;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChestShopEntry {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##");

  public final ItemStack item;
  public final String owner;
  public final Location signLocation;
  public final int quantity;
  public final double buyPrice;
  public final double sellPrice;
  public final BlockVector3 blockVector;

  public final IEvaluationEnvironment shopEnvironment;

  public int stock;
  public int containerSize;

  private final ConfigKeeper<MainSection> config;
  private IItemBuildable representativeBuildable;

  public ChestShopEntry(
    ItemStack item,
    String owner,
    Location signLocation,
    int quantity,
    double buyPrice,
    double sellPrice,
    int stock,
    int containerSize,
    ConfigKeeper<MainSection> config
  ) {
    this.item = item;
    this.owner = owner;
    this.signLocation = signLocation;
    this.quantity = quantity;
    this.buyPrice = buyPrice;
    this.sellPrice = sellPrice;
    this.stock = stock;
    this.containerSize = containerSize;
    this.config = config;
    this.blockVector = BukkitAdapter.adapt(signLocation).toVector().toBlockPoint();

    this.shopEnvironment = new EvaluationEnvironmentBuilder()
      .withStaticVariable("owner", owner)
      .withStaticVariable("quantity", quantity)
      .withStaticVariable("buy_price", DECIMAL_FORMAT.format(buyPrice))
      .withStaticVariable("sell_price", DECIMAL_FORMAT.format(sellPrice))
      .withLiveVariable("remaining_stock", () -> this.stock)
      .withLiveVariable("remaining_space", this::calculateSpace)
      .withLiveVariable("can_buy", () -> buyPrice >= 0)
      .withLiveVariable("can_sell", () -> sellPrice >= 0)
      .withStaticVariable("loc_world", signLocation.getWorld().getName())
      .withStaticVariable("loc_x", signLocation.getBlockX())
      .withStaticVariable("loc_y", signLocation.getBlockY())
      .withStaticVariable("loc_z", signLocation.getBlockZ())
      .build();

    this.updateBuildable();
  }

  public void updateBuildable() {
    this.representativeBuildable = new ItemBuilder(item, item.getAmount()).patch(config.rootSection.resultDisplay.items.representativePatch);
  }

  public IItemBuildable getRepresentativeBuildable() {
    return this.representativeBuildable;
  }

  public int calculateSpace() {
    if (containerSize < 0)
      return -1;

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

  public static @Nullable ChestShopEntry fromJson(JsonElement json, ConfigKeeper<MainSection> config, Logger logger) {
    try {
      var yamlConfig = YamlConfiguration.loadConfiguration(new StringReader(json.getAsString()));

      return new ChestShopEntry(
        yamlConfig.getItemStack("item"),
        yamlConfig.getString("owner"),
        Objects.requireNonNull(yamlConfig.getLocation("signLocation")),
        yamlConfig.getInt("quantity"),
        yamlConfig.getDouble("buyPrice"),
        yamlConfig.getDouble("sellPrice"),
        yamlConfig.getInt("stock"),
        yamlConfig.getInt("containerSize", 0),
        config
      );
    } catch (Throwable e) {
      logger.log(Level.WARNING, "An error occurred while trying to parse a shop from it's YAML-representation", e);
      return null;
    }
  }
}
