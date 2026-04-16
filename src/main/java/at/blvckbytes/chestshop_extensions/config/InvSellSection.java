package at.blvckbytes.chestshop_extensions.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class InvSellSection extends ConfigSection {

  public ComponentMarkup filtersInventoryTitle;

  public ComponentMarkup noPermission;
  public ComponentMarkup commandUsage;
  public ComponentMarkup openingFiltersInventory;
  public ComponentMarkup savedFiltersInventory;
  public ComponentMarkup noFiltersConfigured;
  public ComponentMarkup noMatchingItemsFound;

  public InvSellSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
