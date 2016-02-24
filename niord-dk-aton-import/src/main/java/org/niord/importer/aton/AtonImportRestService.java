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
package org.niord.importer.aton;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.jboss.security.annotation.SecurityDomain;
import org.niord.core.model.AtonNode;
import org.niord.core.model.User;
import org.niord.core.repo.RepositoryService;
import org.niord.core.sequence.DefaultSequence;
import org.niord.core.sequence.Sequence;
import org.niord.core.sequence.SequenceService;
import org.niord.core.service.AtonService;
import org.niord.core.service.UserService;
import org.niord.model.vo.aton.AtonNodeVo;
import org.niord.model.vo.aton.AtonOsmVo;
import org.slf4j.Logger;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.InputStream;
import java.util.*;

/**
 * Imports AtoN from Excel sheets.
 */
@Path("/import/atons")
@Stateless
@SecurityDomain("keycloak")
@PermitAll
@SuppressWarnings("unused")
public class AtonImportRestService {

    private final static Sequence AFM_SEQUENCE = new DefaultSequence("AFM_ATON_VERSION", 1);

    @Inject
    Logger log;

    @Context
    ServletContext servletContext;

    @Inject
    AtonService atonService;

    @Inject
    UserService userService;

    @Inject
    SequenceService sequenceService;

    /**
     * Imports an uploaded AtoN Excel file
     *
     * @param request the servlet request
     * @return a status
     */
    @POST
    @Path("/upload-xls")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("text/plain")
    @RolesAllowed("admin")
    public String importXls(@Context HttpServletRequest request) throws Exception {

        FileItemFactory factory = RepositoryService.newDiskFileItemFactory(servletContext);
        ServletFileUpload upload = new ServletFileUpload(factory);
        List<FileItem> items = upload.parseRequest(request);

        StringBuilder txt = new StringBuilder();

        for (FileItem item : items) {
            if (!item.isFormField()) {
                String name = item.getName().toLowerCase();
                System.out.println("XXXX " + name);

                // AtoN Import
                if (name.startsWith("afmmyndighed_table") && name.endsWith(".xls")) {
                    importAtoN(item.getInputStream(), item.getName(), txt);

                } else if (name.startsWith("fyr") && name.endsWith(".xls")) {
                    importLights(item.getInputStream(), item.getName(), txt);

                } else if (name.startsWith("ais") && name.endsWith(".xls")) {
                    importAis(item.getInputStream(), item.getName(), txt);

                } else if (name.startsWith("dgps") && name.endsWith(".xls")) {
                    importDgps(item.getInputStream(), item.getName(), txt);

                } else if (name.startsWith("racon") && name.endsWith(".xls")) {
                    importRacons(item.getInputStream(), item.getName(), txt);
                }
            }
        }

        return txt.toString();
    }

    /**
     * Extracts the AtoNs from the Excel sheet
     * @param inputStream the Excel sheet input stream
     * @param fileName the name of the PDF file
     * @param txt a log of the import
     */
    private void importAtoN(InputStream inputStream, String fileName, StringBuilder txt) throws Exception {
        log.info("Extracting AtoNs from Excel sheet " + fileName);

        List<AtonNode> atons = new ArrayList<>();

        // Get the column indexes of the relevant columns
        Map<String, Integer> colIndex = new HashMap<>();
        Iterator<Row> rowIterator = parseHeaderRow(inputStream, colIndex, AfmAtonImportHelper.FIELDS);
        User user = userService.currentUser();
        int changeset = (int)sequenceService.getNextValue(AFM_SEQUENCE);

        // Extract the AtoNs
        int row = 0, errors = 0;
        while (rowIterator.hasNext()) {
            AfmAtonImportHelper importHelper = new AfmAtonImportHelper(user, changeset, colIndex, rowIterator.next());

            try {
                AtonNode aton = importHelper.afm2osm();
                atons.add(aton);
            } catch (Exception e) {
                txt.append(String.format("Error parsing AtoN row %d: %s%n", row, e.getMessage()));
                log.warn("Error parsing AtoN row " + row + ": " + e, e);
                errors++;
            }
            row++;
        }

        // Update the AtoN database
        //atonService.replaceAtons(atons);

        log.info("Extracted " + atons.size() + " AtoNs from " + fileName);
        txt.append(String.format("Parsed %d AtoN rows in file %s. Imported %d. Errors: %d%n", row, fileName, atons.size(), errors));

        printResult(atons);
    }


    /**
     * Extracts the lights from the Excel sheet
     * @param inputStream the Excel sheet input stream
     * @param fileName the name of the PDF file
     * @param txt a log of the import
     */
    private void importLights(InputStream inputStream, String fileName, StringBuilder txt) throws Exception {
        log.info("Extracting lights from Excel sheet " + fileName);

        List<AtonNode> atons = new ArrayList<>();

        // Get the column indexes of the relevant columns
        Map<String, Integer> colIndex = new HashMap<>();
        Iterator<Row> rowIterator = parseHeaderRow(inputStream, colIndex, AfmAtonImportHelper.FIELDS);
        User user = userService.currentUser();
        int changeset = (int)sequenceService.getNextValue(AFM_SEQUENCE);

        // Extract the AtoNs
        int row = 0, errors = 0;
        while (rowIterator.hasNext()) {
            AfmAisImportHelper importHelper = new AfmAisImportHelper(user, changeset, colIndex, rowIterator.next());

            try {
                AtonNode aton = importHelper.afm2osm();

                // TODO: Lookup and merge with AtoN

                atons.add(aton);
            } catch (Exception e) {
                txt.append(String.format("Error parsing light row %d: %s%n", row, e.getMessage()));
                log.warn("Error parsing light row " + row + ": " + e);
                errors++;
            }
            row++;
        }

        // Update the AtoN database
        //atonService.replaceAtons(atons);

        log.info("Extracted " + atons.size() + " AtoNs from " + fileName);
        txt.append(String.format("Parsed %d AtoN rows in file %s. Imported %d. Errors: %d%n", row, fileName, atons.size(), errors));

        printResult(atons);
    }

    /**
     * Extracts the AIS from the Excel sheet
     * @param inputStream the Excel sheet input stream
     * @param fileName the name of the PDF file
     * @param txt a log of the import
     */
    private void importAis(InputStream inputStream, String fileName, StringBuilder txt) throws Exception {
    }


    /**
     * Extracts the DGPS transmitters from the Excel sheet
     * @param inputStream the Excel sheet input stream
     * @param fileName the name of the PDF file
     * @param txt a log of the import
     */
    private void importDgps(InputStream inputStream, String fileName, StringBuilder txt) throws Exception {
    }


    /**
     * Extracts the RACONs from the Excel sheet
     * @param inputStream the Excel sheet input stream
     * @param fileName the name of the PDF file
     * @param txt a log of the import
     */
    private void importRacons(InputStream inputStream, String fileName, StringBuilder txt) throws Exception {
    }


    /**
     * Opens the Excel sheet, reads in the header row and build a map of the column indexes for the given header fields.
     * @param inputStream the Excel sheet
     * @param colIndex the column index map
     * @param fields the fields to determine column indexes for
     * @return the Excel row iterator pointing to the first data row
     */
    private Iterator<Row> parseHeaderRow(InputStream inputStream, Map<String, Integer> colIndex, String[] fields) throws Exception {
        // Create Workbook instance holding reference to .xls file
        HSSFWorkbook workbook = new HSSFWorkbook(inputStream);
        // Get first/desired sheet from the workbook
        HSSFSheet sheet = workbook.getSheetAt(0);

        // Get row iterator
        Iterator<Row> rowIterator = sheet.iterator();
        Row headerRow = rowIterator.next();

        // Get the column indexes of the relevant columns
        Arrays.stream(fields).forEach(f -> updateColumnIndex(headerRow, colIndex, f));

        return rowIterator;
    }

    /** Determines the column index of the given column name */
    private boolean updateColumnIndex(Row headerRow, Map<String, Integer> colIndex, String colName) {
        int index = 0;
        for (Cell cell : headerRow) {
            if (cell.getCellType() == Cell.CELL_TYPE_STRING &&
                    colName.equalsIgnoreCase(cell.getStringCellValue())) {
                colIndex.put(colName, index);
                return true;
            }
            index++;
        }
        return false;
    }


    /** Prints the result to the command line */
    private void printResult(List<AtonNode> atons) {

        AtonOsmVo osm = new AtonOsmVo();
        osm.setVersion(1.0f);
        osm.setNodes(atons.stream()
            .map(AtonNode::toVo)
            .toArray(AtonNodeVo[]::new));

        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(AtonOsmVo.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(osm, System.out);
        } catch (JAXBException e) {
            e.printStackTrace();
        }

    }

}
