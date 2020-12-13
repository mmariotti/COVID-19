package it.mmariotti.covid19.service.fetch;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.ejb.Stateless;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.mmariotti.covid19.model.RecordProperty;
import it.mmariotti.covid19.service.FetchService;
import one.util.streamex.EntryStream;


@Stateless
public class UnitedStatesFetchService extends FetchService
{
    private static final Logger logger = LoggerFactory.getLogger(UnitedStatesFetchService.class);

    private static final int EXECUTION_ORDER = 400;

    private static final String URL_TEMPLATE = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports_us/%s.csv";

    private static final DateFormat URL_DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy");

    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd HH:mm:ss",
        "M/d/yy H:mm"
    };

    private static final Map<RecordProperty, String> MAPPING = EntryStream.of(
        RecordProperty.confirmed, "Confirmed",
        RecordProperty.deceased, "Deaths",
        RecordProperty.recovered, "Recovered",
        RecordProperty.standardCare, "People_Hospitalized",
        RecordProperty.tested, "People_Tested")
        .toImmutableMap();

    private static final String COUNTRY_NAME = "US";

    @Override
    protected String[] extractRegionChain(CSVRecord line)
    {
        return new String[] {
            COUNTRY_NAME,
            get(line, "Province_State")
        };
    }

    @Override
    protected Date extractRegistered(CSVRecord line)
    {
        return getDate(line, "Last_Update", DATE_PATTERNS);
    }

    @Override
    protected BigDecimal extractLatitude(CSVRecord line)
    {
        return getBigDecimal(line, "Lat");
    }

    @Override
    protected BigDecimal extractLongitude(CSVRecord line)
    {
        return getBigDecimal(line, "Long_");
    }

    @Override
    protected Map<RecordProperty, String> getMapping()
    {
        return MAPPING;
    }

    @Override
    protected Logger getLogger()
    {
        return logger;
    }

    @Override
    public int getExecutionOrder()
    {
        return EXECUTION_ORDER;
    }

    @Override
    protected String getUrlTemplate()
    {
        return URL_TEMPLATE;
    }

    @Override
    protected DateFormat getUrlDateFormat()
    {
        return URL_DATE_FORMAT;
    }
}
