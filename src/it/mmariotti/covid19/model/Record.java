package it.mmariotti.covid19.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;
import one.util.streamex.StreamEx;


@Entity
@Table(name = "RECORD",
	indexes = {
		@Index(name = "IX_RECORD_REGISTERED", columnList = "REGISTERED"),
		@Index(name = "IX_RECORD_REGION_NAME", columnList = "REGION_NAME")
	})
public class Record implements Serializable
{
	private static final long serialVersionUID = 1L;

	@EmbeddedId
	private RecordId id;

	@Column(nullable = false)
	protected boolean aggregate = false;

	@Transient
	protected Record previous;

	/* values */

	protected long confirmed;

	protected long deceased;

	protected long recovered;

	protected long tested;

	protected long quarantined;

	protected long standardCare;

	protected long intensiveCare;


	/* derived */

	protected long active;

	protected long closed;

	protected long hospitalized;

	protected long workload;


	/* computed */

	protected double lethality;

	protected double lethalityDelta;

	protected double testDensity;

	protected double testDensityDelta;

	protected long hypotheticalInfected;

	protected long hypotheticalInfectedDelta;


	/* delta */

	protected long confirmedDelta;

	protected long deceasedDelta;

	protected long recoveredDelta;

	protected long testedDelta;

	protected long quarantinedDelta;

	protected long standardCareDelta;

	protected long intensiveCareDelta;

	protected long activeDelta;

	protected long closedDelta;

	protected long hospitalizedDelta;

	protected long workloadDelta;


	/* delta percent */

	protected double confirmedDeltaPercent;

	protected double deceasedDeltaPercent;

	protected double recoveredDeltaPercent;

	protected double testedDeltaPercent;

	protected double quarantinedDeltaPercent;

	protected double standardCareDeltaPercent;

	protected double intensiveCareDeltaPercent;

	protected double activeDeltaPercent;

	protected double closedDeltaPercent;

	protected double hospitalizedDeltaPercent;

	protected double workloadDeltaPercent;


	/* values Population percent */

	protected double confirmedPopulationPercent;

	protected double deceasedPopulationPercent;

	protected double recoveredPopulationPercent;

	protected double testedPopulationPercent;

	protected double quarantinedPopulationPercent;

	protected double standardCarePopulationPercent;

	protected double intensiveCarePopulationPercent;

	protected double activePopulationPercent;

	protected double closedPopulationPercent;

	protected double hospitalizedPopulationPercent;

	protected double workloadPopulationPercent;


	/* values delta population percent */

	protected double confirmedDeltaPopulationPercent;

	protected double deceasedDeltaPopulationPercent;

	protected double recoveredDeltaPopulationPercent;

	protected double testedDeltaPopulationPercent;

	protected double quarantinedDeltaPopulationPercent;

	protected double standardCareDeltaPopulationPercent;

	protected double intensiveCareDeltaPopulationPercent;

	protected double activeDeltaPopulationPercent;

	protected double closedDeltaPopulationPercent;

	protected double hospitalizedDeltaPopulationPercent;

	protected double workloadDeltaPopulationPercent;

	public Record()
	{
		super();
	}

	public Record(RecordId id)
	{
		this();
		this.id = id;
	}

	public Record(Region region, Date registered)
	{
		this(new RecordId(region, registered));
	}

	public static Record latestRecord(EntityManager em, Region region, Date registered)
	{
		List<Record> resultList = em.createNamedQuery("latestRecord", Record.class)
			.setParameter("region", region)
			.setParameter("registered", registered)
			.setMaxResults(1)
			.getResultList();

		return resultList.isEmpty() ? null : resultList.get(0);
	}

	public static List<Record> latestSubRecordList(EntityManager em, Region container, Date registered)
	{
		List<Record> subRecords = em.createNamedQuery("latestSubRecordList", Record.class)
			.setParameter("registered", registered)
			.setParameter("container", container)
			.getResultList();

		return subRecords;
	}

	public static List<Record> latestRecordList(EntityManager em)
	{
		List<Record> records = em.createNamedQuery("latestRecordList", Record.class)
			.getResultList();

		return records;
	}

	public static Record buildRecord(EntityManager em, RecordId recordId)
	{
		Region region = recordId.getRegion();
		Date registered = recordId.getRegistered();

		List<Record> resultList = em.createNamedQuery("latestRecord", Record.class)
			.setParameter("region", region)
			.setParameter("registered", registered)
			.setMaxResults(2)
			.getResultList();

		Record previous = resultList.size() > 1 ? resultList.get(1) : new Record();
		Record latest = resultList.size() > 0 ? resultList.get(0) : new Record();
		latest.setPrevious(previous);

		if(Objects.equals(latest.getRegistered(), registered))
		{
			return latest;
		}

		Record record = new Record(recordId);
		record.setPrevious(latest);
		return record;
	}

	public void aggregate(Collection<? extends Record> records)
	{
		aggregate = true;

		StreamEx.of(RecordProperty.values())
			.cross(records)
			.mapToValue(RecordProperty::get)
			.grouping(Collectors.summingLong(x -> x))
			.forEach((k, v) -> k.set(this, v));
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(id);
	}

	@Override
	public boolean equals(Object obj)
	{
		if(this == obj)
		{
			return true;
		}

		if(!(obj instanceof Record))
		{
			return false;
		}

		Record other = (Record) obj;

		return Objects.equals(getId(), other.getId());
	}

	@Override
	public String toString()
	{
		return getClass().getSimpleName() + "{" + getRegion() + "|" + getRegistered() + "}";
	}

	public void compute()
	{
		if(previous == null)
		{
			throw new IllegalStateException("previous");
		}

		long population = getRegion().getPopulation();

		active = confirmed - deceased - recovered;
		closed = deceased + recovered;
		hospitalized = standardCare + intensiveCare;
		workload = hospitalized - closed;

		if(closed != 0)
		{
			lethality = (double) deceased / closed;
		}

		if(tested != 0)
		{
			testDensity = (double) confirmed / tested;
			hypotheticalInfected = (population * confirmed) / tested;
		}

		long confirmed2 = previous.getConfirmed();
		long deceased2 = previous.getDeceased();
		long recovered2 = previous.getRecovered();
		long tested2 = previous.getTested();
		long quarantined2 = previous.getQuarantined();
		long standardCare2 = previous.getStandardCare();
		long intensiveCare2 = previous.getIntensiveCare();
		long active2 = previous.getActive();
		long closed2 = previous.getClosed();
		long hospitalized2 = previous.getHospitalized();
		long workload2 = previous.getWorkload();
		double lethality2 = previous.getLethality();
		double testDensity2 = previous.getTestDensity();
		long hypotheticalInfected2 = previous.getHypotheticalInfected();

		confirmedDelta = confirmed - confirmed2;
		deceasedDelta = deceased - deceased2;
		recoveredDelta = recovered - recovered2;
		testedDelta = tested - tested2;
		quarantinedDelta = quarantined - quarantined2;
		standardCareDelta = standardCare - standardCare2;
		intensiveCareDelta = intensiveCare - intensiveCare2;
		activeDelta = active - active2;
		closedDelta = closed - closed2;
		hospitalizedDelta = hospitalized - hospitalized2;
		workloadDelta = workload - workload2;
		lethalityDelta = lethality - lethality2;
		testDensityDelta = testDensity - testDensity2;
		hypotheticalInfectedDelta = hypotheticalInfected - hypotheticalInfected2;


		if(confirmed2 != 0)
		{
			confirmedDeltaPercent = (double) confirmedDelta / confirmed2;
		}

		if(deceased2 != 0)
		{
			deceasedDeltaPercent = (double) deceasedDelta / deceased2;
		}

		if(recovered2 != 0)
		{
			recoveredDeltaPercent = (double) recoveredDelta / recovered2;
		}

		if(tested2 != 0)
		{
			testedDeltaPercent = (double) testedDelta / tested2;
		}

		if(quarantined2 != 0)
		{
			quarantinedDeltaPercent = (double) quarantinedDelta / quarantined2;
		}

		if(standardCare2 != 0)
		{
			standardCareDeltaPercent = (double) standardCareDelta / standardCare2;
		}

		if(intensiveCare2 != 0)
		{
			intensiveCareDeltaPercent = (double) intensiveCareDelta / intensiveCare2;
		}

		if(active2 != 0)
		{
			activeDeltaPercent = (double) activeDelta / active2;
		}

		if(closed2 != 0)
		{
			closedDeltaPercent = (double) closedDelta / closed2;
		}

		if(hospitalized2 != 0)
		{
			hospitalizedDeltaPercent = (double) hospitalizedDelta / hospitalized2;
		}

		if(workload2 != 0)
		{
			workloadDeltaPercent = workloadDelta / workload2;
		}


		if(population != 0)
		{
			confirmedPopulationPercent = (double) confirmed / population;
			deceasedPopulationPercent = (double) deceased / population;
			recoveredPopulationPercent = (double) recovered / population;
			testedPopulationPercent = (double) tested / population;
			quarantinedPopulationPercent = (double) quarantined / population;
			standardCarePopulationPercent = (double) standardCare / population;
			intensiveCarePopulationPercent = (double) intensiveCare / population;
			activePopulationPercent = (double) active / population;
			closedPopulationPercent = (double) closed / population;
			hospitalizedPopulationPercent = (double) hospitalized / population;
			workloadPopulationPercent = (double) workload / population;

			confirmedDeltaPopulationPercent = (double) confirmedDelta / population;
			deceasedDeltaPopulationPercent = (double) deceasedDelta / population;
			recoveredDeltaPopulationPercent = (double) recoveredDelta / population;
			testedDeltaPopulationPercent = (double) testedDelta / population;
			quarantinedDeltaPopulationPercent = (double) quarantinedDelta / population;
			standardCareDeltaPopulationPercent = (double) standardCareDelta / population;
			intensiveCareDeltaPopulationPercent = (double) intensiveCareDelta / population;
			activeDeltaPopulationPercent = (double) activeDelta / population;
			closedDeltaPopulationPercent = (double) closedDelta / population;
			hospitalizedDeltaPopulationPercent = (double) hospitalizedDelta / population;
			workloadDeltaPopulationPercent = (double) workloadDelta / population;
		}
	}

	public RecordId getId()
	{
		return id;
	}

	public void setId(RecordId id)
	{
		this.id = id;
	}

	public Region getRegion()
	{
		return id == null ? null : id.getRegion();
	}

	public void setRegion(Region region)
	{
		if(id == null)
		{
			id = new RecordId();
		}

		id.setRegion(region);
	}

	public Date getRegistered()
	{
		return id == null ? null : id.getRegistered();
	}

	public void setRegistered(Date registered)
	{
		if(id == null)
		{
			id = new RecordId();
		}

		id.setRegistered(registered);
	}

	public boolean isAggregate()
	{
		return aggregate;
	}

	public void setAggregate(boolean aggregate)
	{
		this.aggregate = aggregate;
	}

	public Record getPrevious()
	{
		return previous;
	}

	public void setPrevious(Record previous)
	{
		this.previous = previous;
	}

	public long getConfirmed()
	{
		return confirmed;
	}

	public void setConfirmed(long confirmed)
	{
		this.confirmed = confirmed;
	}

	public long getDeceased()
	{
		return deceased;
	}

	public void setDeceased(long deceased)
	{
		this.deceased = deceased;
	}

	public long getRecovered()
	{
		return recovered;
	}

	public void setRecovered(long recovered)
	{
		this.recovered = recovered;
	}

	public long getTested()
	{
		return tested;
	}

	public void setTested(long tested)
	{
		this.tested = tested;
	}

	public long getQuarantined()
	{
		return quarantined;
	}

	public void setQuarantined(long quarantined)
	{
		this.quarantined = quarantined;
	}

	public long getStandardCare()
	{
		return standardCare;
	}

	public void setStandardCare(long standardCare)
	{
		this.standardCare = standardCare;
	}

	public long getIntensiveCare()
	{
		return intensiveCare;
	}

	public void setIntensiveCare(long intensiveCare)
	{
		this.intensiveCare = intensiveCare;
	}

	public long getActive()
	{
		return active;
	}

	public void setActive(long active)
	{
		this.active = active;
	}

	public long getClosed()
	{
		return closed;
	}

	public void setClosed(long closed)
	{
		this.closed = closed;
	}

	public long getHospitalized()
	{
		return hospitalized;
	}

	public void setHospitalized(long hospitalized)
	{
		this.hospitalized = hospitalized;
	}

	public long getWorkload()
	{
		return workload;
	}

	public void setWorkload(long workload)
	{
		this.workload = workload;
	}

	public double getLethality()
	{
		return lethality;
	}

	public void setLethality(double lethality)
	{
		this.lethality = lethality;
	}

	public double getLethalityDelta()
	{
		return lethalityDelta;
	}

	public void setLethalityDelta(double lethalityDelta)
	{
		this.lethalityDelta = lethalityDelta;
	}

	public double getTestDensity()
	{
		return testDensity;
	}

	public void setTestDensity(double testDensity)
	{
		this.testDensity = testDensity;
	}

	public double getTestDensityDelta()
	{
		return testDensityDelta;
	}

	public void setTestDensityDelta(double testDensityDelta)
	{
		this.testDensityDelta = testDensityDelta;
	}

	public long getHypotheticalInfected()
	{
		return hypotheticalInfected;
	}

	public void setHypotheticalInfected(long hypotheticalInfected)
	{
		this.hypotheticalInfected = hypotheticalInfected;
	}

	public long getHypotheticalInfectedDelta()
	{
		return hypotheticalInfectedDelta;
	}

	public void setHypotheticalInfectedDelta(long hypotheticalInfectedDelta)
	{
		this.hypotheticalInfectedDelta = hypotheticalInfectedDelta;
	}

	public long getConfirmedDelta()
	{
		return confirmedDelta;
	}

	public void setConfirmedDelta(long confirmedDelta)
	{
		this.confirmedDelta = confirmedDelta;
	}

	public long getDeceasedDelta()
	{
		return deceasedDelta;
	}

	public void setDeceasedDelta(long deceasedDelta)
	{
		this.deceasedDelta = deceasedDelta;
	}

	public long getRecoveredDelta()
	{
		return recoveredDelta;
	}

	public void setRecoveredDelta(long recoveredDelta)
	{
		this.recoveredDelta = recoveredDelta;
	}

	public long getTestedDelta()
	{
		return testedDelta;
	}

	public void setTestedDelta(long testedDelta)
	{
		this.testedDelta = testedDelta;
	}

	public long getQuarantinedDelta()
	{
		return quarantinedDelta;
	}

	public void setQuarantinedDelta(long quarantinedDelta)
	{
		this.quarantinedDelta = quarantinedDelta;
	}

	public long getStandardCareDelta()
	{
		return standardCareDelta;
	}

	public void setStandardCareDelta(long standardCareDelta)
	{
		this.standardCareDelta = standardCareDelta;
	}

	public long getIntensiveCareDelta()
	{
		return intensiveCareDelta;
	}

	public void setIntensiveCareDelta(long intensiveCareDelta)
	{
		this.intensiveCareDelta = intensiveCareDelta;
	}

	public long getActiveDelta()
	{
		return activeDelta;
	}

	public void setActiveDelta(long activeDelta)
	{
		this.activeDelta = activeDelta;
	}

	public long getClosedDelta()
	{
		return closedDelta;
	}

	public void setClosedDelta(long closedDelta)
	{
		this.closedDelta = closedDelta;
	}

	public long getHospitalizedDelta()
	{
		return hospitalizedDelta;
	}

	public void setHospitalizedDelta(long hospitalizedDelta)
	{
		this.hospitalizedDelta = hospitalizedDelta;
	}

	public long getWorkloadDelta()
	{
		return workloadDelta;
	}

	public void setWorkloadDelta(long workloadDelta)
	{
		this.workloadDelta = workloadDelta;
	}

	public double getConfirmedDeltaPercent()
	{
		return confirmedDeltaPercent;
	}

	public void setConfirmedDeltaPercent(double confirmedDeltaPercent)
	{
		this.confirmedDeltaPercent = confirmedDeltaPercent;
	}

	public double getDeceasedDeltaPercent()
	{
		return deceasedDeltaPercent;
	}

	public void setDeceasedDeltaPercent(double deceasedDeltaPercent)
	{
		this.deceasedDeltaPercent = deceasedDeltaPercent;
	}

	public double getRecoveredDeltaPercent()
	{
		return recoveredDeltaPercent;
	}

	public void setRecoveredDeltaPercent(double recoveredDeltaPercent)
	{
		this.recoveredDeltaPercent = recoveredDeltaPercent;
	}

	public double getTestedDeltaPercent()
	{
		return testedDeltaPercent;
	}

	public void setTestedDeltaPercent(double testedDeltaPercent)
	{
		this.testedDeltaPercent = testedDeltaPercent;
	}

	public double getQuarantinedDeltaPercent()
	{
		return quarantinedDeltaPercent;
	}

	public void setQuarantinedDeltaPercent(double quarantinedDeltaPercent)
	{
		this.quarantinedDeltaPercent = quarantinedDeltaPercent;
	}

	public double getStandardCareDeltaPercent()
	{
		return standardCareDeltaPercent;
	}

	public void setStandardCareDeltaPercent(double standardCareDeltaPercent)
	{
		this.standardCareDeltaPercent = standardCareDeltaPercent;
	}

	public double getIntensiveCareDeltaPercent()
	{
		return intensiveCareDeltaPercent;
	}

	public void setIntensiveCareDeltaPercent(double intensiveCareDeltaPercent)
	{
		this.intensiveCareDeltaPercent = intensiveCareDeltaPercent;
	}

	public double getActiveDeltaPercent()
	{
		return activeDeltaPercent;
	}

	public void setActiveDeltaPercent(double activeDeltaPercent)
	{
		this.activeDeltaPercent = activeDeltaPercent;
	}

	public double getClosedDeltaPercent()
	{
		return closedDeltaPercent;
	}

	public void setClosedDeltaPercent(double closedDeltaPercent)
	{
		this.closedDeltaPercent = closedDeltaPercent;
	}

	public double getHospitalizedDeltaPercent()
	{
		return hospitalizedDeltaPercent;
	}

	public void setHospitalizedDeltaPercent(double hospitalizedDeltaPercent)
	{
		this.hospitalizedDeltaPercent = hospitalizedDeltaPercent;
	}

	public double getWorkloadDeltaPercent()
	{
		return workloadDeltaPercent;
	}

	public void setWorkloadDeltaPercent(double workloadDeltaPercent)
	{
		this.workloadDeltaPercent = workloadDeltaPercent;
	}

	public double getConfirmedPopulationPercent()
	{
		return confirmedPopulationPercent;
	}

	public void setConfirmedPopulationPercent(double confirmedPopulationPercent)
	{
		this.confirmedPopulationPercent = confirmedPopulationPercent;
	}

	public double getDeceasedPopulationPercent()
	{
		return deceasedPopulationPercent;
	}

	public void setDeceasedPopulationPercent(double deceasedPopulationPercent)
	{
		this.deceasedPopulationPercent = deceasedPopulationPercent;
	}

	public double getRecoveredPopulationPercent()
	{
		return recoveredPopulationPercent;
	}

	public void setRecoveredPopulationPercent(double recoveredPopulationPercent)
	{
		this.recoveredPopulationPercent = recoveredPopulationPercent;
	}

	public double getTestedPopulationPercent()
	{
		return testedPopulationPercent;
	}

	public void setTestedPopulationPercent(double testedPopulationPercent)
	{
		this.testedPopulationPercent = testedPopulationPercent;
	}

	public double getQuarantinedPopulationPercent()
	{
		return quarantinedPopulationPercent;
	}

	public void setQuarantinedPopulationPercent(double quarantinedPopulationPercent)
	{
		this.quarantinedPopulationPercent = quarantinedPopulationPercent;
	}

	public double getStandardCarePopulationPercent()
	{
		return standardCarePopulationPercent;
	}

	public void setStandardCarePopulationPercent(double standardCarePopulationPercent)
	{
		this.standardCarePopulationPercent = standardCarePopulationPercent;
	}

	public double getIntensiveCarePopulationPercent()
	{
		return intensiveCarePopulationPercent;
	}

	public void setIntensiveCarePopulationPercent(double intensiveCarePopulationPercent)
	{
		this.intensiveCarePopulationPercent = intensiveCarePopulationPercent;
	}

	public double getActivePopulationPercent()
	{
		return activePopulationPercent;
	}

	public void setActivePopulationPercent(double activePopulationPercent)
	{
		this.activePopulationPercent = activePopulationPercent;
	}

	public double getClosedPopulationPercent()
	{
		return closedPopulationPercent;
	}

	public void setClosedPopulationPercent(double closedPopulationPercent)
	{
		this.closedPopulationPercent = closedPopulationPercent;
	}

	public double getHospitalizedPopulationPercent()
	{
		return hospitalizedPopulationPercent;
	}

	public void setHospitalizedPopulationPercent(double hospitalizedPopulationPercent)
	{
		this.hospitalizedPopulationPercent = hospitalizedPopulationPercent;
	}

	public double getWorkloadPopulationPercent()
	{
		return workloadPopulationPercent;
	}

	public void setWorkloadPopulationPercent(double workloadPopulationPercent)
	{
		this.workloadPopulationPercent = workloadPopulationPercent;
	}

	public double getConfirmedDeltaPopulationPercent()
	{
		return confirmedDeltaPopulationPercent;
	}

	public void setConfirmedDeltaPopulationPercent(double confirmedDeltaPopulationPercent)
	{
		this.confirmedDeltaPopulationPercent = confirmedDeltaPopulationPercent;
	}

	public double getDeceasedDeltaPopulationPercent()
	{
		return deceasedDeltaPopulationPercent;
	}

	public void setDeceasedDeltaPopulationPercent(double deceasedDeltaPopulationPercent)
	{
		this.deceasedDeltaPopulationPercent = deceasedDeltaPopulationPercent;
	}

	public double getRecoveredDeltaPopulationPercent()
	{
		return recoveredDeltaPopulationPercent;
	}

	public void setRecoveredDeltaPopulationPercent(double recoveredDeltaPopulationPercent)
	{
		this.recoveredDeltaPopulationPercent = recoveredDeltaPopulationPercent;
	}

	public double getTestedDeltaPopulationPercent()
	{
		return testedDeltaPopulationPercent;
	}

	public void setTestedDeltaPopulationPercent(double testedDeltaPopulationPercent)
	{
		this.testedDeltaPopulationPercent = testedDeltaPopulationPercent;
	}

	public double getQuarantinedDeltaPopulationPercent()
	{
		return quarantinedDeltaPopulationPercent;
	}

	public void setQuarantinedDeltaPopulationPercent(double quarantinedDeltaPopulationPercent)
	{
		this.quarantinedDeltaPopulationPercent = quarantinedDeltaPopulationPercent;
	}

	public double getStandardCareDeltaPopulationPercent()
	{
		return standardCareDeltaPopulationPercent;
	}

	public void setStandardCareDeltaPopulationPercent(double standardCareDeltaPopulationPercent)
	{
		this.standardCareDeltaPopulationPercent = standardCareDeltaPopulationPercent;
	}

	public double getIntensiveCareDeltaPopulationPercent()
	{
		return intensiveCareDeltaPopulationPercent;
	}

	public void setIntensiveCareDeltaPopulationPercent(double intensiveCareDeltaPopulationPercent)
	{
		this.intensiveCareDeltaPopulationPercent = intensiveCareDeltaPopulationPercent;
	}

	public double getActiveDeltaPopulationPercent()
	{
		return activeDeltaPopulationPercent;
	}

	public void setActiveDeltaPopulationPercent(double activeDeltaPopulationPercent)
	{
		this.activeDeltaPopulationPercent = activeDeltaPopulationPercent;
	}

	public double getClosedDeltaPopulationPercent()
	{
		return closedDeltaPopulationPercent;
	}

	public void setClosedDeltaPopulationPercent(double closedDeltaPopulationPercent)
	{
		this.closedDeltaPopulationPercent = closedDeltaPopulationPercent;
	}

	public double getHospitalizedDeltaPopulationPercent()
	{
		return hospitalizedDeltaPopulationPercent;
	}

	public void setHospitalizedDeltaPopulationPercent(double hospitalizedDeltaPopulationPercent)
	{
		this.hospitalizedDeltaPopulationPercent = hospitalizedDeltaPopulationPercent;
	}

	public double getWorkloadDeltaPopulationPercent()
	{
		return workloadDeltaPopulationPercent;
	}

	public void setWorkloadDeltaPopulationPercent(double workloadDeltaPopulationPercent)
	{
		this.workloadDeltaPopulationPercent = workloadDeltaPopulationPercent;
	}
}
