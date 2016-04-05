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
import org.niord.core.settings.annotation.Setting;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import static org.niord.core.settings.Setting.Type.Password;

/**
 * Defines the interface to the Danish legacy NW database
 */
@Stateless
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

    // This is the location where a backup of legacy NW data can be fetched.
    // NB: There is no sensitive data in the dump at all, so the location is not a secret...
    @Inject
    @Setting(value = "legacyNwDbLocation", defaultValue = "http://msi.dma.dk/msi-safe-dump.sql.gz",
            description = "Location of legacy NW database dump")
    String dbLocation;

    // The next fields define the local mysql database to which the dump above will be imported
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
        // TEST TEST TEST
        downloadAndImportLegacyNwDump();

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


    /** Downloads a legacy NW database dump and imports it to the local legacy NW mysql database */
    public void downloadAndImportLegacyNwDump() {

        try {
            // Download step
            long t0 = System.currentTimeMillis();
            File dbFile = downloadLegacyNwDump();
            log.info(String.format("File %s decompressed to %s in %d ms",
                    dbLocation, dbFile, System.currentTimeMillis() -  t0));

            // Import step
            t0 = System.currentTimeMillis();
            importLegacyNwDump(dbFile);
            log.info(String.format("File %s imported in %d ms",
                    dbFile, System.currentTimeMillis() -  t0));

            // Delete the file
            log.info("Deleted legacy dump file with result: " + dbFile.delete());

        } catch (IOException | SQLException e) {
            log.error("Error downloading and importing legacy NW database dump", e);
        }
    }


    /** Downloads a legacy NW database dump */
    private File downloadLegacyNwDump() throws IOException {
        byte[] buffer = new byte[1024];

        File decompressedFile = File.createTempFile("msi-safe-dump-", ".sql");

        try (InputStream in = new URL(dbLocation).openStream();
             GZIPInputStream gzipInputStream = new GZIPInputStream(in);
             FileOutputStream fileOutputStream = new FileOutputStream(decompressedFile)) {

            int bytes_read;
            while ((bytes_read = gzipInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytes_read);
            }
            return decompressedFile;
        }
    }


    /** Imports a legacy NW database dump to a local mysql database */
    private void importLegacyNwDump(File file) throws SQLException, IOException {
        try (Connection con = openConnection();
             Statement stmt = con.createStatement();
             BufferedReader bf = new BufferedReader(new FileReader(file))) {

            StringBuilder sql = new StringBuilder();
            String line;
            while ((line = bf.readLine()) != null) {

                // Skip blank lines and comments
                if (StringUtils.isBlank(line) || line.startsWith("--")) {
                    continue;
                }

                sql.append(line).append("\n");
                if (line.endsWith(";")) {
                    stmt.executeUpdate(sql.toString());
                    sql = new StringBuilder();
                }
            }
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
