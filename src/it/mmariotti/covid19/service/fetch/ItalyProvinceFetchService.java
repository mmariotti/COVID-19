package it.mmariotti.covid19.service.fetch;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import javax.ejb.Stateless;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.mmariotti.covid19.model.RecordProperty;
import it.mmariotti.covid19.service.FetchService;
import one.util.streamex.EntryStream;


@Stateless
public class ItalyProvinceFetchService extends FetchService
{
	private static final Logger logger = LoggerFactory.getLogger(ItalyProvinceFetchService.class);

	private static final String URL_TEMPLATE = "https://raw.githubusercontent.com/pcm-dpc/COVID-19/master/dati-province/dpc-covid19-ita-province-%s.csv";

	private static final DateFormat URL_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

	private static final String COUNTRY_NAME = "Italy";

	private static final String[] DATE_PATTERNS = {
		"yyyy-MM-dd'T'HH:mm:ss"
	};

	private static final Map<RecordProperty, String> MAPPING = EntryStream.of(
		RecordProperty.confirmed, "totale_casi")
		.toImmutableMap();

	@Override
	protected String[] extractRegionChain(CSVRecord line)
	{
		String provinceCode = get(line, "sigla_provincia");
		if(StringUtils.isBlank(provinceCode))
		{
			return null;
		}

		return new String[] {
			COUNTRY_NAME,
			get(line, "denominazione_regione"),
			get(line, "denominazione_provincia")
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
