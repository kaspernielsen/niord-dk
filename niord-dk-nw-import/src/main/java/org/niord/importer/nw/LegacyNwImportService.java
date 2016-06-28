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
import org.niord.core.message.MessageSeries;
import org.niord.core.message.MessageSeriesService;
import org.niord.core.settings.Setting;
import org.niord.core.settings.SettingsService;
import org.niord.core.util.TextUtils;
import org.niord.core.util.TimeUtils;
import org.niord.importer.nw.LegacyNwImportRestService.ImportLegacyNwParams;
import org.niord.model.vo.MainType;
import org.niord.model.vo.Status;
import org.niord.model.vo.Type;
import org.niord.model.vo.geojson.LineStringVo;
import org.niord.model.vo.geojson.MultiPointVo;
import org.niord.model.vo.geojson.PointVo;
import org.niord.model.vo.geojson.PolygonVo;
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
import java.util.Date;
import java.util.List;

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
     * Registers the start date of the legacy message import.
     * By default, pick the first day of the year
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
    MessageSeriesService messageSeriesService;

    /** Returns or creates new parameters */
    public ImportLegacyNwParams getImportLegacyNwParams() {
        ImportLegacyNwParams params = settingsService.getFromJson(LEGACY_NW_IMPORT_PARAMS, ImportLegacyNwParams.class);
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
            message.setLegacyId(String.valueOf(messageId));
            message.setMainType(MainType.NW);
            message.setCreated(created);
            message.setUpdated(updated);
            message.setVersion(version);
            DateInterval dateInterval = new DateInterval();
            dateInterval.setFromDate(validFrom);
            dateInterval.setToDate((validTo != null) ? validTo : deleted);
            message.addDateInterval(dateInterval);
            message.setPublishDate(validFrom);

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
            messageSeriesService.updateMessageSeriesIdentifiers(message, false);

            // Status
            Date now = new Date();
            Status status = Status.PUBLISHED;
            if (deleted != null && statusDraft) {
                status = Status.DELETED;
            } else if (deleted != null && validTo != null && deleted.after(validTo)) {
                status = Status.EXPIRED;
                message.setUnpublishDate(validTo);
            } else if (deleted != null) {
                status = Status.CANCELLED;
                message.setUnpublishDate(deleted);
            } else if (statusDraft) {
                status = Status.DRAFT;
            } else if (validTo != null && now.after(validTo)) {
                status = Status.EXPIRED;
                message.setUnpublishDate(validTo);
            }
            message.setStatus(status);

            // Message Desc
            String titleDa = title;
            String titleEn = title;
            if (title != null && title.indexOf('/') != -1) {
                // By convention, the "enctext" field is the Danish title.
                // However, if it contains a "/" character, it is the "Danish / English" title
                titleDa = title.substring(0, title.indexOf('/')).trim();
                titleEn = title.substring(title.indexOf('/') + 1).trim();
            }
            if (StringUtils.isNotBlank(titleEn) || StringUtils.isNotBlank(descriptionEn) || StringUtils.isNotBlank(area3En)) {
                MessageDesc descEn = message.checkCreateDesc("en");
                descEn.setSubject(titleEn);
                descEn.setDescription(TextUtils.txt2html(descriptionEn));
                descEn.setVicinity(area3En);
            }
            if (StringUtils.isNotBlank(titleDa) || StringUtils.isNotBlank(descriptionDa) || StringUtils.isNotBlank(area3Da)) {
                MessageDesc descDa = message.checkCreateDesc("da");
                descDa.setSubject(titleDa);
                descDa.setDescription(TextUtils.txt2html(descriptionDa));
                descDa.setVicinity(area3Da);
            }
            message.setAutoTitle(true);

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
            message.setGeometry(featureCollection);
        }
    }


    /** GeoJSON linear rings has the same start and end coordinate */
    private double[][] toLinearRing(double[][] coords) {
        double[][] linearRing = new double[coords.length + 1][];
        System.arraycopy(coords, 0, linearRing, 0, coords.length);
        linearRing[coords.length] = coords[0];
        return linearRing;
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
                area.createDesc("en").setName(nameEn);
            }
            if (StringUtils.isNotBlank(nameDa)) {
                area.createDesc("da").setName(nameDa);
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
