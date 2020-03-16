package it.mmariotti.covid19.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.commons.lang3.StringUtils;


@Entity
@Table(name = "REGIONE",
	indexes = {
		@Index(name = "IX_REGIONE_DATA", columnList = "DATA"),
		@Index(name = "IX_REGIONE_DENOMINAZIONE", columnList = "DENOMINAZIONE"),
	})
public class Regione implements Serializable
{
	private static final long serialVersionUID = 1L;

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

	@Id
	@Temporal(TemporalType.TIMESTAMP)
	private Date data;

	private String stato;

	private int codice;

	@Id
	private String denominazione;

	private BigDecimal latitudine;

	private BigDecimal longitudine;

	private int ricoveratiNormali;

	private int ricoveratiNormaliDelta;

	private int ricoveratiIntensiva;

	private int ricoveratiIntensivaDelta;

	private int ospedalizzati;

	private int ospedalizzatiDelta;

	private int isolamento;

	private int isolamentoDelta;

	private int positiviAttuali;

	private int positiviAttualiDelta;

	private int guariti;

	private int guaritiDelta;

	private int deceduti;

	private int decedutiDelta;

	private int positiviTotali;

	private int positiviTotaliDelta;

	private int tamponi;

	private int tamponiDelta;

	public Regione()
	{
		super();
	}

	public static Regione build(String line)
	{
		if(StringUtils.isBlank(line))
		{
			return null;
		}

		try
		{
			String[] split = StringUtils.split(line, ',');

			int i = 0;
			Regione r = new Regione();
			r.data = DATE_FORMAT.parse(split[i++]);
			r.stato = split[i++];
			r.codice = Integer.parseInt(split[i++]);
			r.denominazione = split[i++];
			r.latitudine = new BigDecimal(split[i++]);
			r.longitudine = new BigDecimal(split[i++]);

			r.ricoveratiNormali = Integer.parseInt(split[i++]);
			r.ricoveratiIntensiva = Integer.parseInt(split[i++]);
			r.ospedalizzati = Integer.parseInt(split[i++]);
			r.isolamento = Integer.parseInt(split[i++]);
			r.positiviAttuali = Integer.parseInt(split[i++]);
			i++;
			r.guariti = Integer.parseInt(split[i++]);
			r.deceduti = Integer.parseInt(split[i++]);
			r.positiviTotali = Integer.parseInt(split[i++]);
			r.tamponi = Integer.parseInt(split[i++]);

			return r;
		}
		catch(Exception e)
		{
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public void update(Regione previous)
	{
		if(previous == null)
		{
			update(new Regione());
			return;
		}

		ricoveratiNormaliDelta = ricoveratiNormali - previous.ricoveratiNormali;
		ricoveratiIntensivaDelta = ricoveratiIntensiva - previous.ricoveratiIntensiva;
		ospedalizzatiDelta = ospedalizzati - previous.ospedalizzati;
		isolamentoDelta = isolamento - previous.isolamento;
		positiviAttualiDelta = positiviAttuali - previous.positiviAttuali;
		guaritiDelta = guariti - previous.guariti;
		decedutiDelta = deceduti - previous.deceduti;
		positiviTotaliDelta = positiviTotali - previous.positiviTotali;
		tamponiDelta = tamponi - previous.tamponi;
	}

	public Regione add(Regione regione)
	{
		ricoveratiNormali += regione.ricoveratiNormali;
		ricoveratiIntensiva += regione.ricoveratiIntensiva;
		ospedalizzati += regione.ospedalizzati;
		isolamento += regione.isolamento;
		positiviAttuali += regione.positiviAttuali;
		guariti += regione.guariti;
		deceduti += regione.deceduti;
		positiviTotali += regione.positiviTotali;
		tamponi += regione.tamponi;

		ricoveratiNormaliDelta += regione.ricoveratiNormaliDelta;
		ricoveratiIntensivaDelta += regione.ricoveratiIntensivaDelta;
		ospedalizzatiDelta += regione.ospedalizzatiDelta;
		isolamentoDelta += regione.isolamentoDelta;
		positiviAttualiDelta += regione.positiviAttualiDelta;
		guaritiDelta += regione.guaritiDelta;
		decedutiDelta += regione.decedutiDelta;
		positiviTotaliDelta += regione.positiviTotaliDelta;
		tamponiDelta += regione.tamponiDelta;

		return this;
	}

	public Date getData()
	{
		return data;
	}

	public void setData(Date data)
	{
		this.data = data;
	}

	public String getStato()
	{
		return stato;
	}

	public void setStato(String stato)
	{
		this.stato = stato;
	}

	public int getCodice()
	{
		return codice;
	}

	public void setCodice(int codice)
	{
		this.codice = codice;
	}

	public String getDenominazione()
	{
		return denominazione;
	}

	public void setDenominazione(String denominazione)
	{
		this.denominazione = denominazione;
	}

	public BigDecimal getLatitudine()
	{
		return latitudine;
	}

	public void setLatitudine(BigDecimal latitudine)
	{
		this.latitudine = latitudine;
	}

	public BigDecimal getLongitudine()
	{
		return longitudine;
	}

	public void setLongitudine(BigDecimal longitudine)
	{
		this.longitudine = longitudine;
	}

	public int getRicoveratiNormali()
	{
		return ricoveratiNormali;
	}

	public void setRicoveratiNormali(int ricoveratiNormali)
	{
		this.ricoveratiNormali = ricoveratiNormali;
	}

	public int getRicoveratiNormaliDelta()
	{
		return ricoveratiNormaliDelta;
	}

	public void setRicoveratiNormaliDelta(int ricoveratiNormaliDelta)
	{
		this.ricoveratiNormaliDelta = ricoveratiNormaliDelta;
	}

	public int getRicoveratiIntensiva()
	{
		return ricoveratiIntensiva;
	}

	public void setRicoveratiIntensiva(int ricoveratiIntensiva)
	{
		this.ricoveratiIntensiva = ricoveratiIntensiva;
	}

	public int getRicoveratiIntensivaDelta()
	{
		return ricoveratiIntensivaDelta;
	}

	public void setRicoveratiIntensivaDelta(int ricoveratiIntensivaDelta)
	{
		this.ricoveratiIntensivaDelta = ricoveratiIntensivaDelta;
	}

	public int getOspedalizzati()
	{
		return ospedalizzati;
	}

	public void setOspedalizzati(int ospedalizzati)
	{
		this.ospedalizzati = ospedalizzati;
	}

	public int getOspedalizzatiDelta()
	{
		return ospedalizzatiDelta;
	}

	public void setOspedalizzatiDelta(int ospedalizzatiDelta)
	{
		this.ospedalizzatiDelta = ospedalizzatiDelta;
	}

	public int getIsolamento()
	{
		return isolamento;
	}

	public void setIsolamento(int isolamento)
	{
		this.isolamento = isolamento;
	}

	public int getIsolamentoDelta()
	{
		return isolamentoDelta;
	}

	public void setIsolamentoDelta(int isolamentoDelta)
	{
		this.isolamentoDelta = isolamentoDelta;
	}

	public int getPositiviAttuali()
	{
		return positiviAttuali;
	}

	public void setPositiviAttuali(int positiviAttuali)
	{
		this.positiviAttuali = positiviAttuali;
	}

	public int getPositiviAttualiDelta()
	{
		return positiviAttualiDelta;
	}

	public void setPositiviAttualiDelta(int positiviAttualiDelta)
	{
		this.positiviAttualiDelta = positiviAttualiDelta;
	}

	public int getGuariti()
	{
		return guariti;
	}

	public void setGuariti(int guariti)
	{
		this.guariti = guariti;
	}

	public int getGuaritiDelta()
	{
		return guaritiDelta;
	}

	public void setGuaritiDelta(int guaritiDelta)
	{
		this.guaritiDelta = guaritiDelta;
	}

	public int getDeceduti()
	{
		return deceduti;
	}

	public void setDeceduti(int deceduti)
	{
		this.deceduti = deceduti;
	}

	public int getDecedutiDelta()
	{
		return decedutiDelta;
	}

	public void setDecedutiDelta(int decedutiDelta)
	{
		this.decedutiDelta = decedutiDelta;
	}

	public int getPositiviTotali()
	{
		return positiviTotali;
	}

	public void setPositiviTotali(int positiviTotali)
	{
		this.positiviTotali = positiviTotali;
	}

	public int getPositiviTotaliDelta()
	{
		return positiviTotaliDelta;
	}

	public void setPositiviTotaliDelta(int positiviTotaliDelta)
	{
		this.positiviTotaliDelta = positiviTotaliDelta;
	}

	public int getTamponi()
	{
		return tamponi;
	}

	public void setTamponi(int tamponi)
	{
		this.tamponi = tamponi;
	}

	public int getTamponiDelta()
	{
		return tamponiDelta;
	}

	public void setTamponiDelta(int tamponiDelta)
	{
		this.tamponiDelta = tamponiDelta;
	}
}
