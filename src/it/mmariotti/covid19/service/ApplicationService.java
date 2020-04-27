package it.mmariotti.covid19.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.ExcludeDefaultInterceptors;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import it.mmariotti.covid19.model.Record;
import it.mmariotti.covid19.model.Region;
import one.util.streamex.StreamEx;


@Singleton
@Startup
public class ApplicationService
{
	public static final String LOOKUP_NAME = "java:module/ApplicationService";

	@PersistenceContext
	private EntityManager em;

	@EJB
	private ScheduleService scheduleService;

	private Map<String, Record> latestRecordMap;

	private Map<String, List<Record>> latestSubRecordMap;

	public static ApplicationService lookup()
	{
		try
		{
			return InitialContext.doLookup(LOOKUP_NAME);
		}
		catch(NamingException e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@PostConstruct
	@TransactionAttribute(TransactionAttributeType.NEVER)
	public void init()
	{
		scheduleService.executeFetch();
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public void buildLatestRecordMap()
	{
		List<Record> latestRecordList = Record.latestRecordList(em);
		latestRecordList.forEach(x -> x.getRegion().setLatestRecord(x));

		latestRecordMap = StreamEx.of(latestRecordList).toMap(x -> x.getRegion().getName(), x -> x);
		latestSubRecordMap = StreamEx.of(latestRecordList).toMap(
			x -> Optional.ofNullable(x.getRegion().getContainer()).map(Region::getName).orElse(null),
			x ->
			{
				List<Record> list = new ArrayList<>();
				list.add(x);
				return list;
			},
			(List<Record> prevList, List<Record> nextList) ->
			{
				prevList.addAll(nextList);
				return prevList;
			});
	}

	@ExcludeDefaultInterceptors
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Map<String, Record> getLatestRecordMap()
	{
		return latestRecordMap;
	}

	@ExcludeDefaultInterceptors
	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Map<String, List<Record>> getLatestSubRecordMap()
	{
		return latestSubRecordMap;
	}
}
