package at.blvckbytes.chestshop_search.config.result_display;

import at.blvckbytes.chestshop_search.config.display_common.GuiItemStackSection;
import me.blvckbytes.bbconfigmapper.sections.AConfigSection;
import me.blvckbytes.bbconfigmapper.sections.CSAlways;
import me.blvckbytes.bukkitevaluable.section.ItemStackSection;
import me.blvckbytes.gpeee.interpreter.EvaluationEnvironmentBuilder;

@CSAlways
public class ResultDisplayItemsSection extends AConfigSection {

  public GuiItemStackSection previousPage;
  public GuiItemStackSection nextPage;
  public GuiItemStackSection sorting;
  public GuiItemStackSection filtering;
  public GuiItemStackSection filler;
  public GuiItemStackSection shopOwner;
  public GuiItemStackSection backButton;
  public ItemStackSection representativePatch;

  public ResultDisplayItemsSection(EvaluationEnvironmentBuilder baseEnvironment) {
    super(baseEnvironment);
  }
}
