package it.mmariotti.covid19.service;

import java.io.Reader;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Future;

import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.FlushModeType;
import javax.persistence.PersistenceContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;

import it.mmariotti.covid19.model.Record;
import it.mmariotti.covid19.model.RecordId;
import it.mmariotti.covid19.model.RecordProperty;
import it.mmariotti.covid19.model.Region;
import it.mmariotti.covid19.model.Source;
import it.mmariotti.covid19.util.Util;
import one.util.streamex.StreamEx;


public abstract class FetchService
{
    public static final String SEPARATOR = "; ";

    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    private static final Properties POPULATION = Util.loadProperties("population");

    @Inject
    protected TestedService testedService;

    @PersistenceContext
    protected EntityManager em;

    protected abstract Logger getLogger();

    protected abstract String getUrlTemplate();

    protected abstract DateFormat getUrlDateFormat();

    protected abstract Date extractRegistered(CSVRecord line);

    protected abstract String[] extractRegionChain(CSVRecord line);

    protected abstract BigDecimal extractLatitude(CSVRecord line);

    protected abstract BigDecimal extractLongitude(CSVRecord line);

    protected abstract Map<RecordProperty, String> getMapping();


    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public Future<DataContent> loadDataContent(Date date)
    {
        getLogger().info("loadDataContent() date: {}", LOG_DATE_FORMAT.format(date));

        try
        {
            URL url = getSourceURL(date);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int code = connection.getResponseCode();
            if(code != 200)
            {
                return new AsyncResult<>(null);
            }

            String contentType = connection.getContentType();
            String charset = StringUtils.trimToNull(StringUtils.substringAfterLast(contentType, "charset="));
            if(charset == null)
            {
                charset = StandardCharsets.UTF_8.name();
            }

            byte[] contentBytes = IOUtils.toByteArray(url);

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digestBytes = md.digest(contentBytes);
            String digest = DatatypeConverter.printHexBinary(digestBytes);

            return new AsyncResult<>(new DataContent(url, date, contentBytes, charset, digest));
        }
        catch(Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public Collection<Record> fetch(DataContent content)
    {
        getLogger().info("fetch() date: {}", LOG_DATE_FORMAT.format(content.getDate()));

        em.setFlushMode(FlushModeType.COMMIT);

        try
        {
            URL url = content.getUrl();
            String digest = content.getDigest();

            Source source = em.find(Source.class, url.toString());
            if(source == null)
            {
                source = new Source();
                source.setName(url.toString());
                source.setDigest(digest);
                em.persist(source);
            }
            else
            {
                if(digest.equals(source.getDigest()))
                {
                    getLogger().info("fetch() source already present");
                    return Collections.emptySet();
                }

                source.setDigest(digest);
            }

            Map<RecordProperty, String> mapping = getMapping();
            EnumSet<RecordProperty> unmappedProperties = StreamEx.of(RecordProperty.getMain())
                .remove(mapping::containsKey)
                .toCollection(() -> EnumSet.noneOf(RecordProperty.class));

            Map<Date, Map<String, Long>> testedMap = testedService.getTestedMap();

            Map<RecordId, Record> recordMap = new LinkedHashMap<>();

            try(Reader reader = content.getReader())
            {
                Iterable<CSVRecord> lines = CSVFormat.DEFAULT
                    .withTrim()
                    .withIgnoreEmptyLines()
                    .withIgnoreHeaderCase()
                    .withIgnoreSurroundingSpaces()
                    .withFirstRecordAsHeader()
                    .parse(reader);

                for(CSVRecord line : lines)
                {
                    Date registered = Optional.ofNullable(extractRegistered(line))
                        .map(x -> DateUtils.truncate(x, Calendar.DATE))
                        .orElse(null);

                    if(registered == null)
                    {
                        continue;
                    }

                    Region region = buildRegion(line);
                    if(region == null)
                    {
                        continue;
                    }

                    Record record = recordMap.computeIfAbsent(new RecordId(region, registered), x ->
                    {
                        Record r = Record.buildRecord(em, x);
                        region.getRecords().add(r);
                        return r;
                    });


                    for(Entry<RecordProperty, String> entry : mapping.entrySet())
                    {
                        RecordProperty property = entry.getKey();
                        String header = entry.getValue();

                        long value = getLong(line, header);
                        property.set(record, value);
                    }

                    if(!mapping.containsKey(RecordProperty.tested))
                    {
                        Map<String, Long> registeredTestedMap = testedMap.get(registered);
                        if(registeredTestedMap != null)
                        {
                            String regionName = region.getName();
                            long tested = registeredTestedMap.getOrDefault(regionName, 0L);
                            if(tested != 0)
                            {
                                record.setTested(tested);
                            }
                        }
                    }
                }
            }

            return StreamEx.ofValues(recordMap)
                .map(record ->
                {
                    if(!em.contains(record))
                    {
                        Record previous = record.getPrevious();
                        for(RecordProperty property : unmappedProperties)
                        {
                            long value = property.get(previous);
                            if(value != 0)
                            {
                                property.set(record, value);
                            }
                        }

                        record.compute();

                        if(!record.hasChanges())
                        {
                            return null;
                        }

                        em.persist(record);
                    }
                    else
                    {
                        record.compute();
                    }

                    return record;
                })
                .nonNull()
                .toList();
        }
        catch(RuntimeException e)
        {
            throw e;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    private Region buildRegion(CSVRecord line)
    {
        String[] regionChain = extractRegionChain(line);

        if(regionChain == null || regionChain.length == 0)
        {
            return null;
        }

        Region container = em.find(Region.class, Region.WORLD);
        if(container == null)
        {
            container = new Region(Region.WORLD);
            setPopulation(container);
            em.persist(container);
        }

        String fullName = regionChain[0];

        for(int i = 1; i < regionChain.length; i++)
        {
            Region region = em.find(Region.class, fullName);
            if(region == null)
            {
                region = new Region(fullName);
                region.setContainer(container);

                setPopulation(region);
                em.persist(region);

                container.getSubRegions().add(region);
            }

            container = region;
            fullName += SEPARATOR + regionChain[i];
        }

        BigDecimal latitude = extractLatitude(line);
        BigDecimal longitude = extractLongitude(line);

        Region region = em.find(Region.class, fullName);
        if(region == null)
        {
            region = new Region(fullName);
            region.setContainer(container);

            if(latitude != null && !BigDecimal.ZERO.equals(latitude))
            {
                region.setLatitude(latitude);
            }

            if(longitude != null && !BigDecimal.ZERO.equals(longitude))
            {
                region.setLongitude(longitude);
            }

            setPopulation(region);
            em.persist(region);

            container.getSubRegions().add(region);
        }
        else
        {
            if(latitude != null && !BigDecimal.ZERO.equals(latitude) && !latitude.equals(region.getLatitude()))
            {
                region.setLatitude(latitude);
            }

            if(longitude != null && !BigDecimal.ZERO.equals(longitude) && !longitude.equals(region.getLongitude()))
            {
                region.setLongitude(longitude);
            }
        }

        return region;
    }

    private URL getSourceURL(Date date)
    {
        try
        {
            String param = getUrlDateFormat().format(date);
            String spec = String.format(getUrlTemplate(), param);
            URL url = new URL(spec);
            return url;
        }
        catch(Exception e)
        {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    protected static String get(CSVRecord values, String key)
    {
        return values.isSet(key) ? StringUtils.trimToNull(values.get(key)) : null;
    }

    protected static String get(CSVRecord values, String... keys)
    {
        for(String key : keys)
        {
            String value = get(values, key);
            if(value != null)
            {
                return value;
            }
        }

        return null;
    }

    protected static Date getDate(CSVRecord values, String key, String... patterns)
    {
        String value = get(values, key);
        if(StringUtils.isNotBlank(value))
        {
            try
            {
                return DateUtils.parseDate(value, patterns);
            }
            catch(ParseException e)
            {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        return null;
    }

    protected static long getLong(CSVRecord values, String key)
    {
        String value = get(values, key);
        return NumberUtils.toLong(value);
    }

    protected static double getDouble(CSVRecord values, String key)
    {
        String value = get(values, key);
        return NumberUtils.toDouble(value);
    }

    protected static BigDecimal getBigDecimal(CSVRecord values, String key)
    {
        String value = get(values, key);
        return StringUtils.isBlank(value) ? null : new BigDecimal(value);
    }

    private static void setPopulation(Region region)
    {
        String name = region.getName();
        String value = POPULATION.getProperty(name);
        if(StringUtils.isNotBlank(value))
        {
            long population = NumberUtils.toLong(value);
            region.setPopulation(population);
        }
    }
}
