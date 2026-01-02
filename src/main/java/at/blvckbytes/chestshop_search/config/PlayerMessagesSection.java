package at.blvckbytes.chestshop_search.config;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bukkitevaluable.BukkitEvaluable;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class PlayerMessagesSection extends AConfigSection {

  public BukkitEvaluable searchCommandInvalidSearch;
  public BukkitEvaluable searchCommandNoResults;
  public BukkitEvaluable searchCommandResponse;
  public BukkitEvaluable searchCommandBlankUi;

  public BukkitEvaluable shopTeleportShopGone;
  public BukkitEvaluable shopTeleportTeleported;

  public BukkitEvaluable shopSearchToggleNotInARegion;
  public BukkitEvaluable shopSearchToggleNowVisible;
  public BukkitEvaluable shopSearchToggleNowInvisible;

  public BukkitEvaluable pluginReloadedSuccess;
  public BukkitEvaluable pluginReloadedError;

  public PlayerMessagesSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
