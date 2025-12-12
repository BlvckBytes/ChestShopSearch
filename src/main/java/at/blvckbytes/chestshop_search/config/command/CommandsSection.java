package at.blvckbytes.chestshop_search.config.command;

import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
public class CommandsSection extends AConfigSection {

  public ShopSearchCommandSection shopSearch;
  public ShopSearchToggleCommandSection shopSearchToggle;
  public ShopOverviewCommandSection shopOverview;
  public ShopSearchReloadCommandSection shopSearchReload;

  public CommandsSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
