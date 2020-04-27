package it.mmariotti.covid19.service;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.mmariotti.covid19.model.Record;
import it.mmariotti.covid19.model.RecordId;
import it.mmariotti.covid19.model.Region;


@Stateless
public class AggregatorService
{
	private static final Logger logger = LoggerFactory.getLogger(AggregatorService.class);

	@PersistenceContext
	protected EntityManager em;

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void compute(Set<Record> records)
	{
		logger.info("compute() {} records", records.size());

		Set<RecordId> visited = new HashSet<>();

		for(Record record : records)
		{
			Date registered = record.getRegistered();
			Region region = em.find(Region.class, record.getRegion().getName());

			for(region = region.getContainer(); region != null; region = region.getContainer())
			{
				RecordId recordId = new RecordId(region, registered);
				if(!visited.add(recordId))
				{
					break;
				}

				Record aggregate = Record.buildRecord(em, recordId);
				if(em.contains(aggregate) && !aggregate.isAggregate())
				{
					continue;
				}

				List<Record> subRecords = Record.latestSubRecordList(em, region, registered);
				aggregate.aggregate(subRecords);
				aggregate.compute();

				if(!em.contains(aggregate))
				{
					em.persist(aggregate);
				}
			}
		}
	}
}
