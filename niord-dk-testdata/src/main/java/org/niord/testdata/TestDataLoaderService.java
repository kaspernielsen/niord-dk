/*
 * Copyright 2016 Danish Maritime Authority.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.niord.testdata;

import org.niord.core.area.Area;
import org.niord.core.batch.BatchService;
import org.niord.core.category.Category;
import org.niord.core.chart.Chart;
import org.niord.core.domain.Domain;
import org.niord.core.domain.DomainService;
import org.niord.core.fm.FmReport;
import org.niord.core.message.MessageSeries;
import org.niord.core.message.vo.SystemMessageSeriesVo.NumberSequenceType;
import org.niord.core.publication.Publication;
import org.niord.core.service.BaseService;
import org.niord.core.source.Source;
import org.niord.model.message.MainType;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

    @Inject
    DomainService domainService;

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

        // Check if we need to load publications
        if (count(Publication.class) == 0) {
            startBatchJob("publication-import", "publications.json");
        }

        // Check if we need to load sources
        if (count(Source.class) == 0) {
            startBatchJob("source-import", "sources.json");
        }

        // Check if we need to create reports
        checkCreateReports();
    }


    /** Creates a couple of message series and domains */
    private void importDomains() {

        Domain d = new Domain();
        d.setDomainId("niord-client-nw");
        d.setName("NW");
        d.getMessageSeries().add(createMessageSeries(
                "dma-nw",
                MainType.NW,
                NumberSequenceType.YEARLY,
                "NW-${number-3-digits}-${year-2-digits}"
                ));
        d.getMessageSeries().add(createMessageSeries(
                "dma-nw-local",
                MainType.NW,
                NumberSequenceType.NONE,
                null
        ));
        d.setTimeZone("Europe/Copenhagen");
        d.setPublish(true);
        em.persist(d);

        d = new Domain();
        d.setDomainId("niord-client-nm");
        d.setName("NM");
        d.getMessageSeries().add(createMessageSeries(
                "dma-nm",
                MainType.NM,
                NumberSequenceType.YEARLY,
                "NM-${number-3-digits}-${year-2-digits} ${t-or-p}",
                "nm-w${week-2-digits}-${year}"
        ));
        d.setTimeZone("Europe/Copenhagen");
        d.setPublish(true);
        em.persist(d);


        d = new Domain();
        d.setDomainId("niord-client-fa");
        d.setName("Firing Areas");
        d.getMessageSeries().add(createMessageSeries(
                "dma-fa",
                MainType.NM,
                NumberSequenceType.MANUAL,
                null
        ));
        d.setTimeZone("Europe/Copenhagen");
        d.setSchedule(true);
        em.persist(d);

        d = new Domain();
        d.setDomainId("niord-client-annex");
        d.setName("NM Annex");
        d.getMessageSeries().add(createMessageSeries(
                "dma-nm-annex",
                MainType.NM,
                NumberSequenceType.YEARLY,
                "A/${number} ${year}"
        ));
        d.setTimeZone("Europe/Copenhagen");
        d.setMessageSortOrder("ID ASC");
        em.persist(d);


        log.info("Created test domains");
    }


    /** Creates the given message series */
    private MessageSeries createMessageSeries(
            String seriesId, MainType type, NumberSequenceType numberSequenceType,
            String shortFormat, String... publishTagFormats) {
        MessageSeries s = new MessageSeries();
        s.setSeriesId(seriesId);
        s.setMainType(type);
        s.setNumberSequenceType(numberSequenceType);
        s.setShortFormat(shortFormat);
        if (publishTagFormats != null && publishTagFormats.length > 0) {
            s.getPublishTagFormats().addAll(Arrays.asList(publishTagFormats));
        }
        em.persist(s);
        return s;
    }


    /** Creates a standard NM report */
    private void checkCreateReports() {
        try {
            List<FmReport> reports = em.createNamedQuery("FmReport.findByReportId", FmReport.class)
                    .setParameter("reportId", "nm-report")
                    .getResultList();
            if (reports.isEmpty()) {
                Domain nmDomain = domainService.findByDomainId("niord-client-nm");
                if (nmDomain != null) {
                    FmReport report = new FmReport();
                    report.setReportId("nm-report");
                    report.setName("NM report");
                    report.setTemplatePath("/templates/messages/nm-report-pdf.ftl");
                    report.getDomains().add(nmDomain);
                    em.persist(report);

                    report = new FmReport();
                    report.setReportId("nm-tp-report");
                    report.setName("NM T&P report");
                    report.setTemplatePath("/templates/messages/nm-tp-report-pdf.ftl");
                    report.getDomains().add(nmDomain);
                    report.getProperties().put("mapThumbnails", Boolean.FALSE);
                    em.persist(report);
                }

                Domain faDomain = domainService.findByDomainId("niord-client-fa");
                if (faDomain != null) {
                    FmReport report = new FmReport();
                    report.setReportId("fa-list");
                    report.setName("Firing Areas");
                    report.setTemplatePath("/templates/messages/fa-list-pdf.ftl");
                    report.getDomains().add(faDomain);
                    em.persist(report);
                }

                Domain annexDomain = domainService.findByDomainId("niord-client-annex");
                if (annexDomain != null) {
                    FmReport report = new FmReport();
                    report.setReportId("nm-annex");
                    report.setName("NM Annex");
                    report.setTemplatePath("/templates/messages/nm-annex-report-pdf.ftl");
                    report.getDomains().add(annexDomain);
                    report.getProperties().put("mapThumbnails", Boolean.FALSE);
                    em.persist(report);
                }

                log.info("Created NM reports");}
        } catch (Exception e) {
            log.error("Error creating NM reports", e);
        }
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
