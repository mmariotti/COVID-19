package it.mmariotti.covid19.controller;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import it.mmariotti.covid19.model.Regione;
import it.mmariotti.covid19.model.Regione_;
import it.mmariotti.covid19.service.UpdateService;


@Named
@ApplicationScoped
public class Controller implements Serializable
{
	private static final long serialVersionUID = 1L;

	@PersistenceUnit
	private EntityManagerFactory emf;

	private String[] properties = {
		Regione_.tamponi.getName(),
		Regione_.positiviTotali.getName(),
		Regione_.positiviAttuali.getName(),
		Regione_.deceduti.getName(),
		Regione_.guariti.getName(),
		Regione_.isolamento.getName(),
		Regione_.ricoveratiNormali.getName(),
		Regione_.ricoveratiIntensiva.getName()
	};

	@EJB
	private UpdateService service;

	public Date getLastUpdate()
	{
		return service.getLastUpdate();
	}

	public List<Regione> getLatestData()
	{
		return service.getLatestData();
	}

	public Regione getLastSummary()
	{
		return service.getLastSummary();
	}

	public String[] getProperties()
	{
		return properties;
	}
}
