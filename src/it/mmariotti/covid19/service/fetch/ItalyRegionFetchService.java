package it.mmariotti.covid19.service.fetch;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import javax.ejb.Stateless;

import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.mmariotti.covid19.model.Record;
import it.mmariotti.covid19.model.RecordId;
import it.mmariotti.covid19.model.RecordProperty;
import it.mmariotti.covid19.model.Region;
import it.mmariotti.covid19.service.DataContent;
import it.mmariotti.covid19.service.FetchService;
import one.util.streamex.EntryStream;


@Stateless
public class ItalyRegionFetchService extends FetchService
{
    private static final Logger logger = LoggerFactory.getLogger(ItalyRegionFetchService.class);

    private static final int EXECUTION_ORDER = 200;

    private static final String URL_TEMPLATE = "https://raw.githubusercontent.com/pcm-dpc/COVID-19/master/dati-regioni/dpc-covid19-ita-regioni-%s.csv";

    private static final DateFormat URL_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    private static final String COUNTRY_NAME = "Italy";

    private static final String[] DATE_PATTERNS = {
        "yyyy-MM-dd'T'HH:mm:ss"
    };

    private static final Map<RecordProperty, String> MAPPING = EntryStream.of(
        RecordProperty.confirmed, "totale_casi",
        RecordProperty.standardCare, "ricoverati_con_sintomi",
        RecordProperty.intensiveCare, "terapia_intensiva",
        RecordProperty.quarantined, "isolamento_domiciliare",
        RecordProperty.recovered, "dimessi_guariti",
        RecordProperty.deceased, "deceduti",
        RecordProperty.tested, "casi_testati")
        .toImmutableMap();

    @Override
    public Collection<Record> fetch(DataContent content)
    {
        Collection<Record> records = super.fetch(content);

        if(records.isEmpty())
        {
            return records;
        }

        Region region = em.find(Region.class, COUNTRY_NAME);
        Date registered = content.getDate();

        Record record = Record.buildRecord(em, new RecordId(region, registered));
        record.aggregate(records);
        record.setAggregate(false);
        record.compute();

        if(!record.hasChanges())
        {
            return records;
        }

        if(!em.contains(record))
        {
            em.persist(record);
        }

        ArrayList<Record> results = new ArrayList<>(records);
        results.add(record);

        return results;
    }

    @Override
    protected String[] extractRegionChain(CSVRecord line)
    {
        return new String[] {
            COUNTRY_NAME,
            get(line, "denominazione_regione")
        };
    }


    @Override
    protected Date extractRegistered(CSVRecord line)
    {
        return getDate(line, "data", DATE_PATTERNS);
    }

    @Override
    protected BigDecimal extractLatitude(CSVRecord line)
    {
        return getBigDecimal(line, "lat");
    }

    @Override
    protected BigDecimal extractLongitude(CSVRecord line)
    {
        return getBigDecimal(line, "long");
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
