package it.mmariotti.covid19.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Future;

import javax.ejb.Asynchronous;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.mail.smtp.SMTPTransport;

import it.mmariotti.covid19.model.Record;
import one.util.streamex.StreamEx;


@Singleton
public class ScheduleService
{
    private static final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static final Date INITIAL_DATE = new GregorianCalendar(2020, Calendar.JANUARY, 22).getTime();

    @Inject
    @Any
    private Instance<FetchService> fetchers;

    @Inject
    private ApplicationService application;

    @Inject
    private AggregatorService aggregator;


    @Schedule(second = "40", minute = "40", hour = "*", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void scheduledFetch()
    {
        executeFetch();
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void executeFetch()
    {
        logger.info("executeFetch() begin");

        application.loadSourceMap();

        Date date = INITIAL_DATE;
        Date now = new Date();

        List<FetchService> fetcherList = StreamEx.of(fetchers.iterator())
            .sortedBy(FetchService::getExecutionOrder)
            .toList();

        try
        {
            List<String> fetched = new ArrayList<>();

            while(!now.before(date))
            {
                Date currentDate = date;

                logger.info("begin computing {}", DATE_FORMAT.format(currentDate));
                long millis = System.currentTimeMillis();

                Map<FetchService, Future<DataContent>> contentMap = StreamEx.of(fetcherList)
                    .mapToEntry(x -> x.loadDataContent(currentDate))
                    .nonNullValues()
                    .toCustomMap(LinkedHashMap::new);

                Set<Record> records = new LinkedHashSet<>();

                for(Entry<FetchService, Future<DataContent>> entry : contentMap.entrySet())
                {
                    FetchService fetcher = entry.getKey();
                    DataContent content = entry.getValue().get();
                    if(content == null)
                    {
                        continue;
                    }

                    Collection<Record> fetchedRecords = fetcher.fetch(content);
                    if(!fetchedRecords.isEmpty())
                    {
                        String msg = String.format("%s - %s - %,d",
                            DATE_FORMAT.format(date),
                            fetcher.getClass().getSuperclass().getSimpleName(),
                            fetchedRecords.size());

                        fetched.add(msg);
                        records.addAll(fetchedRecords);
                    }
                }

                aggregator.compute(records);

                logger.info("end computing {} - {}", DATE_FORMAT.format(currentDate), DurationFormatUtils.formatDuration(System.currentTimeMillis() - millis, "d'd' H'h' m'm' s's' S'ms'"));

                date = DateUtils.addDays(date, 1);
            }

            application.buildLatestRecordMap();

            if(!fetched.isEmpty())
            {
                sendMail("UPDATE", StringUtils.join(fetched, "\r\n"));
            }
        }
        catch(Exception e)
        {
            sendMail("ERROR", "date: " + date + "\r\n\r\n" + ExceptionUtils.getStackTrace(e));
        }
        finally
        {
            logger.info("executeFetch() end");
        }
    }

    private static void sendMail(String subject, String body)
    {
        try
        {
            Properties prop = new Properties();
            try(InputStream in = new FileInputStream("c:\\shape\\covid19-mail.properties"))
            {
                prop.load(in);
            }

            String username = prop.getProperty("mail.smtp.user");
            String password = prop.getProperty("mail.smtp.pass");
            String fromAddress = prop.getProperty("mail.smtp.from.address");
            String fromPersonal = prop.getProperty("mail.smtp.from.personal");
            String recipientsTo = prop.getProperty("mail.smtp.to");

            Session session = Session.getInstance(prop, null);
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(fromAddress, fromPersonal));
            msg.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientsTo, false));
            msg.setSubject(subject);
            msg.setText("Shape COVID-19 data notification\r\n\r\nhttp://covid.shapeitalia.com\r\n\r\n" + body);
            msg.setSentDate(new Date());

            SMTPTransport t = (SMTPTransport) session.getTransport("smtp");
            t.connect(username, password);
            try
            {
                t.sendMessage(msg, msg.getAllRecipients());

                logger.info("sendMail(): response: " + t.getLastServerResponse());
            }
            finally
            {
                t.close();
            }
        }
        catch(Exception e)
        {
            logger.warn(e.getMessage(), e);
        }
    }
}
