package at.blvckbytes.chestshop_extensions.command;

import at.blvckbytes.chestshop_extensions.ChestShopEntry;
import at.blvckbytes.chestshop_extensions.ChestShopRegistry;
import me.blvckbytes.item_predicate_parser.SingletonTranslationRegistry;
import me.blvckbytes.item_predicate_parser.TranslationLanguageRegistry;
import me.blvckbytes.item_predicate_parser.translation.TranslationLanguage;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ExportAdminshopsCommand implements CommandExecutor, TabCompleter {

  private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

  // TODO: Would be nice if these messages were also properly formatted and using CM in the config; just not feeling it right now, seeing how this is merely an internal tool.

  private final File outputFile;
  private final ChestShopRegistry shopRegistry;
  private final TranslationLanguageRegistry languageRegistry;
  private final Logger logger;

  public ExportAdminshopsCommand(
    Plugin plugin,
    ChestShopRegistry shopRegistry,
    TranslationLanguageRegistry languageRegistry
  ) {
    this.shopRegistry = shopRegistry;
    this.languageRegistry = languageRegistry;
    this.outputFile = new File(plugin.getDataFolder(), "adminshops-export.csv");
    this.logger = plugin.getLogger();
  }

  @Override
  public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender.hasPermission("chestshopextensions.exportadminshops"))) {
      sender.sendMessage("§cYou're lacking permission to use this command.");
      return true;
    }

    if (args.length != 1) {
      sender.sendMessage("§cUsage: /" + label + " <Language>");
      return true;
    }

    var language = TranslationLanguage.matcher.matchFirst(args[0]);

    if (language == null) {
      sender.sendMessage("§cInvalid language: " + args[0] + "; use one of " + String.join(", ", TranslationLanguage.matcher.createCompletions(null)));
      return true;
    }

    var availableShops = new ArrayList<ChestShopEntry>();

    shopRegistry.forEachKnownShop(shop -> {
      if (ChestShopRegistry.isAdminShop(shop.owner))
        availableShops.add(shop);
    });

    if (availableShops.isEmpty()) {
      sender.sendMessage("§cDidn't find any currently-known adminshops! Try loading the chunks of the corresponding regions by walking in them.");
      return true;
    }

    var registry = languageRegistry.getTranslationRegistry(language.constant);

    try (
      var writer = new FileWriter(outputFile);
    ) {
      writer.write("Type;Quantity;Buy-Price;Sell-Price;Location;Item-Name;Item-Lore;Item-Enchantments");

      for (var shop : availableShops) {
        writer.write('\n');

        writer.write(tryTranslate(registry, shop.item.getType(), Material::name));

        writer.write(';');
        writer.write(String.valueOf(shop.quantity));

        writer.write(';');
        writer.write(shop.buyPrice <= 0 ? "/" : DECIMAL_FORMAT.format(shop.buyPrice));

        writer.write(';');
        writer.write(shop.sellPrice <= 0 ? "/" : DECIMAL_FORMAT.format(shop.sellPrice));

        writer.write(';');
        writer.write(shop.signLocation.getBlockX() + " " + shop.signLocation.getBlockY() + " " + shop.signLocation.getBlockZ() + " " + shop.signLocation.getWorld().getName());

        var itemMeta = shop.item.getItemMeta();

        writer.write(';');
        var name = itemMeta == null ? null : itemMeta.displayName();
        writer.write(asCSVValue(
          name == null ? "" : componentToPlainText(name)
        ));

        writer.write(';');
        var lore = itemMeta == null ? null : itemMeta.lore();
        writer.write(asCSVValue(
          lore == null ? "" : lore.stream().map(this::componentToPlainText).collect(Collectors.joining("\\n"))
        ));

        writer.write(';');
        writer.write(asCSVValue(
          shop.item.getEnchantments().entrySet().stream()
            .map(entry -> tryTranslate(registry, entry.getKey(), it -> it.getKey().getKey()) + " " + entry.getValue())
            .collect(Collectors.joining(", "))
        ));
      }
    } catch (Throwable e) {
      logger.log(Level.SEVERE, "An error occurred while trying to export adminshop-prices to CSV", e);
    }

    sender.sendMessage("§aExported " + availableShops.size() + " shops to " + outputFile);
    return true;
  }

  @Override
  public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
    if (!(sender.hasPermission("chestshopextensions.exportadminshops")) || args.length != 1)
      return List.of();

    return TranslationLanguage.matcher.createCompletions(args[0]);
  }

  private String asCSVValue(String input) {
    if (input.isEmpty())
      return "/";

    return input
      .replace("\n", "\\n")
      .replace("\r", "\\r")
      .replace(";", "(amp)");
  }

  private <T> String tryTranslate(SingletonTranslationRegistry translationRegistry, T value, Function<T, String> fallbackExtractor) {
    var translation = translationRegistry.getTranslationBySingleton(value);

    if (translation == null)
      return fallbackExtractor.apply(value);

    return translation;
  }

  private String componentToPlainText(Component component) {
    var resultBuilder = new StringBuilder();
    appendTextAndWalkChildren(component, resultBuilder);
    return resultBuilder.toString();
  }

  private void appendTextAndWalkChildren(Component component, StringBuilder output) {
    if (component instanceof net.kyori.adventure.text.TextComponent textComponent)
      output.append(textComponent.content());

    for (var child : component.children())
      appendTextAndWalkChildren(child, output);
  }
}
