package it.mmariotti.covid19.model;

import java.util.function.BiConsumer;
import java.util.function.ToLongFunction;


public enum RecordProperty
{
	confirmed(Record::getConfirmed, Record::setConfirmed),
	deceased(Record::getDeceased, Record::setDeceased),
	recovered(Record::getRecovered, Record::setRecovered),
	tested(Record::getTested, Record::setTested),
	quarantined(Record::getQuarantined, Record::setQuarantined),
	standardCare(Record::getStandardCare, Record::setStandardCare),
	intensiveCare(Record::getIntensiveCare, Record::setIntensiveCare);

	private final ToLongFunction<? super Record> getter;

	private final BiConsumer<? super Record, Long> setter;

	private RecordProperty(ToLongFunction<? super Record> getter, BiConsumer<? super Record, Long> setter)
	{
		this.getter = getter;
		this.setter = setter;
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
