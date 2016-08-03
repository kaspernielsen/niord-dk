package org.niord.testdata;

import org.niord.core.area.Area;
import org.niord.core.batch.BatchService;
import org.niord.core.category.Category;
import org.niord.core.chart.Chart;
import org.niord.core.domain.Domain;
import org.niord.core.message.MessageSeries;
import org.niord.core.service.BaseService;
import org.niord.model.vo.MainType;
import org.niord.model.vo.MessageSeriesVo.NumberSequenceType;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import java.util.HashMap;

/**
 * Loads test base data into an empty system
 */
@Singleton
@Startup
@SuppressWarnings("unused")
public class TestDataLoaderService extends BaseService {

    @Inject
    Logger log;

    @Inject
    BatchService batchService;

    @Resource
    TimerService timerService;

    /**
     * Called when the system starts up. Loads base data
     */
    @PostConstruct
    public void init() {
        // In order not to stall webapp deployment, wait 5 seconds before checking for base data
        timerService.createSingleActionTimer(5000, new TimerConfig());
    }

    /**
     * Check if we need to load base data
     */
    @Timeout
    private void checkLoadBaseData() {

        // Check if we need to load charts
        if (count(Chart.class) == 0) {
            startBatchJob("chart-import", "charts.json");
        }

        // Check if we need to load areas
        if (count(Area.class) == 0) {
            startBatchJob("area-import", "areas.json");
        }

        // Check if we need to load categories
        if (count(Category.class) == 0) {
            startBatchJob("category-import", "categories.json");
        }

        // Check if we need to load domains
        if (count(Domain.class) == 0) {
            importDomains();
        }
    }


    /** Creates a couple of message series and domains */
    private void importDomains() {

        Domain d = new Domain();
        d.setClientId("niord-client-nw");
        d.setName("NW");
        d.getMessageSeries().add(createMessageSeries(
                "dma-nw",
                MainType.NW,
                NumberSequenceType.YEARLY,
                "urn:mrn:iho:nw:dk:dma:${year}:${number}",
                "NW-${number-3-digits}-${year-2-digits}"
                ));
        d.getMessageSeries().add(createMessageSeries(
                "dma-nw-local",
                MainType.NW,
                NumberSequenceType.YEARLY,
                "urn:mrn:iho:nw:dk:dma:local:${year}:${legacy-id}",
                "NW-LOCAL-${legacy-id}-${year-2-digits}"
        ));
        d.setTimeZone("Europe/Copenhagen");
        em.persist(d);

        d = new Domain();
        d.setClientId("niord-client-nm");
        d.setName("NM");
        d.getMessageSeries().add(createMessageSeries(
                "dma-nm",
                MainType.NM,
                NumberSequenceType.YEARLY,
                "urn:mrn:iho:nm:dk:dma:${year}:${number}",
                "NM-${number-3-digits}-${year-2-digits}"
        ));
        d.setTimeZone("Europe/Copenhagen");
        em.persist(d);

        log.info("Created test domains");
    }


    /** Creates the given message series */
    private MessageSeries createMessageSeries(String seriesId, MainType type, NumberSequenceType numberSequenceType, String mrnFormat, String shortFormat) {
        MessageSeries s = new MessageSeries();
        s.setSeriesId(seriesId);
        s.setMainType(type);
        s.setNumberSequenceType(numberSequenceType);
        s.setMrnFormat(mrnFormat);
        s.setShortFormat(shortFormat);
        em.persist(s);
        return s;
    }

    /**
     * Starts the batch job with the given name and load the associated batch file data
     */
    private void startBatchJob(String batchJobName, String batchFileName) {
        try {

            batchService.startBatchJobWithDataFile(
                    batchJobName,
                    getClass().getResourceAsStream("/" + batchFileName),
                    batchFileName,
                    new HashMap<>());

            log.info("**** Started " + batchJobName + " batch job");

        } catch (Exception e) {
            log.error("Failed starting " + batchJobName + " batch job", e);
        }
    }

}
