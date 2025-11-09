package at.blvckbytes.chestshop_search.command;

import at.blvckbytes.chestshop_search.ChestShopRegistry;
import at.blvckbytes.chestshop_search.config.MainSection;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import me.blvckbytes.bukkitevaluable.ConfigKeeper;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class ShopTeleportCommand implements CommandExecutor {

  private final ChestShopRegistry chestShopRegistry;
  private final ConfigKeeper<MainSection> config;

  public ShopTeleportCommand(
    ChestShopRegistry chestShopRegistry,
    ConfigKeeper<MainSection> config
  ) {
    this.chestShopRegistry = chestShopRegistry;
    this.config = config;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender instanceof Player player))
      return false;

    if (args.length != 4) {
      config.rootSection.playerMessages.shopTeleportCommandUsage.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("label", label)
          .build()
      );

      return true;
    }

    var world = Bukkit.getWorld(args[0]);

    if (world == null) {
      config.rootSection.playerMessages.shopTeleportCommandUnknownWorld.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("input", args[0])
          .build()
      );

      return true;
    }

    Location location;

    try {
      location = new Location(
        world,
        Double.parseDouble(args[1]),
        Double.parseDouble(args[2]),
        Double.parseDouble(args[3])
      );
    } catch (NumberFormatException e) {
      config.rootSection.playerMessages.shopTeleportCommandInvalidCoordinates.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("input", args[1] + " " + args[2] + " " + args[3])
          .build()
      );

      return true;
    }

    var coordinates = location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ();
    var targetShop = chestShopRegistry.getShopAtLocation(location);
    var signBlock = location.getBlock();

    if (targetShop == null || !(signBlock.getState() instanceof Sign sign)) {
      config.rootSection.playerMessages.shopTeleportCommandNoShop.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("coordinates", coordinates)
          .withStaticVariable("world", world.getName())
          .build()
      );

      return true;
    }

    if (!ChestShopSign.isValid(sign)) {
      config.rootSection.playerMessages.shopTeleportCommandShopGone.sendMessage(
        player,
        config.rootSection.getBaseEnvironment()
          .withStaticVariable("coordinates", coordinates)
          .withStaticVariable("world", location.getWorld().getName())
          .withStaticVariable("owner", targetShop.owner)
          .build()
      );

      chestShopRegistry.onDestruction(location);
      return true;
    }

    var signFacing = ((Directional) signBlock.getBlockData()).getFacing();

    var signCenter = location.clone();
    var footLocation = location.clone();

    switch (signFacing) {
      case NORTH -> {
        footLocation.add(.5, 0, -.1);
        signCenter.add(.5, .5, .9);
      }

      case SOUTH -> {
        footLocation.add(.5, 0, 1);
        signCenter.add(.5, .5, 0);
      }

      case WEST -> {
        footLocation.add(-.1, 0, .5);
        signCenter.add(.9, .5, .5);
      }

      case EAST -> {
        footLocation.add(1, 0, .5);
        signCenter.add(0, .5, .5);
      }

      default -> { return true; }
    }

    var eyeLocation = footLocation.clone().add(0, 1.6, 0);
    var direction = signCenter.toVector().subtract(eyeLocation.toVector()).normalize();
    footLocation.setDirection(direction);

    player.teleport(footLocation);

    config.rootSection.playerMessages.shopTeleportCommandTeleported.sendMessage(
      player,
      config.rootSection.getBaseEnvironment()
        .withStaticVariable("coordinates", coordinates)
        .withStaticVariable("world", world.getName())
        .withStaticVariable("owner", targetShop.owner)
        .build()
    );

    return true;
  }
}
