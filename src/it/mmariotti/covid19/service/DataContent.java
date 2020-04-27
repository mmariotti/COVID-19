package it.mmariotti.covid19.service;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Date;
import org.apache.commons.io.input.BOMInputStream;


public class DataContent
{
	private URL url;

	private Date date;

	private byte[] content;

	private String charset;

	private String digest;


	public DataContent(URL url, Date date, byte[] content, String charset, String digest)
	{
		super();
		this.url = url;
		this.date = date;
		this.content = content;
		this.charset = charset;
		this.digest = digest;
	}

	public Reader getReader()
	{
		try
		{
			return new InputStreamReader(new BOMInputStream(new ByteArrayInputStream(content)), charset);
		}
		catch(UnsupportedEncodingException e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public URL getUrl()
	{
		return url;
	}

	public Date getDate()
	{
		return date;
	}

	public byte[] getContent()
	{
		return content;
	}

	public String getCharset()
	{
		return charset;
	}

	public String getDigest()
	{
		return digest;
	}
}
