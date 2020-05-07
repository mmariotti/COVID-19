package it.mmariotti.covid19.model;

import java.util.function.BiConsumer;
import java.util.function.ToLongFunction;
import org.apache.commons.lang3.ArrayUtils;


public enum RecordProperty
{
	confirmed(Record::getConfirmed, Record::setConfirmed),
	deceased(Record::getDeceased, Record::setDeceased),
	recovered(Record::getRecovered, Record::setRecovered),
	tested(Record::getTested, Record::setTested),
	quarantined(Record::getQuarantined, Record::setQuarantined),
	standardCare(Record::getStandardCare, Record::setStandardCare),
	intensiveCare(Record::getIntensiveCare, Record::setIntensiveCare),
	active(Record::getActive, Record::setActive),
	closed(Record::getClosed, Record::setClosed),
	hospitalized(Record::getHospitalized, Record::setHospitalized);

	private static final RecordProperty[] PRIMARY = { confirmed, deceased, recovered };

	private static final RecordProperty[] SECONDARY = { tested, quarantined, standardCare, intensiveCare };

	private static final RecordProperty[] DERIVED = { active, closed, hospitalized };

	private static final RecordProperty[] MAIN = ArrayUtils.addAll(PRIMARY, SECONDARY);


	private final ToLongFunction<? super Record> getter;

	private final BiConsumer<? super Record, Long> setter;

	private RecordProperty(ToLongFunction<? super Record> getter, BiConsumer<? super Record, Long> setter)
	{
		this.getter = getter;
		this.setter = setter;
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
}
