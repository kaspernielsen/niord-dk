package org.niord.web.nm;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.junit.Test;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Test extracting text from EfS doc
 */
public class NmWordExtractorTest {

    @Test
    public void extractNmsFromWord() {

        try (InputStream in = NmWordExtractorTest.class.getResourceAsStream("/EfS template.doc")) {
            /*
            WordExtractor wordExtractor = new WordExtractor(in);
            for(String paragraph:wordExtractor.getParagraphText()){
                System.out.print(paragraph);
            }
            */

            NPOIFSFileSystem fs = new NPOIFSFileSystem(in);
            HWPFDocument doc = new HWPFDocument( fs.getRoot() );

            //StyleSheet styleSheet = doc.getStyleSheet();
            //ListTables listTables = doc.getListTables();

            Range r = doc.getRange();
            r.insertAfter("Peder was here");

            OutputStream out = new FileOutputStream("/Users/carolus/Desktop/Test/efs-test.doc");
            doc.write(out);
            out.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
