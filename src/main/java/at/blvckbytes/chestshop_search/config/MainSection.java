package at.blvckbytes.chestshop_search.config;

import at.blvckbytes.chestshop_search.config.command.CommandsSection;
import at.blvckbytes.chestshop_search.config.result_display.ResultDisplaySection;
import at.blvckbytes.chestshop_search.return_items.ReturnItemsSection;
import at.blvckbytes.cm_mapper.mapper.section.CSAlways;
import at.blvckbytes.cm_mapper.mapper.section.ConfigSection;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import at.blvckbytes.component_markup.util.logging.InterpreterLogger;

@CSAlways
public class MainSection extends ConfigSection {

  public CommandsSection commands;
  public PlayerMessagesSection playerMessages;
  public RegionFilterSection regionFilter;
  public ResultDisplaySection resultDisplay;
  public ReturnItemsSection returnItems;

  public MainSection(InterpretationEnvironment baseEnvironment, InterpreterLogger interpreterLogger) {
    super(baseEnvironment, interpreterLogger);
  }
}
