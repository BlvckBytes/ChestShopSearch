package at.blvckbytes.chestshop_search.config.command;

import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class CommandsSection extends ConfigSection {

  public ShopSearchCommandSection shopSearch;
  public ShopSearchToggleCommandSection shopSearchToggle;
  public ShopOverviewCommandSection shopOverview;
  public ShopSearchReloadCommandSection shopSearchReload;

  public CommandsSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
