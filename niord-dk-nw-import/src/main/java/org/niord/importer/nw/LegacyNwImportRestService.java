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
package org.niord.importer.nw;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.batch.BatchService;
import org.niord.model.IJsonSerializable;
import org.slf4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Imports legacy NW from an "oldmsi" database.
 */
@Path("/import/nw")
@Stateless
@SecurityDomain("keycloak")
@RolesAllowed("admin")
@SuppressWarnings("unused")
public class LegacyNwImportRestService {

    @Inject
    Logger log;

    @Inject
    LegacyNwDatabase db;

    @Inject
    LegacyNwImportService nwImportService;

    @Inject
    BatchService batchService;


    /**
     * Tests the database connection and returns success or failure
     * @return if the database connection works
     */
    @GET
    @Path("/test-connection")
    public boolean testConnection() {
        return db.testConnection(false);
    }


    /**
     * Tests the database connection and returns success or failure
     * @return if the database connection works
     */
    @GET
    @Path("/params")
    @Consumes("application/json;charset=UTF-8")
    @NoCache
    public ImportLegacyNwParams getImportLegacyNwParams() {
        return nwImportService.getOrCreateImportLegacyNwParams();
    }


    /**
     * Will import legacy NW messages based on the submitted parameters.
     * @param params the batch import parameters
     * @return a textual status
     */
    @POST
    @Path("/import-nw")
    @Consumes("application/json;charset=UTF-8")
    @Produces("text/plain")
    @NoCache
    public String startNwImport(ImportLegacyNwParams params) {
        try {
            params = params.validate();

            // Persist the parameters
            nwImportService.updateImportLegacyNwParams(params);

            // If a start data is defined, start the batch job
            if (params.getStartImportDate() != null) {
                List<Integer> ids = nwImportService.getImportLegacyNwIds(params, true);

                // No point in importing empty result set
                if (ids.isEmpty()) {
                    return "No legacy NW found";
                }

                // Start the import batch job
                ImportLegacyNwData batchData = new ImportLegacyNwData(params, ids);
                return startNwImportBatchJob(batchData);
            }

            return "Updated NW import parameters";

        } catch (Exception e) {
            String msg = "Error importing legacy NW: " + e;
            log.error(msg, e);
            return msg;
        }
    }


    /**
     * Called periodically to auto-import legacy NW messages (if the auto-import flag is turned on)
     */
    @Schedule(persistent = false, second = "40", minute = "*/10", hour = "*")
    private void periodicAutoImportNwMessages() {
        try {
            long now = System.currentTimeMillis();

            // Check if auto-import is turned on
            ImportLegacyNwParams params = nwImportService.getImportLegacyNwParams();
            if (params == null) {
                return;
            }
            params.validate();

            if (params.getAutoImport() == null || !params.getAutoImport()) {
                // Auto-import NOT turned on
                return;
            }

            // If this periodic job is called right after (within 5 minutes of) a manual execution, bail out.
            if (params.getLastUpdated() != null && now - params.getLastUpdated().getTime() < 1000L * 60L * 5L) {
                return;
            }

            // Get the ID's of the active legacy NW to import
            List<Integer> ids = nwImportService.getActiveLegacyNwIds(params, true);

            // No point in importing empty result set
            if (ids.isEmpty()) {
                log.debug("Periodic legacy NW auto-import: 0 messages imported");
                return;
            }

            // Start the import batch job
            ImportLegacyNwData batchData = new ImportLegacyNwData(params, ids);
            String result = startNwImportBatchJob(batchData);

        } catch (Exception e) {
            log.error("Error performing periodic legacy NW auto-import", e);
        }
    }


    /**
     * Starts the NW import batch job
     * @param batchData the batch job parameters
     * @return a textual status
     */
    private String startNwImportBatchJob(ImportLegacyNwData batchData) throws Exception {

        Map<String, Object> batchProperties = new HashMap<>();
        batchProperties.put("seriesId", batchData.getSeriesId());
        batchProperties.put("localSeriesId", batchData.getLocalSeriesId());
        batchProperties.put("tagId", batchData.getTagId());

        batchService.startBatchJobWithJsonData("dk-nw-import", batchData, "legacy-nw-data.json", batchProperties);

        String msg = "Started dk-nw-import batch job for " + batchData.getIds().size() + " legacy NWs";
        log.info(msg);
        return msg;
    }


    /**
     * Defines the parameters used when starting an import of legacy NW messages
     */
    public static class ImportLegacyNwParams implements IJsonSerializable {

        String seriesId;
        String localSeriesId;
        String tagId;
        Date startImportDate;
        Boolean autoImport;
        Date lastUpdated;


        /** Validates that the paramters are valid, i.e. defines required parameters **/
        public ImportLegacyNwParams validate() throws Exception {
            // Check validity of parameters
            if (StringUtils.isBlank(seriesId)) {
                throw new Exception("Message series must be specified");
            }
            if (StringUtils.isBlank(localSeriesId)) {
                throw new Exception("Local Message series must be specified");
            }
            return this;
        }

        public String getSeriesId() {
            return seriesId;
        }

        public void setSeriesId(String seriesId) {
            this.seriesId = seriesId;
        }

        public String getLocalSeriesId() {
            return localSeriesId;
        }

        public void setLocalSeriesId(String localSeriesId) {
            this.localSeriesId = localSeriesId;
        }

        public String getTagId() {
            return tagId;
        }

        public void setTagId(String tagId) {
            this.tagId = tagId;
        }

        public Date getStartImportDate() {
            return startImportDate;
        }

        public void setStartImportDate(Date startImportDate) {
            this.startImportDate = startImportDate;
        }

        public Boolean getAutoImport() {
            return autoImport;
        }

        public void setAutoImport(Boolean autoImport) {
            this.autoImport = autoImport;
        }

        public Date getLastUpdated() {
            return lastUpdated;
        }

        public void setLastUpdated(Date lastUpdated) {
            this.lastUpdated = lastUpdated;
        }
    }

    /**
     * Defines the parameters used when starting an import of legacy NW messages
     */
    public static class ImportLegacyNwData extends ImportLegacyNwParams {

        List<Integer> ids;

        public ImportLegacyNwData() {
        }

        public ImportLegacyNwData(ImportLegacyNwParams params, List<Integer> ids) {
            this.setSeriesId(params.getSeriesId());
            this.setLocalSeriesId(params.getLocalSeriesId());
            this.setTagId(params.getTagId());
            this.ids = ids;
        }

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }
    }
}
