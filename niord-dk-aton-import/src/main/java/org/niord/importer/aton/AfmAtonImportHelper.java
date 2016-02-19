package org.niord.importer.aton;

import org.apache.commons.lang.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.niord.core.model.AtonNode;

import java.util.Map;

/**
 * Helper class used for converting the legacy "AFM" data into OSM seamark format.
 */
public class AfmAtonImportHelper extends BaseAtonImportHelper {

    public static final String[] FIELDS = {
            "AFMSTATION", "FYRLBNR_DK", "AFM_NAVN", "PLADSNAVN", "AFUFORKORTELSE",
            "BESKRIVELSE", "LATTITUDE", "LONGITUDE", "KARAKNR", "EJER", "AJF_BRUGER", "AJF_DATO" };

    /** Constructor **/
    public AfmAtonImportHelper(Map<String, Integer> colIndex, Row row) {
        super(colIndex, row);
    }

    /** Converts the current AFM row into an OSM AtoN **/
    public AtonNode afm2osm() throws Exception {
        AtonNode aton = new AtonNode();

        // TODO: aton.setId();
        aton.setLat(numericValue("LATTITUDE"));
        aton.setLon(numericValue("LONGITUDE"));
        aton.setTimestamp(dateValue("AJF_DATO"));
        aton.setUser(stringValue("AJF_BRUGER"));
        aton.setUid(0);         // Unknown user ID
        aton.setChangeset(0);   // Unknown changeset
        aton.setVersion(1);     // Unknown version

        aton.updateTag(CUST_TAG_ATON_UID, stringValue("AFMSTATION"));
        if (StringUtils.isNotBlank(stringValue("FYRLBNR_DK"))) {
            aton.updateTag(CUST_TAG_LIGHT_NUMBER, stringValue("FYRLBNR_DK"));
        }

        return aton;
    }

    /*
        aton.setAtonUid(row.getCell(colIndex.get("AFMSTATION")).getStringCellValue());
        aton.setName(row.getCell(colIndex.get("AFM_NAVN")).getStringCellValue());
        aton.setCode(row.getCell(colIndex.get("AFUFORKORTELSE")).getStringCellValue());
        aton.setDescription(row.getCell(colIndex.get("BESKRIVELSE")).getStringCellValue());
        aton.setOwner(row.getCell(colIndex.get("EJER")).getStringCellValue());


        // In this simplified "type" mapping, we map the "karaknr" field to the value 1-3.
        // "karaknr" values:
        // 0: AIS
        // 1: Fyr
        // 2: Bifyr, tågelys, advarselslys, retningsfyr, hindringslys, m.v.
        // 3: Båker, Signalmaster
        // 4: RACONS
        // 5: Lystønder
        // 6: Vagere
        // 7: Stager i bund
        // 8: Radiofyr
        // 9: Tågesignaler

        String karaknr = String.valueOf(Math.round(row.getCell(colIndex.get("KARAKNR")).getNumericCellValue()));
        if (karaknr.contains("5")) {
            aton.setType(2);
        } else if (karaknr.contains("6") || karaknr.contains("7")) {
            aton.setType(3);
        } else {
            aton.setType(1);
        }
    */

}
