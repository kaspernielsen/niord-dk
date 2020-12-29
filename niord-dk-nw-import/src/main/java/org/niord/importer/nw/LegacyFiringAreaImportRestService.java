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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.ejb3.annotation.SecurityDomain;
import org.niord.core.area.Area;
import org.niord.core.schedule.FiringPeriod;
import org.niord.core.batch.BatchService;
import org.niord.core.message.vo.SystemMessageVo;
import org.niord.core.user.Roles;
import org.niord.model.DataFilter;
import org.niord.model.IJsonSerializable;
import org.slf4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Schedule;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handles import of firing area from the "oldmsi" database,
 * and creation of firing area message templates (which area actually created as NM's).
 */
@Path("/import/fa")
@Stateless
@SecurityDomain("keycloak")
@RolesAllowed(Roles.ADMIN)
@SuppressWarnings("unused")
public class LegacyFiringAreaImportRestService {

    @Inject
    Logger log;

    @Inject
    LegacyNwDatabase db;

    @Inject
    LegacyFiringAreaImportService faImportService;

    @Inject
    BatchService batchService;


    /***************************************/
    /** Firing Area Import                **/
    /***************************************/


    /**
     * Imports legacy firing areas
     *
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


    /***************************************/
    /** Firing Exercise Schedule          **/
    /***************************************/

    /** Returns the auto-import flag */
    @GET
    @Path("/auto-import-fa-schedule")
    @NoCache
    public Boolean getAutoImportFeSchedule() {
        return faImportService.getAutoImportFeSchedule();
    }


    /** Updates the auto-import flag */
    @POST
    @Path("/auto-import-fa-schedule")
    @Consumes("application/json;charset=UTF-8")
    @Produces("text/plain")
    @NoCache
    public String updateAutoImportFeSchedule(Boolean autoImport) {
        log.info("Setting auto-import of firing exercise schedule: " + autoImport);
        faImportService.updateAutoImportFeSchedule(autoImport);
        return "OK";
    }


    /**
     * Imports legacy firing area schedule
     * @return the status
     */
    @POST
    @Path("/import-fa-schedule")
    @Consumes("application/json;charset=UTF-8")
    @Produces("text/plain")
    @NoCache
    public String startFaScheduleImport(
            @QueryParam("importDb") @DefaultValue("false") boolean importDb
    ) {

        long t0 = System.currentTimeMillis();
        StringBuilder result = new StringBuilder();
        try {
            List<FiringPeriod> fps = faImportService.importFiringAreaSchedule(importDb, result);

            log.debug("Firing area schedule import completed in " + (System.currentTimeMillis() - t0) + " ms");
            result.append("Firing area schedule import completed in ").append(System.currentTimeMillis() - t0).append(" ms\n");

        } catch (Exception e) {
            log.error("Error updating firing area schedule", e);
            result.append("Error updating firing area schedule: ")
                    .append(e.getMessage())
                    .append("\n");
        }
        return result.toString();
    }


    /**
     * Called periodically to auto-import the legacy firing exercise schedule (if the auto-import flag is turned on)
     */
    @Schedule(persistent = false, second = "10", minute = "*/7", hour = "*")
    private void periodicAutoImportFaSchedule() {
        try {

            Boolean autoImport = faImportService.getAutoImportFeSchedule();
            if (autoImport != null && autoImport) {
                startFaScheduleImport(true);
            }


        } catch (Exception e) {
            log.error("Error performing periodic firing schedule auto-import", e);
        }
    }


    /***************************************/
    /** Firing Area Message Templates     **/
    /***************************************/


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

            List<SystemMessageVo> messages = faImportService.generateFiringAreaMessageTemplates(params.getSeriesId())
                    .stream()
                    .map(m -> m.toVo(SystemMessageVo.class, filter))
                    .collect(Collectors.toList());

            Map<String, Object> batchProperties = new HashMap<>();
            batchProperties.put("seriesId", params.getSeriesId());
            batchProperties.put("tagId", params.getTagId());

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


    /***************************************/
    /** Helper classes                    **/
    /***************************************/


    /**
     * Defines the parameters used when starting an import of legacy NW messages
     */
    public static class GenerateFaTemplateParams implements IJsonSerializable {

        String seriesId;
        String tagId;

        public String getSeriesId() {
            return seriesId;
        }

        public void setSeriesId(String seriesId) {
            this.seriesId = seriesId;
        }

        public String getTagId() {
            return tagId;
        }

        public void setTagId(String tagId) {
            this.tagId = tagId;
        }
    }

}
