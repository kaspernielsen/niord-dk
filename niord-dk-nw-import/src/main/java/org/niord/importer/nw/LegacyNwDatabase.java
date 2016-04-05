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
import org.niord.core.settings.Setting;
import org.niord.core.settings.SettingsService;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Objects;
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

    // This is the location where a backup of legacy NW data dump can be fetched.
    // NB: There is no sensitive data in the dump at all, so the location is not a secret...
    private static final Setting DB_LOCATION =
            new Setting("legacyNwDbLocation", "http://msi.dma.dk/msi-safe-dump.sql.gz")
                    .description("Location of legacy NW database dump")
                    .editable(true);

    // The MD5 checksum for the last imported legacy NW data dump
    private static final Setting DB_CHECKSUM =
            new Setting("legacyNwDbChecksum")
                    .description("MD5 checksum of legacy NW database dump")
                    .editable(false)
                    .cached(false);

    // The next fields define the local mysql database to which the dump above will be imported
    private static final Setting DB_URL =
            new Setting("legacyNwDbUrl", "jdbc:mysql://localhost:3306/oldmsi?useSSL=false")
                    .description("JDBC Url to the legacy NW database")
                    .editable(true);

    private static final Setting DB_USER =
            new Setting("legacyNwDbUser", "oldmsi")
                    .description("Database user to the legacy NW database")
                    .editable(true);

    private static final Setting DB_PASSWORD =
            new Setting("legacyNwDbPassword", "oldmsi")
                    .description("Database password to the legacy NW database")
                    .type(Password)
                    .editable(true);

    @Inject
    Logger log;

    @Inject
    SettingsService settingsService;


    /**
     * Returns a new connection to the legacy database.
     * Important to close the connection afterwards.
     * @return a new connection to the legacy database.
     */
    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
                settingsService.getString(DB_URL),
                settingsService.getString(DB_USER),
                settingsService.getString(DB_PASSWORD));
    }


    /**
     * Tests the database connection and returns success or failure
     * @param containsData check that the database contains message data as well
     * @return success or failure in accessing legacy NW database
     */
    public boolean testConnection(boolean containsData) {
        // TEST TEST TEST
        downloadAndImportLegacyNwDump();

        try {
            try (Connection con = openConnection();
                Statement stmt = con.createStatement()) {
                ResultSet rs;

                if (containsData) {
                    // Check that the database contains message data
                    rs = stmt.executeQuery("select count(*) from message");
                } else {
                    // Just check that the database exists
                    rs = stmt.executeQuery("select 1");
                }

                if (!rs.next()) {
                    throw new Exception("Error testing legacy NW database");
                }
                log.info("Testing legacy NW database. Result: " + rs.getInt(1));
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
            // Download legacy NW dump
            long t0 = System.currentTimeMillis();
            File dbFile = File.createTempFile("msi-safe-dump-", ".sql");

            String checksum = downloadLegacyNwDump(dbFile);
            log.info(String.format("File %s decompressed to %s in %d ms",
                    DB_LOCATION, dbFile, System.currentTimeMillis() -  t0));

            String oldChecksum = settingsService.getString(DB_CHECKSUM);

            if (Objects.equals(checksum, oldChecksum)) {
                log.info("Downloaded legacy NW dump unchanged. Skipping import");

            } else {
                // Import step
                t0 = System.currentTimeMillis();
                importLegacyNwDump(dbFile);
                log.info(String.format("File %s imported in %d ms",
                        dbFile, System.currentTimeMillis() -  t0));

                settingsService.set("legacyNwDbChecksum", checksum);
            }

            // Delete the file
            log.debug("Deleted legacy dump file with result: " + dbFile.delete());

        } catch (IOException | SQLException | NoSuchAlgorithmException e) {
            log.error("Error downloading and importing legacy NW database dump", e);
        }
    }


    /**
     * Downloads a legacy NW database dump
     * @param dbFile the file to save the database to
     * @return the MD5 checksum of the downloaded file
     */
    private String downloadLegacyNwDump(File dbFile) throws IOException, NoSuchAlgorithmException {
        MessageDigest m= MessageDigest.getInstance("MD5");
        byte[] buffer = new byte[1024];
        String url = settingsService.getString(DB_LOCATION);

        try (InputStream in = new URL(url).openStream();
             GZIPInputStream gzipInputStream = new GZIPInputStream(in);
             FileOutputStream fileOutputStream = new FileOutputStream(dbFile)) {

            int bytesRead;
            while ((bytesRead = gzipInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, bytesRead);
                m.update(buffer, 0, bytesRead);
            }

            // Return the checksum
            return new BigInteger(1, m.digest()).toString(16);
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
