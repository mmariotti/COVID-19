package it.mmariotti.covid19.service.fetch;

import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import javax.ejb.Stateless;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.mmariotti.covid19.model.RecordProperty;
import it.mmariotti.covid19.service.FetchService;
import it.mmariotti.covid19.util.Util;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;


@Stateless
public class WorldFetchService extends FetchService
{
	private static final Logger logger = LoggerFactory.getLogger(WorldFetchService.class);

	private static final String URL_TEMPLATE = "https://raw.githubusercontent.com/CSSEGISandData/COVID-19/master/csse_covid_19_data/csse_covid_19_daily_reports/%s.csv";

	private static final DateFormat URL_DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy");

	private static final String[] DATE_PATTERNS = {
		"yyyy-MM-dd HH:mm:ss",
		"yyyy-MM-dd'T'HH:mm:ss",
		"M/d/yy HH:mm"
	};

	private static final Map<RecordProperty, String> MAPPING = EntryStream.of(
		RecordProperty.confirmed, "Confirmed",
		RecordProperty.deceased, "Deaths",
		RecordProperty.recovered, "Recovered")
		.toImmutableMap();

	private static final char SPLIT_SEPARATOR = ',';

	private static final Properties TRANSLATION = Util.loadProperties("translation");

	private static final Properties US_STATES = Util.loadProperties("us_states");

	@Override
	protected Date extractRegistered(CSVRecord line)
	{
		Date registered = getDate(line, "Last Update", DATE_PATTERNS);
		if(registered == null)
		{
			registered = getDate(line, "Last_Update", DATE_PATTERNS);
		}

		return registered;
	}

	@Override
	protected String[] extractRegionChain(CSVRecord line)
	{
		String combinedKey = get(line, "Combined_Key");
		if(combinedKey == null)
		{
			String containerName = get(line, "Country/Region", "Country_Region");
			String regionName = get(line, "Province/State", "Province_State");

			combinedKey = StreamEx.of(regionName, containerName)
				.nonNull()
				.collapse(Objects::equals)
				.joining(SPLIT_SEPARATOR + " ");
		}

		combinedKey = normalize(StringUtils.strip(combinedKey, String.valueOf(SPLIT_SEPARATOR)), true, false);

		String[] tokens = StreamEx.ofNullable(combinedKey)
			.flatArray(x -> StringUtils.splitPreserveAllTokens(x, SPLIT_SEPARATOR))
			.map(x -> normalize(x, false, true))
			.toArray(String[]::new);

		if(tokens == null || tokens.length == 0)
		{
			return ArrayUtils.EMPTY_STRING_ARRAY;
		}

		ArrayUtils.reverse(tokens);

		if(tokens.length > 1)
		{
			if("US".equals(tokens[0]))
			{
				String stateCode = tokens[1];
				if(StringUtils.length(stateCode) == 2 && !"US".equals(stateCode))
				{
					tokens[1] = US_STATES.getProperty(stateCode, stateCode);
				}
			}
			else if("Mainland China".equals(tokens[0]))
			{
				tokens[0] = "China";
			}
		}

		String[] result = StreamEx.of(tokens)
			.flatArray(x -> StringUtils.splitPreserveAllTokens(x, SPLIT_SEPARATOR))
			.nonNull()
			.collapse(Objects::equals)
			.toArray(String[]::new);

		return result;
	}

	@Override
	protected Map<RecordProperty, String> getMapping()
	{
		return MAPPING;
	}

	@Override
	protected BigDecimal extractLatitude(CSVRecord line)
	{
		BigDecimal latitude = getBigDecimal(line, "Latitude");
		if(latitude == null)
		{
			latitude = getBigDecimal(line, "Lat");
		}
		return latitude;
	}

	@Override
	protected BigDecimal extractLongitude(CSVRecord line)
	{
		BigDecimal longitude = getBigDecimal(line, "Longitude");
		if(longitude == null)
		{
			longitude = getBigDecimal(line, "Long_");
		}
		return longitude;
	}

	private static String normalize(String str, boolean translate, boolean defaultIfNull)
	{
		String result = StringUtils.trimToNull(StringUtils.stripAccents(str));

		if(translate && result != null)
		{
			result = TRANSLATION.getProperty(result, result);
		}

		if(defaultIfNull && result == null)
		{
			result = "unknown";
		}

		return result;
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
