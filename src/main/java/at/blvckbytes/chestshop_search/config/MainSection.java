package at.blvckbytes.chestshop_search.config;

import at.blvckbytes.chestshop_search.config.command.CommandsSection;
import at.blvckbytes.chestshop_search.config.result_display.ResultDisplaySection;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
public class MainSection extends AConfigSection {

  public CommandsSection commands;
  public PlayerMessagesSection playerMessages;
  public RegionFilterSection regionFilter;
  public ResultDisplaySection resultDisplay;

  public MainSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
