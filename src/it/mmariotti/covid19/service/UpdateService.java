package it.mmariotti.covid19.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.mmariotti.covid19.model.Regione;
import it.mmariotti.covid19.model.Regione_;
import one.util.streamex.StreamEx;


@Singleton
@Startup
public class UpdateService
{
	private static final Logger logger = LoggerFactory.getLogger(UpdateService.class);

	private static final String URL_TEMPLATE = "https://raw.githubusercontent.com/pcm-dpc/COVID-19/master/dati-regioni/dpc-covid19-ita-regioni-%s.csv";

	private static final DateFormat PARAM_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private static final Date INIT_DATE;
	static
	{
		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2020, Calendar.FEBRUARY, 23);
		INIT_DATE = cal.getTime();
	}

	@PersistenceContext
	private EntityManager em;

	private List<Regione> latestData;

	private Date lastUpdate;

	private Regione lastSummary;


	@PostConstruct
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void init()
	{
		CriteriaBuilder builder = em.getCriteriaBuilder();

		CriteriaQuery<Date> dateQuery = builder.createQuery(Date.class);
		Root<Regione> dateRoot = dateQuery.from(Regione.class);

		dateQuery.select(builder.greatest(dateRoot.get(Regione_.data)));

		try
		{
			lastUpdate = em.createQuery(dateQuery).getSingleResult();
		}
		catch(NoResultException e)
		{
			// ignore
		}

		if(lastUpdate != null)
		{
			CriteriaQuery<Regione> query = builder.createQuery(Regione.class);
			Root<Regione> root = query.from(Regione.class);

			query.select(root);
			query.where(builder.equal(root.get(Regione_.data), lastUpdate));

			latestData = em.createQuery(query).getResultList();
			lastSummary = StreamEx.of(latestData).reduce(new Regione(), Regione::add);
		}
		else
		{
			lastUpdate = INIT_DATE;
			latestData = new ArrayList<>();
		}


		Calendar currentCal = Calendar.getInstance();
		currentCal.set(Calendar.HOUR_OF_DAY, 0);
		currentCal.set(Calendar.MINUTE, 0);
		currentCal.set(Calendar.SECOND, 0);
		currentCal.set(Calendar.MILLISECOND, 0);

		Calendar latestCal = Calendar.getInstance();
		latestCal.clear();
		latestCal.setTime(lastUpdate);
		latestCal.add(Calendar.DATE, 1);

		while(latestCal.compareTo(currentCal) <= 0)
		{
			Date date = latestCal.getTime();

			try
			{
				fetch(date);
			}
			catch(Exception e)
			{
				throw new RuntimeException(e.getMessage(), e);
			}

			latestCal.add(Calendar.DATE, 1);
		}

	}

	@Schedule(minute = "*/10", persistent = false)
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void update()
	{
		Calendar currentCal = Calendar.getInstance();
		currentCal.set(Calendar.HOUR_OF_DAY, 0);
		currentCal.set(Calendar.MINUTE, 0);
		currentCal.set(Calendar.SECOND, 0);
		currentCal.set(Calendar.MILLISECOND, 0);

		Calendar latestCal = Calendar.getInstance();
		latestCal.clear();
		latestCal.setTime(lastUpdate);

		if(latestCal.before(currentCal))
		{
			try
			{
				fetch(latestCal.getTime());
			}
			catch(Exception e)
			{
				logger.error(e.getMessage(), e);
			}
		}
	}

	private void fetch(Date date) throws Exception
	{
		String param = PARAM_DATE_FORMAT.format(date);
		String spec = String.format(URL_TEMPLATE, param);
		URL url = new URL(spec);

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.connect();

		try
		{
			int code = connection.getResponseCode();
			if(code != 200)
			{
				return;
			}

			Map<String, Regione> oldMap = StreamEx.of(latestData).toMap(Regione::getDenominazione, x -> x);
			List<Regione> newData = new ArrayList<>();
			Regione newSummary = new Regione();

			try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
			{
				for(String line : StreamEx.of(reader.lines()).skip(1))
				{
					logger.info(line);

					Regione regione = Regione.build(line);
					if(regione == null)
					{
						continue;
					}

					String denominazione = regione.getDenominazione();
					Regione previous = oldMap.get(denominazione);
					regione.update(previous);

					em.persist(regione);

					newData.add(regione);
					newSummary.add(regione);
				}

				em.flush();
			}

			latestData = newData;
			lastUpdate = date;
			lastSummary = newSummary;
		}
		finally
		{
			connection.disconnect();
		}
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Date getLastUpdate()
	{
		return lastUpdate;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public List<Regione> getLatestData()
	{
		return latestData;
	}

	@TransactionAttribute(TransactionAttributeType.SUPPORTS)
	public Regione getLastSummary()
	{
		return lastSummary;
	}
}
