package at.blvckbytes.chestshop_extensions.config;

import at.blvckbytes.cm_mapper.cm.ComponentMarkup;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

public class SellGuiSection extends ConfigSection {

  public @CSAlways RegionFilterSection regionFilter;

  public ComponentMarkup inventoryTitle;
  public ComponentMarkup noPermission;
  public ComponentMarkup openingPrompt;
  public ComponentMarkup emptyInventory;
  public ComponentMarkup unsellableItems;
  public ComponentMarkup unallowedWorld;
  public ComponentMarkup unallowedRegion;

  public SellGuiSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
