package it.mmariotti.covid19.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;


@Entity
@Table(name = "SOURCE")
public class Source implements Serializable
{
	private static final long serialVersionUID = 1L;

	@NotNull
	@Id
	@Column(nullable = false)
	private String name;

	@NotNull
	@Column(nullable = false)
	private String digest;

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getDigest()
	{
		return digest;
	}

	public void setDigest(String digest)
	{
		this.digest = digest;
	}
}
