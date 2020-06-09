package it.mmariotti.covid19.service;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.interceptor.ExcludeDefaultInterceptors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.mmariotti.covid19.util.Util;
import one.util.streamex.IntStreamEx;


@Singleton
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@ExcludeDefaultInterceptors
public class TestedService
{
    private static final Logger logger = LoggerFactory.getLogger(TestedService.class);

    private static final Properties TRANSLATION = Util.loadProperties("translation");

    private Date fetched = new Date(0);

    private Map<Date, Map<String, Long>> testedMap = new TreeMap<>();


    public Map<Date, Map<String, Long>> getTestedMap()
    {
        if(TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - fetched.getTime()) > 50)
        {
            try
            {
                Map<Date, Map<String, Long>> map = fetchTested();
                testedMap.putAll(map);
            }
            catch(Exception e)
            {
                logger.error("cannot fetch testedMap", e);
            }

            fetched = new Date();
        }

        return Collections.unmodifiableMap(testedMap);
    }

    private static Map<Date, Map<String, Long>> fetchTested() throws Exception
    {
        Map<Date, Map<String, Long>> result = new HashMap<>();

        Document doc = Jsoup.connect("https://www.worldometers.info/coronavirus/").get();

        Map<String, Long> todayMap = fetchTested(doc, "#main_table_countries_today");
        Map<String, Long> yesterdayMap = fetchTested(doc, "#main_table_countries_yesterday");
        Map<String, Long> yesterday2Map = fetchTested(doc, "#main_table_countries_yesterday2");

        Date today = DateUtils.truncate(new Date(), Calendar.DATE);
        result.put(today, todayMap);

        Date yesterday = DateUtils.addDays(today, -1);
        result.put(yesterday, yesterdayMap);

        Date yesterday2 = DateUtils.addDays(today, -2);
        result.put(yesterday2, yesterday2Map);

        return result;
    }

    private static Map<String, Long> fetchTested(Document doc, String selector) throws Exception
    {
        Map<String, Long> result = new HashMap<>();

        Elements th = doc.select(selector + " th");

        int index = IntStreamEx.range(th.size())
            .mapToEntry(i -> i + 1, i -> th.get(i).text())
            .filterValues("total tests"::equalsIgnoreCase)
            .keys()
            .findAny()
            .orElse(-1);

        if(index == -1)
        {
            return result;
        }

        Elements rows = doc.select(selector + " tbody tr");
        for(Element row : rows)
        {
            Element nameElem = row.selectFirst("td a");
            if(nameElem == null)
            {
                continue;
            }

            String name = StringUtils.trimToNull(nameElem.text());
            if(name == null)
            {
                continue;
            }

            name = TRANSLATION.getProperty(name, name);
            if(StringUtils.isBlank(name))
            {
                continue;
            }

            Element testedElem = row.selectFirst("td:nth-child(" + index + ")");
            if(testedElem == null)
            {
                continue;
            }

            long tested = NumberUtils.toLong(StringUtils.remove(testedElem.text(), ','));
            if(tested == 0)
            {
                continue;
            }

            result.put(name, tested);
        }

        return result;
    }
}
