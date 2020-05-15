package it.mmariotti.covid19.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.primefaces.model.DualListModel;
import it.mmariotti.covid19.model.Record;
import it.mmariotti.covid19.model.RecordProperty;
import it.mmariotti.covid19.service.ApplicationService;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;


@Named
@ViewScoped
public class RankingController implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final int LIMIT = 10;

	@EJB
	private ApplicationService applicationService;

	private List<Record> records;

	private DualListModel<Record> lethality;
	private DualListModel<Record> lethalityLatest;
	private DualListModel<Record> growth;
	private DualListModel<Record> activeHypotheticalZero;
	private DualListModel<Record> confirmedHypotheticalFull;

	private RecordProperty property = RecordProperty.confirmed;
	private DualListModel<Record> value;
	private DualListModel<Record> delta;
	private DualListModel<Record> percent;
	private DualListModel<Record> population;
	private DualListModel<Record> hypothetical;


	@PostConstruct
	public void init()
	{
		Date yesterday = DateUtils.addDays(DateUtils.truncate(new Date(), Calendar.DATE), -1);
		records = EntryStream.of(applicationService.getLatestRecordMap())
			.filterValues(x -> !x.getRegistered().before(yesterday))
			.removeKeys(x -> StringUtils.contains(x, "Diamond Princess"))
			.filterKeys(x -> StringUtils.countMatches(x, ';') < 2)
			.values()
			.toList();

		lethality = buildList(Record::getLethality, x -> x.getLethality() > 0 && x.getLethality() < 1);
		lethalityLatest = buildList(Record::getLethalityLatest, x -> x.getLethalityLatest() > 0 && x.getLethalityLatest() < 1);
		growth = buildList(Record::getGrowth, x -> x.getClosedDelta() > 0);
		activeHypotheticalZero = buildList(Record::getActiveHypotheticalZero, x -> x.getActiveHypotheticalZero() != 0);
		confirmedHypotheticalFull = buildList(Record::getConfirmedHypotheticalFull, x -> x.getConfirmedHypotheticalFull() != 0);

		buildCustomModels();
	}

	public void buildCustomModels()
	{
		value = buildList(property::get);
		delta = buildList(property::getDelta, x -> property.get(x) != property.getDelta(x));
		percent = buildList(property::getDeltaPercent, x -> property.get(x) != property.getDelta(x));
		population = buildList(property::getPopulationPercent, x -> x.getRegion().getPopulation() > 0);
		hypothetical = buildList(property::getHypothetical, x -> property.getHypothetical(x) > 0);
	}

	public String[] getSuffixes()
	{
		if(EnumSet.of(RecordProperty.confirmed, RecordProperty.deceased, RecordProperty.recovered, RecordProperty.active, RecordProperty.closed).contains(property))
		{
			return new String[] {
				"",
				"Delta",
				"DeltaPercent",
				"PopulationPercent",
				"Hypothetical"
			};
		}

		return new String[] {
			"",
			"Delta",
			"DeltaPercent",
			"PopulationPercent"
		};
	}

	public DualListModel<Record> getModel(String suffix)
	{
		switch(suffix)
		{
			case "Delta":
				return delta;

			case "DeltaPercent":
				return percent;

			case "PopulationPercent":
				return population;

			case "Hypothetical":
				return hypothetical;

			default:
				return value;
		}
	}

	public String getFormat(String suffix)
	{
		switch(suffix)
		{
			case "Delta":
				return "+#,##0;-#,##0";

			case "DeltaPercent":
				return "+#,##0.00%;-#,##0.00%";

			case "PopulationPercent":
				return "0.000%";

			case "Hypothetical":
			default:
				return "#,##0";
		}
	}

	private <U extends Comparable<? super U>> DualListModel<Record> buildList(Function<? super Record, ? extends U> mapper, Predicate<? super Record> filter)
	{
		List<Record> list = StreamEx.of(records)
			.filter(filter)
			.reverseSorted(Comparator.comparing(mapper))
			.toList();

		return buildDualListModel(list);
	}

	private <U extends Comparable<? super U>> DualListModel<Record> buildList(Function<? super Record, ? extends U> mapper)
	{
		List<Record> list = StreamEx.of(records)
			.reverseSorted(Comparator.comparing(mapper))
			.toList();

		return buildDualListModel(list);
	}

	private static DualListModel<Record> buildDualListModel(List<Record> list)
	{
		int size = list.size();

		List<Record> topList = list.subList(0, Math.min(size, LIMIT));
		List<Record> bottomList = new ArrayList<>(list.subList(Math.max(size - LIMIT, 0), size));

		return new DualListModel<>(topList, bottomList);
	}

	public RecordProperty getProperty()
	{
		return property;
	}

	public void setProperty(RecordProperty property)
	{
		this.property = property;
	}

	public DualListModel<Record> getLethality()
	{
		return lethality;
	}

	public DualListModel<Record> getLethalityLatest()
	{
		return lethalityLatest;
	}

	public DualListModel<Record> getGrowth()
	{
		return growth;
	}

	public DualListModel<Record> getActiveHypotheticalZero()
	{
		return activeHypotheticalZero;
	}

	public DualListModel<Record> getConfirmedHypotheticalFull()
	{
		return confirmedHypotheticalFull;
	}

	public DualListModel<Record> getValue()
	{
		return value;
	}

	public DualListModel<Record> getDelta()
	{
		return delta;
	}

	public DualListModel<Record> getPercent()
	{
		return percent;
	}

	public DualListModel<Record> getPopulation()
	{
		return population;
	}

	public DualListModel<Record> getHypothetical()
	{
		return hypothetical;
	}
}
