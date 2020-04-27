package it.mmariotti.covid19.controller;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Named;
import org.apache.commons.lang3.time.DateUtils;
import org.omnifaces.util.Faces;
import org.primefaces.event.NodeSelectEvent;
import org.primefaces.model.DefaultTreeNode;
import org.primefaces.model.TreeNode;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.DateAxis;
import org.primefaces.model.chart.LegendPlacement;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.LinearAxis;
import it.mmariotti.covid19.model.Record;
import it.mmariotti.covid19.model.Region;
import it.mmariotti.covid19.service.ApplicationService;
import it.mmariotti.covid19.service.DataService;
import it.mmariotti.covid19.service.ScheduleService;
import one.util.streamex.StreamEx;


@Named
@ViewScoped
public class DataController implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final SimpleDateFormat AXIS_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

	@EJB
	private ApplicationService applicationService;

	@EJB
	private DataService dataService;

	private PropertyResourceBundle bundle;

	private TreeNode rootNode;

	private TreeNode comparisonNode;

	private TreeNode selectedNode;

	private CartesianChartModel valueChart;

	private CartesianChartModel deltaChart;

	private CartesianChartModel percentChart;

	private String property = "confirmed";

	private Date minDate = ScheduleService.INITIAL_DATE;

	private Date maxDate = new Date();

	private Date startDate = minDate;

	private Date endDate = maxDate;

	@PostConstruct
	public void init()
	{
		FacesContext context = FacesContext.getCurrentInstance();
		bundle = context.getApplication().evaluateExpressionGet(context, "#{bundle}", PropertyResourceBundle.class);

		Map<String, List<Record>> latestSubRecordMap = applicationService.getLatestSubRecordMap();
		Region world = applicationService.getLatestRecordMap().get("World").getRegion();

		rootNode = new DefaultTreeNode();
		rootNode.setExpanded(true);
		rootNode.setSelectable(false);

		selectedNode = new DefaultTreeNode(world, rootNode);
		comparisonNode = selectedNode;

		StreamEx.ofTree(selectedNode, x -> StreamEx.of(x.getData())
			.select(Region.class)
			.map(Region::getName)
			.flatCollection(latestSubRecordMap::get)
			.map(Record::getRegion)
			.map(y -> new DefaultTreeNode(y, x)))
			.toList();

		selectedNode.setExpanded(true);
		selectedNode.setSelected(true);

		buildCharts();
	}

	public void onSelect(NodeSelectEvent event)
	{
		TreeNode node = event.getTreeNode();
		if(node == null)
		{
			return;
		}

		if(selectedNode != null)
		{
			selectedNode.setSelected(false);
		}

		node.setSelected(true);

		StreamEx.iterate(node.getParent(), Objects::nonNull, TreeNode::getParent)
			.nonNull()
			.forEach(x -> x.setExpanded(true));

		selectedNode = node;

		buildCharts();
	}

	public void buildCharts()
	{
		valueChart = buildChart(property, false);
		deltaChart = buildChart(property + "Delta", false);
		percentChart = buildChart(property + "DeltaPercent", true);
	}

	public CartesianChartModel buildChart(String property, boolean percent)
	{
		CartesianChartModel chart = new LineChartModel();
		chart.setLegendPosition("s");
		chart.setLegendPlacement(LegendPlacement.OUTSIDEGRID);
		chart.setLegendCols(2);
		chart.setLegendRows(1);
		chart.setMouseoverHighlight(true);
		chart.setShowDatatip(true);
		chart.setShowPointLabels(false);
//		chartModel.setStacked(true);
		chart.setZoom(true);
//		chart.setExtender("chartExtender");

		Axis xAxis = new DateAxis();
		chart.getAxes().put(AxisType.X, xAxis);
		xAxis.setTickFormat("%b-%#d");
		xAxis.setMin(AXIS_DATE_FORMAT.format(startDate));
		xAxis.setMax(AXIS_DATE_FORMAT.format(endDate));
		xAxis.setTickAngle(60);
		xAxis.setTickCount(20);

		Axis yAxis = new LinearAxis();
		chart.getAxes().put(AxisType.Y, yAxis);
		yAxis.setTickCount(11);

		if(percent)
		{
			yAxis.setTickFormat("%.2f%%");
			yAxis.setMin(0);
			yAxis.setMax(100);
		}
		else
		{
			yAxis.setTickFormat("%'d");
		}

		if(comparisonNode != null && !Objects.equals(selectedNode, comparisonNode))
		{
			ChartSeries comparisonSeries = buildSeries(comparisonNode, property, percent);
			chart.addSeries(comparisonSeries);
		}

		ChartSeries selectedSeries = buildSeries(selectedNode, property, percent);
		chart.addSeries(selectedSeries);


		StreamEx.of(chart.getSeries())
			.map(ChartSeries::getData)
			.remove(Map::isEmpty)
			.map(x -> x.keySet().iterator().next())
			.select(String.class)
			.min(Comparator.naturalOrder())
			.ifPresent(xAxis::setMin);

		return chart;
	}

	private ChartSeries buildSeries(TreeNode node, String property, boolean percent)
	{
		Region region = (Region) node.getData();

		Set<Record> records = region.getRecords();

//		NavigableMap<Date, Record> recordMap = StreamEx.of(records).toNavigableMap(Record::getRegistered, x -> x);
//
//		SortedMap<Object, Number> data = StreamEx.iterate(startDate, x -> !x.after(endDate), x -> DateUtils.addDays(x, 1))
//			.mapToEntry(recordMap::floorEntry)
//			.mapValues(x -> x == null ? null : x.getValue())
//			.mapValues(r -> r == null ? 0 : Faces.<Number> resolveExpressionGet(r, property).doubleValue())
//			.<Number> mapValues(x -> percent ? x * 100 : x)
//			.<Object> mapKeys(AXIS_DATE_FORMAT::format)
//			.toSortedMap();

		SortedMap<Object, Number> data = StreamEx.of(records)
			.mapToEntry(Record::getRegistered, x -> x)
			.removeKeys(x -> x.before(startDate) || x.after(endDate))
			.mapValues(r -> Faces.<Number> resolveExpressionGet(r, property).doubleValue())
			.<Number> mapValues(x -> percent ? x * 100 : x)
			.<Object> mapKeys(AXIS_DATE_FORMAT::format)
			.toSortedMap();

		LineChartSeries series = new LineChartSeries(region.getName());
		series.setData(data);

		return series;
	}

	public void lastWeek()
	{
		startDate = DateUtils.addDays(maxDate, -7);
		endDate = maxDate;
		buildCharts();
	}

	public void lastMonth()
	{
		startDate = DateUtils.addDays(maxDate, -30);
		endDate = maxDate;
		buildCharts();
	}

	public void fullInterval()
	{
		startDate = minDate;
		endDate = maxDate;
		buildCharts();
	}

	public TreeNode getRootNode()
	{
		return rootNode;
	}

	public TreeNode getComparisonNode()
	{
		return comparisonNode;
	}

	public void setComparisonNode(TreeNode comparisonNode)
	{
		this.comparisonNode = comparisonNode;
	}

	public TreeNode getSelectedNode()
	{
		return selectedNode;
	}

	public void setSelectedNode(TreeNode selectedNode)
	{
		this.selectedNode = selectedNode;
	}

	public String getProperty()
	{
		return property;
	}

	public void setProperty(String property)
	{
		this.property = property;
	}

	public Date getStartDate()
	{
		return startDate;
	}

	public void setStartDate(Date startDate)
	{
		this.startDate = startDate;
	}

	public Date getEndDate()
	{
		return endDate;
	}

	public void setEndDate(Date endDate)
	{
		this.endDate = endDate;
	}

	public Date getMinDate()
	{
		return minDate;
	}

	public Date getMaxDate()
	{
		return maxDate;
	}

	public CartesianChartModel getValueChart()
	{
		return valueChart;
	}

	public CartesianChartModel getDeltaChart()
	{
		return deltaChart;
	}

	public CartesianChartModel getPercentChart()
	{
		return percentChart;
	}
}
