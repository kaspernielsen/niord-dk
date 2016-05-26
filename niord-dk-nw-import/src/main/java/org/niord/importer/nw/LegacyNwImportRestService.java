/* Copyright (c) 2011 Danish Maritime Authority
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this library.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.niord.importer.nw;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.batch.BatchService;
import org.niord.model.IJsonSerializable;
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
    LegacyNwImportService importService;

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
        return importService.getImportLegacyNwParams();
    }


    @POST
    @Path("/import-nw")
    @Consumes("application/json;charset=UTF-8")
    @Produces("text/plain")
    @NoCache
    public String startNwImport(ImportLegacyNwParams params) {
        try {
            // Persist the parameters
            importService.updateImportLegacyNwParams(params);

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

            List<Integer> ids = importService.getImportLegacyNwIds(params, true);

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
}
