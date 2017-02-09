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

import com.vividsolutions.jts.geom.Geometry;
import org.apache.commons.lang.StringUtils;
import org.niord.core.area.Area;
import org.niord.core.category.Category;
import org.niord.core.conf.TextResource;
import org.niord.core.domain.Domain;
import org.niord.core.domain.DomainService;
import org.niord.core.geojson.Feature;
import org.niord.core.geojson.FeatureCollection;
import org.niord.core.geojson.JtsConverter;
import org.niord.core.message.DateInterval;
import org.niord.core.message.Message;
import org.niord.core.message.MessageDesc;
import org.niord.core.message.MessagePart;
import org.niord.core.message.MessagePartDesc;
import org.niord.core.message.MessageSearchParams;
import org.niord.core.message.MessageSeries;
import org.niord.core.message.MessageSeriesService;
import org.niord.core.message.MessageService;
import org.niord.core.settings.Setting;
import org.niord.core.settings.SettingsService;
import org.niord.core.util.TextUtils;
import org.niord.core.util.TimeUtils;
import org.niord.importer.nw.LegacyNwImportRestService.ImportLegacyNwParams;
import org.niord.model.geojson.LineStringVo;
import org.niord.model.geojson.MultiPointVo;
import org.niord.model.geojson.PointVo;
import org.niord.model.geojson.PolygonVo;
import org.niord.model.message.MainType;
import org.niord.model.message.Status;
import org.niord.model.message.Type;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Imports messages from a local db dump of the Danish MSI database
 */
@Stateless
public class LegacyNwImportService {

    @Inject
    Logger log;

    @Inject
    SettingsService settingsService;

    /**
     * Registers the start date of the legacy message import along
     * with the messages series to use, optionally a message tag,
     * and whether or not to auto-import messages.
     */
    private static final Setting LEGACY_NW_IMPORT_PARAMS
            = new Setting("legacyMsiImportParams")
            .type(Setting.Type.json)
            .description("Parameters used for importing legacy NW params")
            .editable(true)
            .cached(false);

    @Inject
    @TextResource("/sql/nw_all_message_ids.sql")
    String allMessagesSql;

    @Inject
    @TextResource("/sql/nw_active_message_ids.sql")
    String activeMessagesSql;

    @Inject
    @TextResource("/sql/nw_message_data.sql")
    String messageDataSql;

    @Inject
    @TextResource("/sql/nw_location_data.sql")
    String geometrySql;

    @Inject
    LegacyNwDatabase db;

    @Inject
    DomainService domainService;

    @Inject
    MessageService messageService;

    @Inject
    MessageSeriesService messageSeriesService;


    /** Returns the import parameters */
    public ImportLegacyNwParams getImportLegacyNwParams() {
        return settingsService.getFromJson(LEGACY_NW_IMPORT_PARAMS, ImportLegacyNwParams.class);
    }


    /** Returns or creates new parameters */
    public ImportLegacyNwParams getOrCreateImportLegacyNwParams() {
        ImportLegacyNwParams params = getImportLegacyNwParams();
        if (params == null) {
            params = new ImportLegacyNwParams();
            params.setStartImportDate(TimeUtils.getDate(null, 0, 1));
            Domain domain = domainService.currentDomain();
            if (domain != null) {
                params.setSeriesId(domain.getMessageSeries().stream()
                    .filter(s -> MainType.NW.equals(s.getMainType()))
                    .map(MessageSeries::getSeriesId)
                    .findFirst()
                    .orElse(null));
            }
        }
        return params;
    }


    /** Updates the batch import parameters */
    public void updateImportLegacyNwParams(ImportLegacyNwParams params) {
        params.setLastUpdated(new Date());
        settingsService.set(LEGACY_NW_IMPORT_PARAMS.getKey(), params);
    }


    /**
     * Returns the IDs of the legacy NWs to import
     * @param params the import parameters
     * @param importDb whether to import the database first
     * @return the IDs of legacy NWs to import
     */
    public List<Integer> getImportLegacyNwIds(ImportLegacyNwParams params, boolean importDb) throws Exception {

        if (importDb) {
            db.downloadAndImportLegacyNwDump();
        }

        try (Connection con = db.openConnection();
             PreparedStatement stmt = con.prepareStatement(allMessagesSql)) {
            stmt.setTimestamp(1, new Timestamp(params.getStartImportDate().getTime()));

            List<Integer> result = new ArrayList<>();
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                result.add(getInt(rs, "id"));
            }
            rs.close();

            log.info("Fetched " + result.size() + " legacy NW ids");
            return result;
        }
    }


    /**
     * Returns the IDs of the active legacy NWs to import or update (i.e. cancel)
     * @param params the import parameters
     * @param importDb whether to import the database first
     * @return the IDs of legacy NWs to import
     */
    public List<Integer> getActiveLegacyNwIds(ImportLegacyNwParams params, boolean importDb) throws Exception {

        if (importDb) {
            db.downloadAndImportLegacyNwDump();
        }


        // First, find the published messages with the given message series
        MessageSearchParams publishedMsgParams = new MessageSearchParams()
                .statuses(toSet(Status.PUBLISHED))
                .seriesIds(toSet(params.getSeriesId(), params.getLocalSeriesId()));
        Set<Integer> publishedNiordIds = messageService.search(publishedMsgParams)
                .getData()
                .stream()
                .filter(m -> m.getLegacyId() != null && m.getLegacyId().matches("^-?\\d+$"))
                .map(m -> Integer.valueOf(m.getLegacyId()))
                .collect(Collectors.toSet());
        log.debug("Found " + publishedNiordIds.size() + " published legacy messages in Niord");


        // Next, find the active legacy messages
        Set<Integer> publishedLegacyMsiIds = new HashSet<>();
        try (Connection con = db.openConnection();
             PreparedStatement stmt = con.prepareStatement(activeMessagesSql)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                publishedLegacyMsiIds.add(getInt(rs, "id"));
            }
            rs.close();
            log.debug("Found " + publishedLegacyMsiIds.size() + " active legacy NW ids");
        }


        // The resulting set of IDs are a combination of:
        // 1) Those messages that are published in Niord but not in the legacy MSI system
        // 2) Those messages that are published in the legacy MSI system, but not (present) in Niord
        List<Integer> result = new ArrayList<>();
        publishedNiordIds.stream()
                .filter(id -> !publishedLegacyMsiIds.contains(id))
                .filter(this::legacyIdExists)
                .forEach(result::add);
        publishedLegacyMsiIds.stream()
                .filter(id -> !publishedNiordIds.contains(id))
                .forEach(result::add);

        if (!result.isEmpty()) {
            log.info("New or cancelled legacy NW IDs " + result);
        }

        return result;
    }


    /** Validates that the legacy ID exists in the legacy MSI system or not **/
    public boolean legacyIdExists(Integer id) {
        try {
            Object result = db.getSingleResult("select count(*) from message where id = " + id);
            return ((Number)result).intValue() == 1;
        } catch (Exception e) {
            return false;
        }
    }


    /**
     * Reads the legacy NW data for the given message.
     *
     * @param id the ID of the NW to read in
     */
    @SuppressWarnings("unused")
    public Message readMessage(ImportLegacyNwParams params, Integer id) throws SQLException {

        // Inject the id into the SQL
        String sql = messageDataSql.replace(":id", id.toString());

        // Execute the DB query
        try (Connection con = db.openConnection();
             Statement stmt = con.createStatement()) {

            ResultSet rs = stmt.executeQuery(sql);

            if (!rs.next()) {
                // Should never happen...
                throw new SQLException("Legacy NW with ID " + id + " does not exist");
            }

            Integer messageId           = getInt(rs, "messageId");
            Boolean statusDraft         = getBoolean(rs, "statusDraft");
            Boolean latest              = getBoolean(rs, "latest");
            String  navtexNo            = getString(rs, "navtexNo");
            String  descriptionEn       = getString(rs, "description_en");
            String  descriptionDa       = getString(rs, "description_da");
            String  title               = getString(rs, "title");
            Date    validFrom           = getDate(rs, "validFrom");
            Date    validTo             = getDate(rs, "validTo");
            Date    created             = getDate(rs, "created");
            Date    updated             = getDate(rs, "updated");
            Date    deleted             = getDate(rs, "deleted");
            Integer version             = getInt(rs, "version");
            String  priority            = getString(rs, "priority");
            String  messageType         = getString(rs, "messageType");
            Integer category1Id         = getInt(rs, "category1_id");
            String  category1En         = getString(rs, "category1_en");
            String  category1Da         = getString(rs, "category1_da");
            Integer category2Id         = getInt(rs, "category2_id");
            String  category2En         = getString(rs, "category2_en");
            String  category2Da         = getString(rs, "category2_da");
            Integer area1Id             = getInt(rs, "area1_id");
            String  area1En             = getString(rs, "area1_en");
            String  area1Da             = getString(rs, "area1_da");
            Integer area2Id             = getInt(rs, "area2_id");
            String  area2En             = getString(rs, "area2_en");
            String  area2Da             = getString(rs, "area2_da");
            String  area3En             = getString(rs, "area3_en");
            String  area3Da             = getString(rs, "area3_da");
            String  locationType        = getString(rs, "locationType");
            rs.close();

            Message message = new Message();
            MessagePart part = new MessagePart();
            message.addPart(part);
            part.setHideSubject(true);

            message.setLegacyId(String.valueOf(id));
            message.setMainType(MainType.NW);
            message.setCreated(created);
            message.setUpdated(updated);
            message.setVersion(version);
            DateInterval dateInterval = new DateInterval();
            dateInterval.setFromDate(validFrom);
            dateInterval.setToDate((validTo != null) ? validTo : deleted);
            part.addEventDates(dateInterval);
            message.setPublishDateFrom(validFrom);

            if (StringUtils.isNotBlank(navtexNo) && navtexNo.split("-").length == 3) {
                // Extract the series identifier from the navtex number
                String[] parts = navtexNo.split("-");
                int number = Integer.valueOf(parts[1]);
                message.setNumber(number);
                message.setType(Type.SUBAREA_WARNING);
                message.setMessageSeries(messageSeriesService.findBySeriesId(params.getSeriesId()));
            } else {
                message.setType(Type.LOCAL_WARNING);
                message.setMessageSeries(messageSeriesService.findBySeriesId(params.getLocalSeriesId()));
            }

            // Status
            Date now = new Date();
            Status status = Status.PUBLISHED;
            if (latest != null && !latest) {
                status = Status.DELETED;
            } else if (deleted != null && statusDraft) {
                status = Status.DELETED;
            } else if (deleted != null && validTo != null && deleted.after(validTo)) {
                status = Status.EXPIRED;
                message.setPublishDateTo(validTo);
            } else if (deleted != null) {
                status = Status.CANCELLED;
                message.setPublishDateTo(deleted);
            } else if (statusDraft) {
                status = Status.DRAFT;
            } else if (validTo != null && now.after(validTo)) {
                status = Status.EXPIRED;
                message.setPublishDateTo(validTo);
            }
            message.setStatus(status);

            // Update the message according to the associated message series
            messageSeriesService.updateMessageIdsFromMessageSeries(message, false);

            // Message Desc
            String titleDa = title;
            String titleEn = title;
            if (title != null && title.indexOf('/') != -1) {
                // By convention, the "enctext" field is the Danish title.
                // However, if it contains a "/" character, it is the "Danish / English" title
                titleDa = title.substring(0, title.indexOf('/')).trim();
                titleEn = title.substring(title.indexOf('/') + 1).trim();
            }
            if (StringUtils.isNotBlank(area3En)) {
                MessageDesc descEn = message.checkCreateDesc("en");
                descEn.setVicinity(area3En);
            }
            if (StringUtils.isNotBlank(area3Da)) {
                MessageDesc descDa = message.checkCreateDesc("da");
                descDa.setVicinity(area3Da);
            }
            message.setAutoTitle(true);

            // Message Part Desc
            if (StringUtils.isNotBlank(titleEn) || StringUtils.isNotBlank(descriptionEn)) {
                MessagePartDesc descEn = part.checkCreateDesc("en");
                descEn.setSubject(titleEn);
                descEn.setDetails(TextUtils.txt2html(descriptionEn));
            }
            if (StringUtils.isNotBlank(titleDa) || StringUtils.isNotBlank(descriptionDa)) {
                MessagePartDesc descDa = part.checkCreateDesc("da");
                descDa.setSubject(titleDa);
                descDa.setDetails(TextUtils.txt2html(descriptionDa));
            }

            // Areas
            Area area = createAreaTemplate(area1Id, area1En, area1Da, null);
            // Annoyingly, legacy data has Danmark as a sub-area of Danmark
            if (!StringUtils.equals(area1En, area2En) || !StringUtils.equals(area1Da, area2Da)) {
                area = createAreaTemplate(area2Id, area2En, area2Da, area);
            }
            if (area != null) {
                message.getAreas().add(area);
            }

            // Categories
            /*
            Category category = createCategoryTemplate(category1Id, category1En, category1Da, null);
            category = createCategoryTemplate(category2Id, category2En, category2Da, category);
            if (category != null) {
                message.getCategories().add(category);
            }
            */

            // Read the location type
            readLocationData(stmt, message, id, locationType);

            return message;
        }
    }


    /**
     * Reads the location for the legacy MSI message.
     *
     * @param message the message to read the location for
     */
    private void readLocationData(Statement stmt, Message message, Integer id, String locationType) throws SQLException {

        // Inject the id into the SQL
        String sql = geometrySql.replace(":id", id.toString());

        List<double[]> coordList = new ArrayList<>();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            //Integer pointIndex      = getInt(rs, "pointIndex");
            Double pointLatitude    = getDouble(rs, "pointLatitude");
            Double pointLongitude   = getDouble(rs, "pointLongitude");
            //Integer pointRadius     = getInt(rs, "pointRadius");
            if (pointLatitude != null || pointLongitude != null) {
                coordList.add(new double[]{pointLongitude, pointLatitude});
            }
        }

        // Convert points to an array
        double[][] coords = new double[coordList.size()][];
        for (int x = 0; x < coordList.size(); x++) {
            coords[x] = coordList.get(x);
        }

        Geometry geometry = null;
        if (coords.length == 1) {
            PointVo pt = new PointVo();
            pt.setCoordinates(coords[0]);
            geometry = JtsConverter.toJts(pt);
        } else if (("Point".equals(locationType) || "Points".equals(locationType)) && coords.length >= 1) {
            MultiPointVo pts = new MultiPointVo();
            pts.setCoordinates(coords);
            geometry = JtsConverter.toJts(pts);
        } else if ("Polyline".equals(locationType) && coords.length > 1) {
            LineStringVo l = new LineStringVo();
            l.setCoordinates(coords);
            geometry = JtsConverter.toJts(l);
        } else if ("Polygon".equals(locationType) && coords.length > 2) {
            // GeoJSON linear rings has the same start and end coordinate
            coords = toLinearRing(coords);
            PolygonVo p = new PolygonVo();
            p.setCoordinates(new double[][][] { coords });
            geometry = JtsConverter.toJts(p);
        }

        if (geometry != null) {
            FeatureCollection featureCollection = new FeatureCollection();
            Feature feature = new Feature();
            featureCollection.addFeature(feature);
            feature.setGeometry(geometry);
            message.getParts().get(0).setGeometry(featureCollection);
        }
    }


    /** GeoJSON linear rings has the same start and end coordinate */
    public static double[][] toLinearRing(double[][] coords) {
        double[][] linearRing = new double[coords.length + 1][];
        System.arraycopy(coords, 0, linearRing, 0, coords.length);
        linearRing[coords.length] = coords[0];
        return linearRing;
    }


    /**
     * Handle known spelling-mistakes in the MSI-editor
     * @param name the area name
     * @return the corrected name
     */
    private static String fixAreaSpellingMistakes(String name) {
        if (name.equalsIgnoreCase("Skagerak")) {
            return "Skagerrak";
        }
        return name;
    }


    /**
     * Creates an Area template based on the given Danish and English name
     * and optionally a parent Area
     * @param id the id of the area
     * @param nameEn English name
     * @param nameDa Danish name
     * @param parent parent area
     * @return the Area template, or null if the names are empty
     */
    public static Area createAreaTemplate(Integer id, String nameEn, String nameDa, Area parent) {
        Area area = null;
        if (id != null && (StringUtils.isNotBlank(nameEn) || StringUtils.isNotBlank(nameDa))) {
            area = new Area();
            area.setId(id);
            if (StringUtils.isNotBlank(nameEn)) {
                area.createDesc("en").setName(fixAreaSpellingMistakes(nameEn));
            }
            if (StringUtils.isNotBlank(nameDa)) {
                area.createDesc("da").setName(fixAreaSpellingMistakes(nameDa));
            }
            area.setParent(parent);
        }
        return area;
    }


    /**
     * Creates an Category template based on the given Danish and English name
     * and optionally a parent Category
     * @param id the id of the category
     * @param nameEn English name
     * @param nameDa Danish name
     * @param parent parent area
     * @return the Category template, or null if the names are empty
     */
    @SuppressWarnings("unused")
    public static Category createCategoryTemplate(Integer id, String nameEn, String nameDa, Category parent) {
        Category category = null;
        if (id != null && (StringUtils.isNotBlank(nameEn) || StringUtils.isNotBlank(nameDa))) {
            category = new Category();
            category.setId(id);
            if (StringUtils.isNotBlank(nameEn)) {
                category.createDesc("en").setName(nameEn);
            }
            if (StringUtils.isNotBlank(nameDa)) {
                category.createDesc("da").setName(nameDa);
            }
            category.setParent(parent);
        }
        return category;
    }


    /** Utility funciton that converts an array to a set **/
    @SafeVarargs
    private final <T> Set<T> toSet(T... vals) {
        Set<T> result = new HashSet<>();
        if (vals != null) {
            Arrays.stream(vals).forEach(result::add);
        }
        return result;
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
