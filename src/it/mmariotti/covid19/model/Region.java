package it.mmariotti.covid19.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.validation.constraints.NotNull;
import org.hibernate.Hibernate;
import it.mmariotti.covid19.service.DataService;


@Entity
@Table(name = "REGION")
public class Region implements Serializable
{
	private static final long serialVersionUID = 1L;

	public static final String WORLD = "World";

	@NotNull
	@Id
	@Column(nullable = false, columnDefinition = "VARCHAR(255) BINARY")
	private String name;

	@Column(precision = 11, scale = 8)
	private BigDecimal latitude;

	@Column(precision = 11, scale = 8)
	private BigDecimal longitude;

	private long population;

	@ManyToOne
	@JoinColumn(name = "CONTAINER_NAME")
	private Region container;

	@OneToMany(mappedBy = "container")
	@OrderBy("name")
	private Set<Region> subRegions = new LinkedHashSet<>();

	@OneToMany(mappedBy = "id.region")
	@OrderBy("id.registered desc")
	private Set<Record> records = new LinkedHashSet<>();

	@Transient
	private transient Record latestRecord;

	public Region()
	{
		super();
	}

	public Region(String name)
	{
		super();
		this.name = name;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(getName());
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
		{
			return true;
		}

		if(!(obj instanceof Region))
		{
			return false;
		}

		Region other = (Region) obj;

		return Objects.equals(getName(), other.getName());
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "{" + name + "}";
	}

	public Record getLatestRecord()
	{
		if(latestRecord == null)
		{
			if(Hibernate.isInitialized(records) && !records.isEmpty())
			{
				latestRecord = records.iterator().next();
			}
			else
			{
				latestRecord = DataService.lookup().findLatest(this);
			}
		}

		return latestRecord;
	}

	public void setLatestRecord(Record latestRecord)
	{
		this.latestRecord = latestRecord;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public BigDecimal getLatitude()
	{
		return latitude;
	}

	public void setLatitude(BigDecimal latitude)
	{
		this.latitude = latitude;
	}

	public BigDecimal getLongitude()
	{
		return longitude;
	}

	public void setLongitude(BigDecimal longitude)
	{
		this.longitude = longitude;
	}

	public long getPopulation()
	{
		return population;
	}

	public void setPopulation(long population)
	{
		this.population = population;
	}

	public Region getContainer()
	{
		return container;
	}

	public void setContainer(Region container)
	{
		this.container = container;
	}

	public Set<Region> getSubRegions()
	{
		return subRegions;
	}

	public void setSubRegions(Set<Region> subRegions)
	{
		this.subRegions = subRegions;
	}

	public Set<Record> getRecords()
	{
		return records;
	}

	public void setRecords(Set<Record> records)
	{
		this.records = records;
	}
}
