package at.blvckbytes.chestshop_search.display.result;

import at.blvckbytes.chestshop_search.ChestShopEntry;
import at.blvckbytes.component_markup.expression.interpreter.InterpretationEnvironment;
import com.google.gson.JsonObject;

import java.util.*;

public class SelectionState {

  // FIXME: It would certainly be far cleaner to directly operate on the instances which are also
  //        accessed by the markup, instead of keeping separate indices and maps.

  private List<SortingCriterionSelection> sortingSelections;
  private int selectedSortingSelectionIndex;

  private Map<ShopFilteringCriteria, PredicateSelection> filteringSelections;
  private ShopFilteringCriteria selectedFilteringCriteria;

  public SelectionState() {
    resetSorting();
    resetFiltering();
  }

  private SelectionState(
    List<SortingCriterionSelection> sortingSelections,
    int selectedSortingSelectionIndex,
    Map<ShopFilteringCriteria, PredicateSelection> filteringSelections,
    ShopFilteringCriteria selectedFilteringCriteria
  ) {
    this.sortingSelections = sortingSelections;
    this.selectedSortingSelectionIndex = selectedSortingSelectionIndex;
    this.filteringSelections = filteringSelections;
    this.selectedFilteringCriteria = selectedFilteringCriteria;
  }

  public void resetFiltering() {
    this.filteringSelections = makeDefaultFilteringSelections();
    this.selectedFilteringCriteria = ShopFilteringCriteria.values.getFirst();
  }

  public void resetSorting() {
    this.sortingSelections = makeDefaultSortingSelections();
    this.selectedSortingSelectionIndex = 0;
  }

  public void nextSortingSelection() {
    if (++this.selectedSortingSelectionIndex == this.sortingSelections.size())
      this.selectedSortingSelectionIndex = 0;
  }

  public void nextSortingOrder() {
    var sortingSelection = sortingSelections.get(this.selectedSortingSelectionIndex);
    sortingSelection.selection = sortingSelection.selection.next();
  }

  public void moveSortingSelectionDown() {
    var removalIndex = selectedSortingSelectionIndex;
    nextSortingSelection();
    var targetSelection = this.sortingSelections.remove(removalIndex);
    this.sortingSelections.add(selectedSortingSelectionIndex, targetSelection);
  }

  public void nextFilteringCriterion() {
    this.selectedFilteringCriteria = this.selectedFilteringCriteria.next();
  }

  public void nextFilteringState() {
    PredicateSelection currentState = this.filteringSelections.get(this.selectedFilteringCriteria);

    if (currentState == null)
      return;

    this.filteringSelections.put(this.selectedFilteringCriteria, currentState.next());
  }

  public void applySort(List<ChestShopEntry> items) {
    items.sort((a, b) -> {
      for (var sortingSelection : sortingSelections) {
        if (sortingSelection.selection == SortingSelection.INACTIVE)
          continue;

        int sortingResult = sortingSelection.criterion.compare(a, b, sortingSelection.selection == SortingSelection.DESCENDING);

        if (sortingResult != 0)
          return sortingResult;
      }

      return 0;
    });
  }

  public List<ChestShopEntry> applyFilter(Collection<ChestShopEntry> items) {
    List<ChestShopEntry> result = new ArrayList<>();

    for (var item : items) {
      var doesMatch = true;

      for (var entry : this.filteringSelections.entrySet()) {
        var predicate = entry.getValue();

        if (predicate == PredicateSelection.INVARIANT)
          continue;

        if (entry.getKey().test(item, predicate == PredicateSelection.NEGATIVE))
          continue;

        doesMatch = false;
        break;
      }

      if (doesMatch)
        result.add(item);
    }

    return result;
  }

  public void extendSortingEnvironment(InterpretationEnvironment environment) {
    for (var index = 0; index < sortingSelections.size(); ++index)
      sortingSelections.get(index).active = index == selectedSortingSelectionIndex;

    environment.withVariable("sorting_selections", this.sortingSelections);
  }

  public void extendFilteringEnvironment(InterpretationEnvironment environment) {
    var mappedSelections = new ArrayList<FilteringCriterionSelection>();

    for (var entry : this.filteringSelections.entrySet()) {
      var selection = new FilteringCriterionSelection(entry.getKey(), entry.getValue());
      selection.active = selection.criterion == selectedFilteringCriteria;
      mappedSelections.add(selection);
    }

    environment.withVariable("filtering_selections", mappedSelections);
  }

  public JsonObject toJson() {
    var result = new JsonObject();

    var sortingSelectionsObject = new JsonObject();

    for (var sortingSelection : sortingSelections)
      sortingSelectionsObject.addProperty(String.valueOf(sortingSelection.criterion.ordinal()), String.valueOf(sortingSelection.selection.ordinal()));

    result.add("sortingSelections", sortingSelectionsObject);
    result.addProperty("selectedSortingSelectionIndex", selectedSortingSelectionIndex);

    var filteringSelectionsObject = new JsonObject();

    for (var filteringSelection : filteringSelections.entrySet())
      filteringSelectionsObject.addProperty(String.valueOf(filteringSelection.getKey().ordinal()), filteringSelection.getValue().ordinal());

    result.add("filteringSelections", filteringSelectionsObject);
    result.addProperty("selectedFilteringCriteria", selectedFilteringCriteria.ordinal());

    return result;
  }

  public static SelectionState fromJson(JsonObject json) {
    var sortingSelections = makeDefaultSortingSelections();
    var sortingSelectionsObject = json.getAsJsonObject("sortingSelections");

    var sortingSelectionIndex = 0;

    for (var sortingSelectionEntry : sortingSelectionsObject.entrySet()) {
      var criterion = ShopSortingCriteria.byOrdinalOrFirst(Integer.parseInt(sortingSelectionEntry.getKey()));
      var selection = SortingSelection.byOrdinalOrFirst(sortingSelectionEntry.getValue().getAsInt());

      sortingSelections.removeIf(x -> x.criterion == criterion);
      sortingSelections.add(sortingSelectionIndex, new SortingCriterionSelection(criterion, selection));

      ++sortingSelectionIndex;
    }

    var filteringSelections = makeDefaultFilteringSelections();
    var filteringSelectionsObject = json.getAsJsonObject("filteringSelections");

    for (ShopFilteringCriteria criterion : ShopFilteringCriteria.values) {
      filteringSelections.put(
        criterion,
        PredicateSelection.byOrdinalOrFirst(filteringSelectionsObject.getAsJsonPrimitive(String.valueOf(criterion.ordinal())).getAsInt())
      );
    }

    var selectedFilteringCriteria = ShopFilteringCriteria.byOrdinalOrFirst(json.getAsJsonPrimitive("selectedFilteringCriteria").getAsInt());
    return new SelectionState(
      sortingSelections, json.get("selectedSortingSelectionIndex").getAsInt(),
      filteringSelections, selectedFilteringCriteria
    );
  }

  private static ArrayList<SortingCriterionSelection> makeDefaultSortingSelections() {
    var result = new ArrayList<SortingCriterionSelection>();

    for (var criterion : ShopSortingCriteria.values)
      result.add(new SortingCriterionSelection(criterion, SortingSelection.INACTIVE));

    return result;
  }

  private static TreeMap<ShopFilteringCriteria, PredicateSelection> makeDefaultFilteringSelections() {
    var result = new TreeMap<ShopFilteringCriteria, PredicateSelection>();

    for (var criteria : ShopFilteringCriteria.values)
      result.put(criteria, PredicateSelection.INVARIANT);

    return result;
  }
}
