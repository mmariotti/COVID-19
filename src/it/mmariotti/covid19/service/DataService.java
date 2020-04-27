package it.mmariotti.covid19.service;

import java.util.Date;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import it.mmariotti.covid19.model.Record;
import it.mmariotti.covid19.model.Region;


@Stateless
public class DataService
{
	public static final String LOOKUP_NAME = "java:module/DataService";

	@PersistenceContext
	private EntityManager em;

	public static DataService lookup()
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

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Record findLatest(Region region)
	{
		return Record.latestRecord(em, region, new Date());
	}
}
