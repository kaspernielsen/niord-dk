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

import org.apache.commons.fileupload.FileItem;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.batch.AbstractBatchableRestService;
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
        log.info("BATCH JOB " + batchJobName + " and params " + params);
        throw new Exception("GED MED GED PÅ");
    }

}
