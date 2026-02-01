package at.blvckbytes.chestshop_search.command;

import at.blvckbytes.chestshop_search.config.MainSection;
import at.blvckbytes.cm_mapper.ConfigKeeper;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Level;
import java.util.logging.Logger;

public class ShopSearchReloadCommand implements CommandExecutor {

  private final ConfigKeeper<MainSection> config;
  private final Logger logger;

  public ShopSearchReloadCommand(
    ConfigKeeper<MainSection> config,
    Logger logger
  ) {
    this.config = config;
    this.logger = logger;
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!sender.hasPermission("chestshopsearch.reload"))
      return false;

    try {
      this.config.reload();

      config.rootSection.playerMessages.pluginReloadedSuccess.sendMessage(sender);
    } catch (Exception e) {
      logger.log(Level.SEVERE, "An error occurred while trying to reload the config", e);
      config.rootSection.playerMessages.pluginReloadedError.sendMessage(sender);
    }

    return false;
  }
}
