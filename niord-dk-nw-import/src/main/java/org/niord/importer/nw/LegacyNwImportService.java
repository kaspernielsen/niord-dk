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

import org.niord.core.conf.TextResource;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Imports messages from a local db dump of the Danish MSI database
 */
@Stateless
@SuppressWarnings("unused")
public class LegacyNwImportService {

    @Inject
    Logger log;

    @Inject
    @TextResource("/sql/nw_active_message_ids.sql")
    String activeNwSql;

    @Inject
    LegacyNwDatabase db;


    /**
     * Returns the IDs of the active legacy NWs
     * @param importDb whether to import the database first
     * @return the IDs of active legacy NWs
     */
    public List<Integer> getActiveLegacyNwIds(boolean importDb) throws Exception {

        if (importDb) {
            db.downloadAndImportLegacyNwDump();
        }

        try (Connection con = db.openConnection();
             Statement stmt = con.createStatement()) {
            ResultSet rs;

            List<Integer> result = new ArrayList<>();
            rs = stmt.executeQuery(activeNwSql);
            while (rs.next()) {
                result.add(getInt(rs, "id"));
            }
            rs.close();

            log.info("Fetched " + result.size() + " active legacy NW ids");
            return result;
        }
    }




    /*************************/
    /** ResultSet accessors **/
    /*************************/

    String getString(ResultSet rs, String key) throws SQLException {
        String val = rs.getString(key);
        return rs.wasNull() ? null : val;
    }

    Integer getInt(ResultSet rs, String key) throws SQLException {
        Integer val = rs.getInt(key);
        return rs.wasNull() ? null : val;
    }

    Double getDouble(ResultSet rs, String key) throws SQLException {
        Double val = rs.getDouble(key);
        return rs.wasNull() ? null : val;
    }

    Date getDate(ResultSet rs, String key) throws SQLException {
        Timestamp val = rs.getTimestamp(key);
        return rs.wasNull() ? null : val;
    }

    Boolean getBoolean(ResultSet rs, String key) throws SQLException {
        boolean val = rs.getBoolean(key);
        return rs.wasNull() ? null : val;
    }

}
