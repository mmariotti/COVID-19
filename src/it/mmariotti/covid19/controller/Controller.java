package it.mmariotti.covid19.controller;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.PropertyResourceBundle;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.omnifaces.util.Faces;
import org.primefaces.context.RequestContext;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.BarChartModel;
import org.primefaces.model.chart.LegendPlacement;
import org.primefaces.model.chart.LineChartSeries;
import it.mmariotti.covid19.model.Regione;
import it.mmariotti.covid19.model.Regione_;
import it.mmariotti.covid19.service.UpdateService;


@Named
@ViewScoped
public class Controller implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM-dd");

	private static final String[] PROPERTIES = {
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

	private List<Regione> latestData;

	private BarChartModel chartModel;

	@PostConstruct
	public void init()
	{
		latestData = new ArrayList<>(service.getLatestData());
	}

	public void showChart(Regione regione, String property, boolean percent)
	{
		Faces.setRequestAttribute("chartRegione", regione.getDenominazione());
		Faces.setRequestAttribute("chartProperty", property);

		chartModel = new BarChartModel();
		chartModel.setLegendPosition("s");
		chartModel.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
		chartModel.setLegendCols(2);
		chartModel.setLegendRows(1);
		chartModel.setMouseoverHighlight(true);
		chartModel.setShowDatatip(true);
		chartModel.setShowPointLabels(true);
//		chartModel.setStacked(true);

		if(percent)
		{
			Axis yAxis = chartModel.getAxis(AxisType.Y);
			yAxis.setTickFormat("%.2f%%");
		}

		FacesContext context = FacesContext.getCurrentInstance();
		PropertyResourceBundle bundle = context.getApplication().evaluateExpressionGet(context, "#{bundle}", PropertyResourceBundle.class);

		LineChartSeries totalSeries = new LineChartSeries();
		chartModel.addSeries(totalSeries);
		totalSeries.setLabel(bundle.getString("total"));

		LineChartSeries deltaSeries = new LineChartSeries();
		deltaSeries.setLabel(bundle.getString("increment"));
		chartModel.addSeries(deltaSeries);

		List<Regione> data = service.getRegionData(regione.getDenominazione());
		for(Regione r : data)
		{
			String key = DATE_FORMAT.format(r.getData());

			double total = Faces.<Number> resolveExpressionGet(r, property).doubleValue();
			double delta = Faces.<Number> resolveExpressionGet(r, property + "Delta").doubleValue();

			if(percent)
			{
				total = total * 100;
				delta = delta * 100;
			}

			deltaSeries.set(key, delta);
			totalSeries.set(key, total);
		}

		RequestContext.getCurrentInstance().execute("PF('chartDialog').show()");
	}

	public Date getLastUpdate()
	{
		return service.getLastUpdate();
	}

	public Regione getLastSummary()
	{
		return service.getLastSummary();
	}

	public List<Regione> getLatestData()
	{
		return latestData;
	}

	public String[] getProperties()
	{
		return PROPERTIES;
	}

	public BarChartModel getChartModel()
	{
		return chartModel;
	}
}
