package it.mmariotti.covid19.model;

import static javax.persistence.FetchType.EAGER;
import java.io.Serializable;
import java.util.Date;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotNull;


@Embeddable
public class RecordId implements Serializable
{
	private static final long serialVersionUID = 1L;

	@NotNull
	@ManyToOne(optional = false, fetch = EAGER)
	@JoinColumn(name = "REGION_NAME", nullable = false, columnDefinition = "VARCHAR(255) BINARY NOT NULL")
	protected Region region;

	@NotNull
	@Temporal(TemporalType.DATE)
	@Column(name = "REGISTERED", nullable = false)
	protected Date registered;

	public RecordId()
	{
		super();
	}

	public RecordId(Region region, Date registered)
	{
		this();
		this.region = region;
		this.registered = registered;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(getRegistered(), getRegion());
	}

	@Override
	public boolean equals(Object obj)
	{
		if(obj == this)
		{
			return true;
		}

		if(obj == null || !(obj instanceof RecordId))
		{
			return false;
		}

		return Objects.equals(getRegistered(), ((RecordId) obj).getRegistered())
			&& Objects.equals(getRegion(), ((RecordId) obj).getRegion());
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "{" + region.getName() + "|" + registered + "}";
	}

	public Region getRegion()
	{
		return region;
	}

	public void setRegion(Region region)
	{
		this.region = region;
	}

	public Date getRegistered()
	{
		return registered;
	}

	public void setRegistered(Date registered)
	{
		this.registered = registered;
	}


}
