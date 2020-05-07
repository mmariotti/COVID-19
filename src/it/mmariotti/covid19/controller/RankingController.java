package it.mmariotti.covid19.controller;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import it.mmariotti.covid19.model.Record;
import it.mmariotti.covid19.service.ApplicationService;


@Named
@ViewScoped
public class RankingController implements Serializable
{
	private static final long serialVersionUID = 1L;

	@EJB
	private ApplicationService applicationService;

	private List<Record> recordList;

	@PostConstruct
	public void init()
	{
		recordList = new ArrayList<>(applicationService.getLatestRecordMap().values());
		recordList.sort(Comparator.comparing(Record::getConfirmed));
	}

	public List<Record> getRecordList()
	{
		return recordList;
	}
}
