package it.mmariotti.covid19.model;

import java.util.function.BiConsumer;
import java.util.function.ToDoubleFunction;
import java.util.function.ToLongFunction;
import org.apache.commons.lang3.ArrayUtils;


public enum RecordProperty
{
	confirmed(Record::getConfirmed, Record::setConfirmed, Record::getConfirmedDelta, Record::getConfirmedDeltaPercent, Record::getConfirmedPopulationPercent, Record::getConfirmedHypothetical),
	deceased(Record::getDeceased, Record::setDeceased, Record::getDeceasedDelta, Record::getDeceasedDeltaPercent, Record::getDeceasedPopulationPercent, Record::getDeceasedHypothetical),
	recovered(Record::getRecovered, Record::setRecovered, Record::getRecoveredDelta, Record::getRecoveredDeltaPercent, Record::getRecoveredPopulationPercent, Record::getRecoveredHypothetical),
	tested(Record::getTested, Record::setTested, Record::getTestedDelta, Record::getTestedDeltaPercent, Record::getTestedPopulationPercent, Record::getTested),
	quarantined(Record::getQuarantined, Record::setQuarantined, Record::getQuarantinedDelta, Record::getQuarantinedDeltaPercent, Record::getQuarantinedPopulationPercent, Record::getQuarantined),
	standardCare(Record::getStandardCare, Record::setStandardCare, Record::getStandardCareDelta, Record::getStandardCareDeltaPercent, Record::getStandardCarePopulationPercent, Record::getStandardCare),
	intensiveCare(Record::getIntensiveCare, Record::setIntensiveCare, Record::getIntensiveCareDelta, Record::getIntensiveCareDeltaPercent, Record::getIntensiveCarePopulationPercent, Record::getIntensiveCare),
	active(Record::getActive, Record::setActive, Record::getActiveDelta, Record::getActiveDeltaPercent, Record::getActivePopulationPercent, Record::getActiveHypothetical),
	closed(Record::getClosed, Record::setClosed, Record::getClosedDelta, Record::getClosedDeltaPercent, Record::getClosedPopulationPercent, Record::getClosedHypothetical),
	hospitalized(Record::getHospitalized, Record::setHospitalized, Record::getHospitalizedDelta, Record::getHospitalizedDeltaPercent, Record::getHospitalizedPopulationPercent, Record::getHospitalized);

	private static final RecordProperty[] PRIMARY = { confirmed, deceased, recovered };

	private static final RecordProperty[] SECONDARY = { tested, quarantined, standardCare, intensiveCare };

	private static final RecordProperty[] DERIVED = { active, closed, hospitalized };

	private static final RecordProperty[] MAIN = ArrayUtils.addAll(PRIMARY, SECONDARY);


	private final ToLongFunction<? super Record> getter;

	private final BiConsumer<? super Record, Long> setter;

	private final ToLongFunction<? super Record> deltaGetter;
	private final ToDoubleFunction<? super Record> deltaPercentGetter;
	private final ToDoubleFunction<? super Record> populationPercentGetter;
	private final ToLongFunction<? super Record> hypotheticalGetter;

	private RecordProperty(
		ToLongFunction<? super Record> getter,
		BiConsumer<? super Record, Long> setter,
		ToLongFunction<? super Record> deltaGetter,
		ToDoubleFunction<? super Record> deltaPercentGetter,
		ToDoubleFunction<? super Record> populationPercentGetter,
		ToLongFunction<? super Record> hypotheticalGetter)
	{
		this.getter = getter;
		this.setter = setter;
		this.deltaGetter = deltaGetter;
		this.deltaPercentGetter = deltaPercentGetter;
		this.populationPercentGetter = populationPercentGetter;
		this.hypotheticalGetter = hypotheticalGetter;
	}

	public static RecordProperty[] getPrimary()
	{
		return PRIMARY;
	}

	public static RecordProperty[] getSecondary()
	{
		return SECONDARY;
	}

	public static RecordProperty[] getDerived()
	{
		return DERIVED;
	}

	public static RecordProperty[] getMain()
	{
		return MAIN;
	}

	public String[] getDerivedNames()
	{
		String name = name();

		return new String[] {
			name,
			name + "Delta",
			name + "DeltaPercent",
			name + "PopulationPercent",
			name + "Hypothetical"
		};
	}

	public long get(Record record)
	{
		return record != null ? getter.applyAsLong(record) : 0;
	}

	public void set(Record record, long value)
	{
		if(record != null)
		{
			setter.accept(record, value);
		}
	}

	public long getDelta(Record record)
	{
		return record != null ? deltaGetter.applyAsLong(record) : 0;
	}

	public double getDeltaPercent(Record record)
	{
		return record != null ? deltaPercentGetter.applyAsDouble(record) : 0;
	}

	public double getPopulationPercent(Record record)
	{
		return record != null ? populationPercentGetter.applyAsDouble(record) : 0;
	}

	public long getHypothetical(Record record)
	{
		return record != null ? hypotheticalGetter.applyAsLong(record) : 0;
	}
}
