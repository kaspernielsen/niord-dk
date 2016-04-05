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

import org.jboss.security.annotation.SecurityDomain;
import org.slf4j.Logger;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

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
    LegacyNwDatabase legacyNwDatabase;

    /**
     * Tests the database connection and returns success or failure
     * @return if the database connection works
     */
    @GET
    @Path("/test-connection")
    public boolean testConnection() {
        return legacyNwDatabase.testConnection(true);
    }

}
