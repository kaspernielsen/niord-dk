package org.niord.testdata;

import org.niord.core.area.Area;
import org.niord.core.batch.BatchService;
import org.niord.core.category.Category;
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


    /** Creates a couple of domains */
    private void importDomains() {
        Domain d = new Domain();
        d.setClientId("niord-client-nw");
        d.setName("NW");
        em.persist(d);

        d = new Domain();
        d.setClientId("niord-client-nm");
        d.setName("NM");
        em.persist(d);

        log.info("Created test domains");
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
                    new Properties());

            log.info("**** Started " + batchJobName + " batch job");

        } catch (Exception e) {
            log.error("Failed starting " + batchJobName + " batch job", e);
        }
    }

}
