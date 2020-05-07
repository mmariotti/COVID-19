package it.mmariotti.covid19.service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.mmariotti.covid19.model.Record;
import it.mmariotti.covid19.model.Region;
import one.util.streamex.StreamEx;


@Stateless
public class AggregatorService
{
	private static final Logger logger = LoggerFactory.getLogger(AggregatorService.class);

	private static final Comparator<String> SUBNAME_COMPARATOR = (x, y) -> -1 * Boolean.compare(x.contains(y), y.contains(x));
	private static final Comparator<String> WORLD_COMPARATOR = Comparator.comparing(Region.WORLD::equals);
	private static final Comparator<Region> REGION_COMPARATOR = Comparator.comparing(Region::getName, SUBNAME_COMPARATOR.thenComparing(WORLD_COMPARATOR));

	@PersistenceContext
	protected EntityManager em;

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void compute(Set<Record> records)
	{
		logger.info("compute() {} records", records.size());

		em.setFlushMode(FlushModeType.COMMIT);

		Map<Date, List<Record>> grouping = StreamEx.of(records).groupingBy(Record::getRegistered);
		for(Entry<Date, List<Record>> entry : grouping.entrySet())
		{
			compute(entry.getKey(), entry.getValue());
		}
	}

	private void compute(Date registered, List<Record> records)
	{
		Map<Region, Record> recordMap = StreamEx.of(records).toMap(x -> em.find(Region.class, x.getRegion().getName()), x -> x);

		List<Region> regions = StreamEx.ofKeys(recordMap)
			.flatMap(x -> StreamEx.iterate(x.getContainer(), Objects::nonNull, Region::getContainer))
			.remove(recordMap::containsKey)
			.distinct()
			.sorted(REGION_COMPARATOR)
			.toList();

		System.currentTimeMillis();

		for(Region region : regions)
		{
			Record aggregate = Record.buildRecord(em, region, registered);

			boolean isNew = !em.contains(aggregate);

			if(isNew || aggregate.isAggregate())
			{
				List<Record> latestSubRecordList = Record.latestSubRecordList(em, region, registered);
				latestSubRecordList.replaceAll(x -> recordMap.getOrDefault(x.getRegion(), x));

				aggregate.aggregate(latestSubRecordList);
				aggregate.compute();

				if(isNew)
				{
					em.persist(aggregate);
				}
			}

			recordMap.put(region, aggregate);
		}
	}

}
