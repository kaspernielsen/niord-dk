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
package org.niord.importer.nw;

import org.apache.commons.lang.StringUtils;
import org.niord.core.settings.Setting;
import org.niord.core.settings.SettingsService;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;
import java.util.zip.GZIPInputStream;

import static org.niord.core.settings.Setting.Type.Password;

/**
 * Defines the interface to the Danish legacy NW database
 * <p>
 * Note to hackers: The database dump that is fetched from http://msi.dma.dk/msi-safe-dump.sql.gz
 * contains no sensitive data and everybody is welcome to download it...
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
            new Setting("legacyNwDbUrl", "jdbc:mysql://localhost:3306/oldmsi?useSSL=false&useUnicode=true&characterEncoding=utf8")
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


    /**
     * Downloads a legacy NW database dump and imports it to the local legacy NW mysql database.
     * The database will only be imported if the dump file has changed since last import.
     *
     * @return if the database was updated
     */
    public boolean downloadAndImportLegacyNwDump() throws Exception {

        // Test that we have a valid database connection
        if (!testConnection(false)) {
            throw new Exception("Invalid legacy NW database connection");
        }

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

                log.debug("Deleting legacy NW dump file with result: " + dbFile.delete());
                return false;

            } else {
                // Import step
                t0 = System.currentTimeMillis();
                importLegacyNwDump(dbFile);
                log.info(String.format("File %s imported in %d ms",
                        dbFile, System.currentTimeMillis() -  t0));

                settingsService.set("legacyNwDbChecksum", checksum);

                log.debug("Deleting legacy NW dump file with result: " + dbFile.delete());
                return true;
            }

        } catch (IOException | SQLException | NoSuchAlgorithmException e) {
            log.error("Error downloading and importing legacy NW database dump", e);
            throw new Exception("Failed downloading and importing legacy NW database dump", e);
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
            fileOutputStream.flush();

            // Return the checksum
            return new BigInteger(1, m.digest()).toString(16);
        }
    }


    /**
     * Imports a legacy NW database dump to a local mysql database
     * @param dbFile the file to import data from
     */
    private void importLegacyNwDump(File dbFile) throws SQLException, IOException {
        try (Connection con = openConnection();
             Statement stmt = con.createStatement();
             FileInputStream fileIn = new FileInputStream(dbFile);
             InputStreamReader in = new InputStreamReader(fileIn, "utf-8");
             BufferedReader bf = new BufferedReader(in)) {

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
}
