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
    private boolean aggregate = false;

    @Transient
    private Record previous;

    /* values */

    private long confirmed;
    private long deceased;
    private long recovered;
    private long tested;
    private long quarantined;
    private long standardCare;
    private long intensiveCare;


    /* derived */

    private long active;
    private long closed;
    private long hospitalized;


    /* computed */

    private double lethality;
    private double lethalityLatest;
    private double testDensity;
    private double testDensityLatest;
    private double growth;
    private long activeHypotheticalZero;
    private long confirmedHypotheticalFull;


    /* delta */

    private long confirmedDelta;
    private long deceasedDelta;
    private long recoveredDelta;
    private long testedDelta;
    private long quarantinedDelta;
    private long standardCareDelta;
    private long intensiveCareDelta;

    private long activeDelta;
    private long closedDelta;
    private long hospitalizedDelta;

    private double lethalityDelta;
    private double growthDelta;
    private double testDensityDelta;


    /* delta percent */

    private double confirmedDeltaPercent;
    private double deceasedDeltaPercent;
    private double recoveredDeltaPercent;
    private double testedDeltaPercent;
    private double quarantinedDeltaPercent;
    private double standardCareDeltaPercent;
    private double intensiveCareDeltaPercent;

    private double activeDeltaPercent;
    private double closedDeltaPercent;
    private double hospitalizedDeltaPercent;


    /* values Population percent */

    private double confirmedPopulationPercent;
    private double deceasedPopulationPercent;
    private double recoveredPopulationPercent;
    private double testedPopulationPercent;
    private double quarantinedPopulationPercent;
    private double standardCarePopulationPercent;
    private double intensiveCarePopulationPercent;

    private double activePopulationPercent;
    private double closedPopulationPercent;
    private double hospitalizedPopulationPercent;


    /* hypothetical */

    private long confirmedHypothetical;
    private long deceasedHypothetical;
    private long recoveredHypothetical;
    private long activeHypothetical;
    private long closedHypothetical;


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

    public static Record buildRecord(EntityManager em, Region region, Date registered)
    {
        return buildRecord(em, new RecordId(region, registered));
    }

    public void aggregate(Collection<? extends Record> records)
    {
        aggregate = true;

        StreamEx.of(RecordProperty.getMain())
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

    public boolean equals(Record that, RecordProperty property)
    {
        if(that == null)
        {
            return false;
        }

        if(this == that)
        {
            return true;
        }

        return Objects.equals(property.get(this), property.get(that));
    }

    public boolean equals(Record that, RecordProperty... properties)
    {
        if(that == null)
        {
            return false;
        }

        if(this == that)
        {
            return true;
        }

        for(RecordProperty property : properties)
        {
            if(!Objects.equals(property.get(this), property.get(that)))
            {
                return false;
            }
        }

        return true;
    }

    public boolean equals(Record that, Iterable<RecordProperty> properties)
    {
        if(that == null)
        {
            return false;
        }

        if(this == that)
        {
            return true;
        }

        for(RecordProperty property : properties)
        {
            if(!Objects.equals(property.get(this), property.get(that)))
            {
                return false;
            }
        }

        return true;
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
        double lethality2 = previous.getLethality();
        double growth2 = previous.getGrowth();
        double testDensity2 = previous.getTestDensity();

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

        if(closed != 0)
        {
            lethality = (double) deceased / closed;
        }

        if(closedDelta != 0)
        {
            growth = (double) confirmedDelta / closedDelta;
            lethalityLatest = (double) deceasedDelta / closedDelta;
        }

        if(tested != 0)
        {
            testDensity = (double) confirmed / tested;
        }

        if(testedDelta != 0)
        {
            testDensityLatest = (double) confirmedDelta / testedDelta;
        }

        if(active > 0 && activeDelta < 0)
        {
            activeHypotheticalZero = (long) -Math.ceil((double) active / activeDelta);
        }

        if(tested > 0 && confirmedDelta > 0)
        {
            confirmedHypotheticalFull = (long) Math.ceil((double) (tested - confirmed) / confirmedDelta);
        }


        lethalityDelta = lethality - lethality2;
        growthDelta = growth - growth2;
        testDensityDelta = testDensity - testDensity2;

        confirmedDeltaPercent = confirmed2 != 0 ? (double) confirmedDelta / confirmed2 : 0;
        deceasedDeltaPercent = deceased2 != 0 ? (double) deceasedDelta / deceased2 : 0;
        recoveredDeltaPercent = recovered2 != 0 ? (double) recoveredDelta / recovered2 : 0;
        testedDeltaPercent = tested2 != 0 ? (double) testedDelta / tested2 : 0;
        quarantinedDeltaPercent = quarantined2 != 0 ? (double) quarantinedDelta / quarantined2 : 0;
        standardCareDeltaPercent = standardCare2 != 0 ? (double) standardCareDelta / standardCare2 : 0;
        intensiveCareDeltaPercent = intensiveCare2 != 0 ? (double) intensiveCareDelta / intensiveCare2 : 0;
        activeDeltaPercent = active2 != 0 ? (double) activeDelta / active2 : 0;
        closedDeltaPercent = closed2 != 0 ? (double) closedDelta / closed2 : 0;
        hospitalizedDeltaPercent = hospitalized2 != 0 ? (double) hospitalizedDelta / hospitalized2 : 0;

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

            if(tested > 0)
            {
                confirmedHypothetical = (population * confirmed) / tested;
                deceasedHypothetical = (population * deceased) / tested;
                recoveredHypothetical = (population * recovered) / tested;
                activeHypothetical = (population * active) / tested;
                closedHypothetical = (population * closed) / tested;
            }
        }
    }

    public boolean hasChanges()
    {
        return StreamEx.of(RecordProperty.getMain())
            .mapToLong(x -> x.getDelta(this))
            .anyMatch(x -> x != 0);
    }

    public static void main(String[] args)
    {
        long population = 60_000_000L;
        long confirmed = 100_000;
        long tested = 500_000;
        long confirmedDelta = 10_000;

        double density = (double) confirmed / tested;
        long confirmedHyp = (long) (population * density);
        long negativeHyp = (long) (population * (1 - density));
        long negativeHyp2 = population - confirmedHyp;

        long confirmedHypDelta = (long) (population * (double) confirmedDelta / tested);
        long confirmedHypDelta2 = (long) (confirmedHyp * (double) confirmedDelta / confirmed);

        System.out.println("confirmedHyp: " + confirmedHyp);
        System.out.println("negativeHyp: " + negativeHyp);
        System.out.println("negativeHyp2: " + negativeHyp2);
        System.out.println("confirmedHypDelta: " + confirmedHypDelta);
        System.out.println("confirmedHypDelta2: " + confirmedHypDelta2);
        System.out.println("confirmedHypFull: " + negativeHyp / confirmedHypDelta);
        System.out.println("confirmedHypFull2: " + ((double) (tested - confirmed) / confirmedDelta));
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

    public double getLethality()
    {
        return lethality;
    }

    public void setLethality(double lethality)
    {
        this.lethality = lethality;
    }

    public double getLethalityLatest()
    {
        return lethalityLatest;
    }

    public void setLethalityLatest(double lethalityLatest)
    {
        this.lethalityLatest = lethalityLatest;
    }

    public double getLethalityDelta()
    {
        return lethalityDelta;
    }

    public void setLethalityDelta(double lethalityDelta)
    {
        this.lethalityDelta = lethalityDelta;
    }

    public double getGrowth()
    {
        return growth;
    }

    public void setGrowth(double growth)
    {
        this.growth = growth;
    }

    public double getGrowthDelta()
    {
        return growthDelta;
    }

    public void setGrowthDelta(double growthDelta)
    {
        this.growthDelta = growthDelta;
    }

    public double getTestDensity()
    {
        return testDensity;
    }

    public void setTestDensity(double testDensity)
    {
        this.testDensity = testDensity;
    }

    public double getTestDensityLatest()
    {
        return testDensityLatest;
    }

    public void setTestDensityLatest(double testDensityLatest)
    {
        this.testDensityLatest = testDensityLatest;
    }

    public double getTestDensityDelta()
    {
        return testDensityDelta;
    }

    public void setTestDensityDelta(double testDensityDelta)
    {
        this.testDensityDelta = testDensityDelta;
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

    public long getConfirmedHypothetical()
    {
        return confirmedHypothetical;
    }

    public void setConfirmedHypothetical(long confirmedHypothetical)
    {
        this.confirmedHypothetical = confirmedHypothetical;
    }

    public long getDeceasedHypothetical()
    {
        return deceasedHypothetical;
    }

    public void setDeceasedHypothetical(long deceasedHypothetical)
    {
        this.deceasedHypothetical = deceasedHypothetical;
    }

    public long getRecoveredHypothetical()
    {
        return recoveredHypothetical;
    }

    public void setRecoveredHypothetical(long recoveredHypothetical)
    {
        this.recoveredHypothetical = recoveredHypothetical;
    }

    public long getActiveHypothetical()
    {
        return activeHypothetical;
    }

    public void setActiveHypothetical(long activeHypothetical)
    {
        this.activeHypothetical = activeHypothetical;
    }

    public long getClosedHypothetical()
    {
        return closedHypothetical;
    }

    public void setClosedHypothetical(long closedHypothetical)
    {
        this.closedHypothetical = closedHypothetical;
    }

    public long getActiveHypotheticalZero()
    {
        return activeHypotheticalZero;
    }

    public void setActiveHypotheticalZero(long activeHypotheticalZero)
    {
        this.activeHypotheticalZero = activeHypotheticalZero;
    }

    public long getConfirmedHypotheticalFull()
    {
        return confirmedHypotheticalFull;
    }

    public void setConfirmedHypotheticalFull(long confirmedHypotheticalFull)
    {
        this.confirmedHypotheticalFull = confirmedHypotheticalFull;
    }
}
