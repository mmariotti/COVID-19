package it.mmariotti.covid19.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
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
		logger.info("init() called");

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

		logger.info("init() lastUpdate: {}", lastUpdate);

		if(lastUpdate != null)
		{
			CriteriaQuery<Regione> query = builder.createQuery(Regione.class);
			Root<Regione> root = query.from(Regione.class);

			query.select(root);
			query.where(builder.equal(root.get(Regione_.data), lastUpdate));

			latestData = em.createQuery(query).getResultList();

			StreamEx.of(latestData)
				.filterBy(Regione::getDenominazione, "ITALIA")
				.findAny()
				.ifPresent(x ->
				{
					lastSummary = x;
					latestData.remove(x);
				});
		}
		else
		{
			lastUpdate = INIT_DATE;
			latestData = new ArrayList<>();
			lastSummary = new Regione();
			lastSummary.setDenominazione("ITALIA");
		}

		logger.info("init() latestData: {}", latestData.size());


		Calendar latestCal = Calendar.getInstance();
		latestCal.clear();
		latestCal.setTime(lastUpdate);
		latestCal.add(Calendar.DATE, 1);

		logger.info("init() latestCal: {}", latestCal.getTime());

		while(true)
		{
			Date date = latestCal.getTime();

			try
			{
				boolean fetched = fetch(date);
				if(!fetched)
				{
					break;
				}
			}
			catch(Exception e)
			{
				throw new RuntimeException(e.getMessage(), e);
			}

			latestCal.add(Calendar.DATE, 1);
		}

		logger.info("init() completed");
	}


	@Schedule(second = "30", minute = "2/10", hour = "*", persistent = false)
	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void update()
	{
		logger.info("update() called");

		Calendar latestCal = Calendar.getInstance();
		latestCal.clear();
		latestCal.setTime(lastUpdate);
		latestCal.add(Calendar.DATE, 1);

		Date date = latestCal.getTime();
		logger.info("update() latestCal: {}", date);

		try
		{
			fetch(date);
		}
		catch(Exception e)
		{
			logger.error(e.getMessage(), e);
		}

		logger.info("update() completed");
	}

	private boolean fetch(Date date) throws Exception
	{
		logger.info("fetch() date: {}", date);

		String param = PARAM_DATE_FORMAT.format(date);
		String spec = String.format(URL_TEMPLATE, param);
		URL url = new URL(spec);

		logger.info("fetch() url: {}", url);

		HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		connection.setRequestMethod("GET");
		connection.connect();

		try
		{
			int code = connection.getResponseCode();
			logger.info("fetch() code: {}", code);

			if(code != 200)
			{
				return false;
			}

			Map<String, Regione> oldMap = StreamEx.of(latestData).toMap(Regione::getDenominazione, x -> x);
			List<Regione> newData = new ArrayList<>();
			Regione newSummary = new Regione();
			newSummary.setDenominazione("ITALIA");
			newSummary.setCodice(0);
			newSummary.setLatitudine(BigDecimal.ZERO);
			newSummary.setLongitudine(BigDecimal.ZERO);
			newSummary.setStato("ITA");

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

					Date regionDate = regione.getData();
					if(regionDate.after(lastUpdate))
					{
						lastUpdate = regionDate;
					}
				}
			}

			newSummary.setData(lastUpdate);

			em.persist(newSummary);

			em.flush();

			latestData = newData;
			lastUpdate = date;
			lastSummary = newSummary;

			logger.info("fetch() latestData: {}", latestData);
			logger.info("fetch() lastUpdate: {}", lastUpdate);
			logger.info("fetch() lastSummary: {}", lastSummary);

			return true;
		}
		finally
		{
			connection.disconnect();

			logger.info("fetch() completed");
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

	public List<Regione> getRegionData(String name)
	{
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Regione> query = builder.createQuery(Regione.class);
		Root<Regione> root = query.from(Regione.class);

		query.select(root);
		query.where(builder.equal(root.get(Regione_.denominazione), name));
		query.orderBy(builder.asc(root.get(Regione_.data)));

		List<Regione> resultList = em.createQuery(query).getResultList();

		return resultList;
	}
}
