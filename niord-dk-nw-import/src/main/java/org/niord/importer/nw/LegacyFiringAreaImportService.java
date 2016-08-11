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
import org.niord.core.area.AreaService;
import org.niord.core.conf.TextResource;
import org.niord.core.geojson.JtsConverter;
import org.niord.model.vo.AreaType;
import org.niord.model.vo.geojson.PointVo;
import org.niord.model.vo.geojson.PolygonVo;
import org.slf4j.Logger;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.inject.Inject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

/**
 * Imports firing areas from a local db dump of the Danish MSI database
 */
@Stateless
public class LegacyFiringAreaImportService {

    @Inject
    Logger log;

    @Inject
    @TextResource("/sql/fa_all_firing_areas.sql")
    String allFiringAreasSql;

    @Inject
    @TextResource("/sql/fa_location_data.sql")
    String geometrySql;

    @Inject
    LegacyNwDatabase db;

    @Inject
    AreaService areaService;

    /**
     * Imports the firing areas
     * @param importDb whether to import the database first
     */
    public Map<Integer, Area> importFiringAreas(boolean importDb) throws Exception {

        if (importDb) {
            db.downloadAndImportLegacyNwDump();
        }

        try (Connection con = db.openConnection();
             PreparedStatement faStmt = con.prepareStatement(allFiringAreasSql);
             PreparedStatement geoStmt = con.prepareStatement(geometrySql)) {


            // First load all areas
            Map<Integer, Area> areas = new LinkedHashMap<>();
            ResultSet rs = faStmt.executeQuery();
            while (rs.next()) {
                Integer id          = getInt(rs, "id");
                Integer active      = getInt(rs, "active");
                String  area1En     = getString(rs, "area1_en");
                String  area1Da     = getString(rs, "area1_da");
                String  area2En     = getString(rs, "area2_en");
                String  area2Da     = getString(rs, "area2_da");
                String  area3En     = getString(rs, "area3_en");
                String  area3Da     = getString(rs, "area3_da");

                Area area = createAreaTemplate(area1En, area1Da, null);
                area = createAreaTemplate(area2En, area2Da, area);
                area = createAreaTemplate(area3En, area3Da, area);
                area.setActive(active == 1);
                areas.put(id, area);
            }
            rs.close();


            // Next, load area locations
            for (Map.Entry<Integer, Area> a : areas.entrySet()) {
                log.debug("Reading geometry for firing area " + a.getKey());
                geoStmt.setInt(1, a.getKey());


                ResultSet grs = geoStmt.executeQuery();
                List<double[]> coords = new ArrayList<>();
                while (grs.next()) {
                    Integer latDeg      = getInt(grs, "lat_deg");
                    Double  latMin      = getDouble(grs, "lat_min");
                    String  latDir      = getString(grs, "lat_dir");
                    Integer lonDeg      = getInt(grs, "lon_deg");
                    Double  lonMin      = getDouble(grs, "lon_min");
                    String  lonDir      = getString(grs, "lon_dir");

                    if (latDeg != null && latMin != null && lonDeg != null && lonMin != null) {
                        double lon = lonDeg.doubleValue() + lonMin / 60.0;
                        if ("W".equalsIgnoreCase(lonDir)) {
                            lon = -lon;
                        }
                        double lat = latDeg.doubleValue() + latMin / 60.0;
                        if ("S".equalsIgnoreCase(latDir)) {
                            lat = -lat;
                        }
                        coords.add(new double[]{ lon, lat });
                    }
                }
                grs.close();

                // Only handle points and polygons
                Geometry geometry = null;
                if (coords.size() == 1) {
                    PointVo pt = new PointVo();
                    pt.setCoordinates(coords.get(0));
                    geometry = JtsConverter.toJts(pt);
                } else if (coords.size() > 2) {
                    // GeoJSON linear rings has the same start and end coordinate
                    coords.add(coords.get(0));
                    double[][] ring = new double[coords.size()][];
                    for (int x = 0; x < coords.size(); x++) {
                        ring[x] = coords.get(x);
                    }
                    PolygonVo p = new PolygonVo();
                    p.setCoordinates(new double[][][] { ring });
                    geometry = JtsConverter.toJts(p);
                }

                if (geometry != null) {
                    a.getValue().setGeometry(geometry);
                }
            }


            log.info("Fetched " + areas.size() + " legacy Firing Areas");
            return areas;
        }
    }


    /**
     * Merges the given area template into the area tree
     * @param areaTemplate the area to merge into the three
     */
    @TransactionAttribute(REQUIRES_NEW)
    public void mergeArea(Area areaTemplate) throws Exception {

        try {
            Area area = areaService.findOrCreateArea(areaTemplate, true);
            if (area != null) {
                // If an existing area was found, check if we need to updated various fields
                boolean updated = false;
                if (area.isActive() != areaTemplate.isActive()) {
                    area.setActive(areaTemplate.isActive());
                    area.updateActiveFlag();
                    updated = true;
                    log.info("Updated active flag of area " + area.getId());
                }
                if (area.getGeometry() == null && areaTemplate.getGeometry() != null) {
                    area.setGeometry(areaTemplate.getGeometry());
                    updated = true;
                    log.info("Updated geometry of area " + area.getId());
                }
                if (area.getType() != AreaType.FIRING_AREA) {
                    area.setType(AreaType.FIRING_AREA);
                    updated = true;
                    log.info("Updated type of area " + area.getId());
                }
                if (updated) {
                    areaService.saveEntity(area);
                }
            }
        } catch (Exception e) {
            log.error("Error updating imported legacy firing area " + areaTemplate.getDescs());
            throw e;
        }
    }


    /**
     * Creates an Area template based on the given Danish and English name
     * and optionally a parent Area
     * @param nameEn English name
     * @param nameDa Danish name
     * @param parent parent area
     * @return the Area template, or null if the names are empty
     */
    public static Area createAreaTemplate(String nameEn, String nameDa, Area parent) {
        Area area = null;
        if (StringUtils.isNotBlank(nameEn) || StringUtils.isNotBlank(nameDa)) {
            area = new Area();
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
}
