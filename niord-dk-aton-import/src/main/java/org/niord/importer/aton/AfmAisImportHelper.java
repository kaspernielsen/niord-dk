package org.niord.importer.aton;

import org.apache.poi.ss.usermodel.Row;
import org.niord.core.model.AtonNode;

import java.util.Map;

/**
 * Helper class used for converting the legacy "AIS" data into OSM seamark format.
 */
public class AfmAisImportHelper extends BaseAtonImportHelper {

    public static final String[] FIELDS = {
            "TBD" };

    /** Constructor **/
    public AfmAisImportHelper(Map<String, Integer> colIndex, Row row) {
        super(colIndex, row);
    }


    /** Converts the current AFM row into an OSM AtoN **/
    public AtonNode afm2osm() throws Exception {
        AtonNode aton = new AtonNode();
        return aton;
    }
}
