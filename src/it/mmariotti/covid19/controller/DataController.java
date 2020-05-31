package it.mmariotti.covid19.controller;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PropertyResourceBundle;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.IntToDoubleFunction;
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
import one.util.streamex.EntryStream;
import one.util.streamex.MoreCollectors;
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

    private final Date minDate = ScheduleService.INITIAL_DATE;

    private final Date maxDate = DateUtils.truncate(new Date(), Calendar.DATE);

    private Date startDate = minDate;

    private Date endDate = maxDate;

    @PostConstruct
    public void init()
    {
        FacesContext context = FacesContext.getCurrentInstance();
        bundle = context.getApplication().evaluateExpressionGet(context, "#{bundle}", PropertyResourceBundle.class);

        Map<String, List<Record>> latestSubRecordMap = applicationService.getLatestSubRecordMap();
        Region world = applicationService.getLatestRecordMap().get(Region.WORLD).getRegion();

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
        xAxis.setTickFormat("%b-%#d (%a)");
        xAxis.setMin(AXIS_DATE_FORMAT.format(startDate));
        xAxis.setMax(AXIS_DATE_FORMAT.format(endDate));
        xAxis.setTickAngle(60);
        xAxis.setTickCount(8);

        Axis yAxis = new LinearAxis();
        chart.getAxes().put(AxisType.Y, yAxis);
        yAxis.setTickCount(11);

        if(percent)
        {
            yAxis.setTickFormat("%.2f%%");
//			yAxis.setMin(0);
//			yAxis.setMax(100);
        }
        else
        {
            yAxis.setTickFormat("%'d");
        }

        SortedMap<Date, Double> selectedData = buildData(selectedNode, property, percent);
        String selectedName = ((Region) selectedNode.getData()).getName();
        addSeries(chart, selectedName, selectedData);

        if(comparisonNode != null && !Objects.equals(selectedNode, comparisonNode))
        {
            SortedMap<Date, Double> comparisonData = buildData(comparisonNode, property, percent);
            String comparisonName = ((Region) comparisonNode.getData()).getName();
            addSeries(chart, comparisonName, comparisonData);
        }
        else if(!percent)
        {
            SortedMap<Date, Double> trendData = computeTrend(selectedData);
            addSeries(chart, "trend", trendData);
        }

        StreamEx.of(chart.getSeries())
            .map(ChartSeries::getData)
            .remove(Map::isEmpty)
            .flatCollection(Map::keySet)
            .select(String.class)
            .collect(MoreCollectors.minMax(Comparator.naturalOrder(), (min, max) ->
            {
                xAxis.setMin(min);
                xAxis.setMax(max);
                return "";
            }));

        StreamEx.of(chart.getSeries())
            .map(ChartSeries::getData)
            .remove(Map::isEmpty)
            .mapToInt(Map::size)
            .map(x -> Math.min(x, 31))
            .max()
            .ifPresent(xAxis::setTickCount);

        return chart;
    }

    private static ChartSeries addSeries(CartesianChartModel chart, String title, SortedMap<Date, Double> data)
    {
        ChartSeries series = new LineChartSeries(title);

        EntryStream.of(data)
            .mapKeys(AXIS_DATE_FORMAT::format)
            .forKeyValue(series::set);

        chart.addSeries(series);

        return series;
    }


    private SortedMap<Date, Double> buildData(TreeNode node, String property, boolean percent)
    {
        Region region = (Region) node.getData();
        Set<Record> records = region.getRecords();

        SortedMap<Date, Double> data = StreamEx.of(records)
            .mapToEntry(Record::getRegistered, x -> x)
            .removeKeys(x -> x.before(startDate) || x.after(endDate))
            .mapValues(r -> Faces.<Number> resolveExpressionGet(r, property).doubleValue())
            .mapValues(x -> percent ? x * 100 : x)
            .toSortedMap();

        return data;
    }

    public static SortedMap<Date, Double> computeTrend(SortedMap<Date, Double> data)
    {
        int n = data.size();
        Date s = data.firstKey();
        Date f = data.lastKey();

        double[] x = StreamEx.ofKeys(data)
            .mapToDouble(d -> Duration.ofMillis(d.getTime() - s.getTime()).toDays())
            .toArray();

        double[] y = StreamEx.ofValues(data)
            .mapToDouble(Double::doubleValue)
            .toArray();

        double xySum = sum(n, i -> x[i] * y[i]);
        double xSum = sum(n, i -> x[i]);
        double ySum = sum(n, i -> y[i]);
        double x2Sum = sum(n, i -> x[i] * x[i]);

        double alpha = (n * xySum - xSum * ySum) / (n * x2Sum - xSum * xSum);
        double beta = (ySum - alpha * xSum) / n;


        SortedMap<Date, Double> trend = new TreeMap<>();
        trend.put(s, beta);
        trend.put(f, beta + (alpha * Duration.ofMillis(f.getTime() - s.getTime()).toDays()));

        return trend;
    }

    private static double sum(int n, IntToDoubleFunction mapper)
    {
        double sum = 0;
        for(int i = 0; i < n; i++)
        {
            sum += mapper.applyAsDouble(i);
        }
        return sum;
    }

    public void lastWeek()
    {
        startDate = DateUtils.addDays(maxDate, -7);
        endDate = maxDate;
        buildCharts();
    }

    public void last2Weeks()
    {
        startDate = DateUtils.addDays(maxDate, -14);
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
