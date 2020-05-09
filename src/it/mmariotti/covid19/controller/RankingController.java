package it.mmariotti.covid19.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
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
	private DualListModel<Record> growth;
	private DualListModel<Record> activeHypoteticalZero;
	private DualListModel<Record> confirmedHypoteticalFull;

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
		growth = buildList(Record::getGrowth, x -> x.getClosedDelta() > 0);
		activeHypoteticalZero = buildList(Record::getGrowth, x -> x.getActiveDelta() < 0);
		confirmedHypoteticalFull = buildList(Record::getGrowth, x -> x.getRegion().getPopulation() > 0 && x.getTested() > 0 && x.getConfirmedDelta() > 0);

		buildCustomModels();
	}

	public void buildCustomModels()
	{
		value = buildList(property::get);
		delta = buildList(property::getDelta);
		percent = buildList(property::getDeltaPercent);
		population = buildList(property::getPopulationPercent, x -> x.getRegion().getPopulation() > 0);
		hypothetical = buildList(property::getHypothetical, x -> x.getRegion().getPopulation() > 0);
	}

	private <U extends Comparable<? super U>> DualListModel<Record> buildList(Function<? super Record, ? extends U> mapper, Predicate<? super Record> filter)
	{
		List<Record> list = StreamEx.of(records)
			.filter(filter)
			.sorted(Comparator.comparing(mapper))
			.toList();

		return buildDualListModel(list);
	}

	private <U extends Comparable<? super U>> DualListModel<Record> buildList(Function<? super Record, ? extends U> mapper)
	{
		List<Record> list = StreamEx.of(records)
			.sorted(Comparator.comparing(mapper))
			.toList();

		return buildDualListModel(list);
	}

	private static DualListModel<Record> buildDualListModel(List<Record> list)
	{
		int size = list.size();

		List<Record> downList = list.subList(0, Math.min(size, LIMIT));
		List<Record> upList = new ArrayList<>(list.subList(Math.max(size - LIMIT, 0), size));
		Collections.reverse(upList);

		return new DualListModel<>(downList, upList);
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

	public DualListModel<Record> getGrowth()
	{
		return growth;
	}

	public DualListModel<Record> getActiveHypoteticalZero()
	{
		return activeHypoteticalZero;
	}

	public DualListModel<Record> getConfirmedHypoteticalFull()
	{
		return confirmedHypoteticalFull;
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
