package it.mmariotti.covid19.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap.SimpleEntry;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import org.apache.commons.io.input.BOMInputStream;
import org.apache.commons.lang3.StringUtils;
import one.util.streamex.EntryStream;
import one.util.streamex.StreamEx;


public class Util
{
	public static Properties loadProperties(String name)
	{
		Properties p = new Properties();

		try(InputStream in = Util.class.getResourceAsStream("/" + name + ".properties"))
		{
			p.load(in);
		}
		catch(Exception e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}

		return p;
	}

	public static Map<String, String> loadMap(URL resource, Charset charset)
	{
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(new BOMInputStream(resource.openStream()), charset)))
		{
			return StreamEx.of(reader.lines())
				.map(StringUtils::trimToNull)
				.nonNull()
				.remove(x -> StringUtils.startsWith(x, "#"))
				.mapToEntry(x -> StringUtils.indexOf(x, '='))
				.filterValues(x -> x >= 0)
				.mapKeyValue((s, i) -> new SimpleEntry<>(s.substring(0, i), s.substring(i + 1)))
				.chain(EntryStream::of)
				.mapKeys(StringUtils::trimToEmpty)
				.mapValues(StringUtils::trimToEmpty)
				.toCustomMap(LinkedHashMap::new);
		}
		catch(IOException e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static Map<String, String> loadMap(URL resource)
	{
		return loadMap(resource, StandardCharsets.UTF_8);
	}

	public static Map<String, String> loadMap(String resource)
	{
		return loadMap(Util.class.getResource("/" + resource + ".map"), StandardCharsets.UTF_8);
	}
}
