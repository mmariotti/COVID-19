package it.mmariotti.covid19.test;

import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import org.apache.commons.lang3.time.DateUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import it.mmariotti.covid19.model.Record;


public class JpaLocalTest
{
	protected static EntityManagerFactory emf;

	protected static InitialContext ic;

	@Test
	public void queryTest() throws Exception
	{
		Date date = DateUtils.parseDate("2020-03-10", "yyyy-MM-dd");
		String region = "Italy";

		String query = "select r " +
			"from Record r " +
			"where (r.id.region, r.id.registered) in ( " +
			"	select r2.id.region, max(r2.id.registered) " +
			"	from Record r2 " +
			"	where r2.id.region.container.name = :region and r2.id.registered <= :registered " +
			"	group by r2.id.region)";

		String query2 = "select r " +
			"from Record r " +
			"where r.id.region.container.name = :region and r.id.registered = ( " +
			"	select max(r2.id.registered) " +
			"	from Record r2 " +
			"	where r2.id.region = r.id.region and r2.id.registered <= :registered)";

		accept(em ->
		{
			em.createQuery(query, Record.class)
				.setParameter("region", region)
				.setParameter("registered", date)
				.getResultList()
				.forEach(x -> System.out.println(x + " - " + x.getConfirmed()));
		});
	}

	@BeforeAll
	public static void initialize() throws Exception
	{
		System.setProperty(Context.INITIAL_CONTEXT_FACTORY, "org.osjava.sj.memory.MemoryContextFactory");
		System.setProperty("org.osjava.sj.jndi.shared", "true");

		ic = new InitialContext();

		emf = Persistence.createEntityManagerFactory("covid19-test");

		ic.rebind("java:module/persistence/EntityManagerFactory", emf);
	}

	@AfterAll
	public static void cleanup() throws Exception
	{
		if(emf != null && emf.isOpen())
		{
			emf.close();
		}

		ic.unbind("java:module/persistence/EntityManagerFactory");
		ic.unbind("java:/jdbc/covid19");
	}

	protected static <T> T apply(Function<EntityManager, T> mapper)
	{
		EntityManager em = emf.createEntityManager();
		try
		{
			EntityTransaction tx = em.getTransaction();
			try
			{
				tx.begin();

				T result = mapper.apply(em);

				tx.commit();

				return result;
			}
			catch(Exception e)
			{
				tx.rollback();

				throw e;
			}
		}
		finally
		{
			em.close();
		}
	}

	protected static void accept(Consumer<EntityManager> consumer)
	{
		apply(x ->
		{
			consumer.accept(x);
			return null;
		});
	}
}
