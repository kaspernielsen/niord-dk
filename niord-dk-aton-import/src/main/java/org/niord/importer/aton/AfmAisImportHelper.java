package org.niord.importer.aton;

import org.apache.poi.ss.usermodel.Row;
import org.niord.core.aton.AtonNode;
import org.niord.core.user.User;

import java.util.Map;

/**
 * Helper class used for converting the legacy "AIS" data into OSM seamark format.
 */
public class AfmAisImportHelper extends BaseAtonImportHelper {

    public static final String[] FIELDS = {
            "TBD" };

    /** Constructor **/
    public AfmAisImportHelper(User user, int changeset, Map<String, Integer> colIndex, Row row) {
        super(user, changeset, colIndex, row);
    }


    /** Converts the current AFM row into an OSM AtoN **/
    public AtonNode afm2osm() throws Exception {
        AtonNode aton = new AtonNode();
        return aton;
    }
}
