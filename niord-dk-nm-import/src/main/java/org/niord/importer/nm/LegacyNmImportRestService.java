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
package org.niord.importer.nm;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.batch.AbstractBatchableRestService;
import org.niord.model.IJsonSerializable;
import org.slf4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Properties;

/**
 * Imports NM from an HTML page generated from the original Word document.
 */
@Path("/import/nm")
@Stateless
@SecurityDomain("keycloak")
@RolesAllowed("admin")
@SuppressWarnings("unused")
public class LegacyNmImportRestService extends AbstractBatchableRestService {

    @Inject
    Logger log;

    /**
     * Imports an uploaded NM HTML file
     *
     * @param request the servlet request
     * @return a status
     */
    @POST
    @Path("/import-nm")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    public String importNms(@Context HttpServletRequest request) throws Exception {

        return executeBatchJobFromUploadedFile(request, "dk-nm-import");
    }


    /** {@inheritDoc} */
    @Override
    protected void checkBatchJob(String batchJobName, FileItem fileItem, Properties params) throws Exception {

        ImportLegacyNmParams batchData;
        try {
            batchData = new ObjectMapper().readValue(params.getProperty("data"), ImportLegacyNmParams.class);
        } catch (IOException e) {
            throw new Exception("Missing batch data with tag and message series");
        }

        if (StringUtils.isBlank(batchData.getTagName())) {
            throw new Exception("Missing message tag for imported NMs");
        }
        if (StringUtils.isBlank(batchData.getSeriesId())) {
            throw new Exception("Missing message series for imported NMs");
        }

        // Update parameters
        params.remove("data");
        params.setProperty("seriesId", batchData.getSeriesId());
        params.setProperty("tagName", batchData.getTagName());
    }


    /**
     * Defines the parameters used when starting an import of legacy NW messages
     */
    public class ImportLegacyNmParams implements IJsonSerializable {

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
