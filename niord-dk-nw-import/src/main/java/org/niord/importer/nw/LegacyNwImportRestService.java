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
import org.niord.core.area.Area;
import org.niord.core.batch.BatchService;
import org.niord.model.DataFilter;
import org.niord.model.IJsonSerializable;
import org.niord.model.message.MessageVo;
import org.slf4j.Logger;

import javax.annotation.security.RolesAllowed;
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
import java.util.stream.Collectors;

/**
 * Imports legacy NW from an "oldmsi" database.
 * <p>
 * Additionally, handles import of firing area from the "oldmsi" database,
 * and creation of firing area message templates (which area actually created as NM's).
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
    LegacyFiringAreaImportService faImportService;

    @Inject
    BatchService batchService;

    /**
     * Tests the database connection and returns success or failure
     * @return if the database connection works
     */
    @GET
    @Path("/test-connection")
    public boolean testConnection() {
        return db.testConnection(true);
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
        return nwImportService.getImportLegacyNwParams();
    }


    @POST
    @Path("/import-nw")
    @Consumes("application/json;charset=UTF-8")
    @Produces("text/plain")
    @NoCache
    public String startNwImport(ImportLegacyNwParams params) {
        try {
            // Persist the parameters
            nwImportService.updateImportLegacyNwParams(params);

            // Check validity of parameters
            if (StringUtils.isBlank(params.getSeriesId())) {
                throw new Exception("Message series must be specified");
            }
            if (StringUtils.isBlank(params.getLocalSeriesId())) {
                throw new Exception("Local Message series must be specified");
            }
            if (params.getStartImportDate() == null) {
                throw new Exception("Start import date must be specified");
            }

            List<Integer> ids = nwImportService.getImportLegacyNwIds(params, true);

            // No point in importing empty result set
            if (ids.isEmpty()) {
                return "No legacy NW found";
            }
            ImportLegacyNwData batchData = new ImportLegacyNwData(params, ids);

            Map<String, Object> batchProperties = new HashMap<>();
            batchProperties.put("seriesId", params.getSeriesId());
            batchProperties.put("localSeriesId", params.getLocalSeriesId());
            batchProperties.put("tagName", params.getTagName());

            batchService.startBatchJobWithJsonData("dk-nw-import", batchData, "legacy-nw-data.json", batchProperties);

            String msg = "Started dk-nw-import batch job for " + ids.size() + " legacy NWs";
            log.info(msg);
            return msg;

        } catch (Exception e) {
            String msg = "Error importing legacy NW: " + e;
            log.error(msg, e);
            return msg;
        }
    }


    /**
     * Imports legacy firing areas
     * @return the status
     */
    @POST
    @Path("/import-fa")
    @Consumes("application/json;charset=UTF-8")
    @Produces("text/plain")
    @NoCache
    public String startFaImport() {
        try {

            Map<Integer, Area> areas = faImportService.importFiringAreas(true);

            // No point in importing empty result set
            if (areas.isEmpty()) {
                return "No legacy Firing Area found";
            }

            StringBuilder result = new StringBuilder();
            for (Map.Entry<Integer, Area> area : areas.entrySet()) {
                try {
                    faImportService.mergeArea(area.getValue());
                } catch (Exception e) {
                    result.append("Error merging area with legacy id ")
                            .append(area.getKey())
                            .append(": ")
                            .append(e.getMessage())
                            .append("\n");
                }
            }

            result.append("Imported ")
                    .append(areas.size())
                    .append(" legacy firing areas");

            log.info(result.toString());
            return result.toString();

        } catch (Exception e) {
            String msg = "Error importing legacy firing areas: " + e;
            log.error(msg, e);
            return msg;
        }
    }


    /**
     * Generates firing exercise message templates for each firing area
     * @return the status
     */
    @POST
    @Path("/generate-fa-messages")
    @Consumes("application/json;charset=UTF-8")
    @Produces("text/plain")
    @NoCache
    public String generateFaTemplates(GenerateFaTemplateParams params) {

        try {

            DataFilter filter = DataFilter.get()
                    .fields("Message.details", "Message.geometry", "Area.parent", "Category.parent");

            List<MessageVo> messages = faImportService.generateFiringAreaMessageTemplates(params.getSeriesId())
                    .stream()
                    .map(m -> m.toVo(filter))
                    .collect(Collectors.toList());

            Map<String, Object> batchProperties = new HashMap<>();
            batchProperties.put("seriesId", params.getSeriesId());
            batchProperties.put("tagName", params.getTagName());

            batchService.startBatchJobWithJsonData("message-import", messages, "message-data.json", batchProperties);

            String msg = "Started message-import batch job for " + messages.size() + " template firing area messages";
            log.info(msg);
            return msg;

        } catch (Exception e) {
            String msg = "Error generating template firing area messages: " + e;
            log.error(msg, e);
            return msg;
        }

    }


    /**
     * Defines the parameters used when starting an import of legacy NW messages
     */
    public static class ImportLegacyNwParams implements IJsonSerializable {

        String seriesId;
        String localSeriesId;
        String tagName;
        Date startImportDate;

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

        public String getTagName() {
            return tagName;
        }

        public void setTagName(String tagName) {
            this.tagName = tagName;
        }

        public Date getStartImportDate() {
            return startImportDate;
        }

        public void setStartImportDate(Date startImportDate) {
            this.startImportDate = startImportDate;
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
            this.setTagName(params.getTagName());
            this.ids = ids;
        }

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }
    }


    /**
     * Defines the parameters used when starting an import of legacy NW messages
     */
    public static class GenerateFaTemplateParams implements IJsonSerializable {

        String seriesId;
        String tagName;

        public String getSeriesId() {
            return seriesId;
        }

        public void setSeriesId(String seriesId) {
            this.seriesId = seriesId;
        }

        public String getTagName() {
            return tagName;
        }

        public void setTagName(String tagName) {
            this.tagName = tagName;
        }
    }

}
