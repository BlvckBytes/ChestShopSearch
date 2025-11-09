package at.blvckbytes.chestshop_search.config.result_display;

import at.blvckbytes.chestshop_search.config.display_common.PaginatedGuiSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

public class ResultDisplaySection extends PaginatedGuiSection<ResultDisplayItemsSection> {

  public ResultDisplaySection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(ResultDisplayItemsSection.class, baseEnvironment);
  }
}
