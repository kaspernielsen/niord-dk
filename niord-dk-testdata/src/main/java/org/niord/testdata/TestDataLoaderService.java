package org.niord.testdata;

import org.niord.core.batch.BatchService;
import org.niord.core.chart.Chart;
import org.niord.core.domain.Domain;
import org.niord.core.service.BaseService;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.Properties;

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

    /**
     * Called when the system starts up. Loads base data
     */
    @PostConstruct
    public void init() {

        // Check if we need to load charts
        if (count(Chart.class) == 0) {
            startChartImportBatchJob();
        }

        // Check if we need to load domains
        if (count(Domain.class) == 0) {
            importDomains();
        }
    }

    /** Creates a couple of domains */
    private void importDomains() {
        Domain d = new Domain();
        d.setClientId("niord-web");
        d.setName("All");
        em.persist(d);

        d = new Domain();
        d.setClientId("niord-web-nw");
        d.setName("NW");
        em.persist(d);

        d = new Domain();
        d.setClientId("niord-web-nm");
        d.setName("NM");
        em.persist(d);

        log.info("Created test domains");
    }

    /**
     * Starts a batch job to load the charts from the "/charts.json" file
     */
    private void startChartImportBatchJob() {
        try {

            batchService.startBatchJobWithDataFile(
                    "chart-import",
                    getClass().getResourceAsStream("/charts.json"),
                    "charts.json",
                    new Properties());

            log.info("**** Started chart-import batch job");

        } catch (Exception e) {
            log.error("Failed starting chart-import batch job", e);
        }
    }
}
