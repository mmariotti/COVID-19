package it.mmariotti.covid19.service;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
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

    private static final Comparator<Region> REGION_COMPARATOR = Comparator.comparing(Region::getName, (x, y) -> Region.WORLD.equals(x) ? -1 : Region.WORLD.equals(y) ? 1 : x.contains(y) ? -1 : y.contains(x) ? 1 : 0);

    @Inject
    private TestedService testedService;

    @PersistenceContext
    private EntityManager em;

    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void compute(Set<Record> records)
    {
        logger.info("compute() {} records", records.size());

        em.setFlushMode(FlushModeType.COMMIT);

        NavigableMap<Date, List<Record>> grouping = StreamEx.of(records).groupingBy(Record::getRegistered, TreeMap::new, Collectors.toList());

        for(Entry<Date, List<Record>> entry : grouping.entrySet())
        {
            compute(entry.getKey(), entry.getValue());
        }
    }

    private void compute(Date registered, List<Record> records)
    {
        Map<String, Long> testedMap = testedService.getTestedMap().get(registered);

        Map<Region, Record> recordMap = StreamEx.of(records).toMap(x -> em.find(Region.class, x.getRegion().getName()), x -> x);

        List<Region> regions = StreamEx.ofKeys(recordMap)
            .flatMap(x -> StreamEx.iterate(x.getContainer(), Objects::nonNull, Region::getContainer))
            .remove(recordMap::containsKey)
            .removeBy(Region::getName, "Italy")
            .distinct()
            .sorted(REGION_COMPARATOR)
            .toList();

        for(Region region : regions)
        {
            Record aggregate = Record.buildRecord(em, region, registered);

            boolean isNew = !em.contains(aggregate);

            if(isNew || aggregate.isAggregate())
            {
                List<Record> latestSubRecordList = Record.latestSubRecordList(em, region, registered);
                latestSubRecordList.replaceAll(x -> recordMap.getOrDefault(x.getRegion(), x));

                aggregate.aggregate(latestSubRecordList);

                if(testedMap != null)
                {
                    String regionName = region.getName();
                    long tested = testedMap.getOrDefault(regionName, 0L);
                    if(tested != 0)
                    {
                        aggregate.setTested(tested);
                    }
                }


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
