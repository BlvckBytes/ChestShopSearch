package at.blvckbytes.chestshop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class PlayerMessagesSection extends AConfigSection {

  public BukkitEvaluable languageCommandUsage;
  public BukkitEvaluable languageCommandUnknownLanguage;
  public BukkitEvaluable languageCommandSelectionMade;

  public BukkitEvaluable shopTeleportCommandUsage;
  public BukkitEvaluable shopTeleportCommandUnknownWorld;
  public BukkitEvaluable shopTeleportCommandInvalidCoordinates;
  public BukkitEvaluable shopTeleportCommandNoShop;
  public BukkitEvaluable shopTeleportCommandShopGone;
  public BukkitEvaluable shopTeleportCommandTeleported;

  public BukkitEvaluable searchCommandInvalidSearch;
  public BukkitEvaluable searchCommandNoResults;
  public BukkitEvaluable searchCommandResponse;

  public BukkitEvaluable shopTeleportShopGone;
  public BukkitEvaluable shopTeleportTeleported;

  public PlayerMessagesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
