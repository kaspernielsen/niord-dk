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
import java.util.List;
import java.util.Properties;

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


    @POST
    @Path("/import-active-nw")
    @Consumes("application/json;charset=UTF-8")
    @Produces("text/plain")
    @NoCache
    public String startActiveNwImport(ImportActiveLegacyNwParams params) {
        try {
            List<Integer> ids = importService.getActiveLegacyNwIds(true);

            // No point in importing empty result set
            if (ids.isEmpty()) {
                return "No active legacy NW found";
            }
            params.setIds(ids);

            Properties batchProperties = new Properties();
            batchProperties.setProperty("seriesId", params.getSeriesId());
            batchProperties.setProperty("tagName", params.getTagName());

            batchService.startBatchJobWithJsonData("dk-nw-import", params, "legacy-nw-data.json", batchProperties);

            String msg = "Started dk-nw-import batch job for " + ids.size() + " legacy NWs";
            log.info(msg);
            return msg;

        } catch (Exception e) {
            String msg = "Error importing active legacy NW: " + e;
            log.error(msg, e);
            return msg;
        }
    }


    /**
     * Defines the parameters used when starting an import of legacy NW messages
     */
    public static class ImportActiveLegacyNwParams implements IJsonSerializable {

        String seriesId;
        String tagName;
        List<Integer> ids;

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

        public List<Integer> getIds() {
            return ids;
        }

        public void setIds(List<Integer> ids) {
            this.ids = ids;
        }
    }
}
