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
import java.util.Map;

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
    protected void checkBatchJob(String batchJobName, FileItem fileItem, Map<String, Object> params) throws Exception {

        ImportLegacyNmParams batchData;
        try {
            batchData = new ObjectMapper().readValue((String)params.get("data"), ImportLegacyNmParams.class);
        } catch (IOException e) {
            throw new Exception("Missing batch data with tag and message series", e);
        }

        if (StringUtils.isBlank(batchData.getSeriesId())) {
            throw new Exception("Missing message series for imported NMs");
        }

        // Update parameters
        params.remove("data");
        params.put("seriesId", batchData.getSeriesId());
        params.put("tagId", batchData.getTagId());
    }


    /**
     * Defines the parameters used when starting an import of legacy NW messages
     */
    public static class ImportLegacyNmParams implements IJsonSerializable {

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
