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

import org.niord.core.settings.annotation.Setting;
import org.slf4j.Logger;

import javax.inject.Inject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;

import static org.niord.core.settings.Setting.Type.Password;

/**
 * Defines the interface to the Danish legacy NW database
 */
@SuppressWarnings("unused")
public class LegacyNwDatabase {

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    static {
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot initialize DB driver " + JDBC_DRIVER);
        }
    }

    @Inject
    Logger log;

    @Inject
    @Setting(value = "legacyNwDbUrl", defaultValue = "jdbc:mysql://localhost:3306/oldmsi?useSSL=false",
            description = "JDBC Url to the legacy NW database")
    String dbUrl;

    @Inject
    @Setting(value = "legacyNwDbUser", defaultValue = "oldmsi",
            description = "Database user to the legacy NW database")
    String dbUser;

    @Inject
    @Setting(value = "legacyNwDbPassword", defaultValue = "oldmsi", type = Password,
            description = "Database password to the legacy NW database")
    String dbPassword;


    /**
     * Returns a new connection to the legacy database.
     * Important to close the connection afterwards.
     * @return a new connection to the legacy database.
     */
    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }


    /**
     * Tests the database connection and returns success or failure
     * @return success or failure in accessing legacy NW database
     */
    public boolean testConnection() {
        try {

            try (Connection con = openConnection();
                Statement stmt = con.createStatement()) {
                ResultSet rs = stmt.executeQuery("select count(*) from message");
                if (!rs.next()) {
                    throw new Exception("No message table available in legacy NW database");
                }
                log.info("Testing connection to legacy NW database. Message count: " + rs.getInt(1));
                rs.close();
            }

            // Report success
            return true;

        } catch (Exception e) {
            log.error("Failed creating a legacy NW database connection", e);
            return false;
        }
    }


    public static String getString(ResultSet rs, String key) throws SQLException {
        String val = rs.getString(key);
        return rs.wasNull() ? null : val;
    }

    public static Integer getInt(ResultSet rs, String key) throws SQLException {
        Integer val = rs.getInt(key);
        return rs.wasNull() ? null : val;
    }

    public static Double getDouble(ResultSet rs, String key) throws SQLException {
        Double val = rs.getDouble(key);
        return rs.wasNull() ? null : val;
    }

    public static Date getDate(ResultSet rs, String key) throws SQLException {
        Timestamp val = rs.getTimestamp(key);
        return rs.wasNull() ? null : val;
    }


    public static Boolean getBoolean(ResultSet rs, String key) throws SQLException {
        boolean val = rs.getBoolean(key);
        return rs.wasNull() ? null : val;
    }

}
