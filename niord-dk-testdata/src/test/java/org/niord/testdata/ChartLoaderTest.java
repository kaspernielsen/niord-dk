package org.niord.testdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import org.niord.model.vo.ChartVo;

import java.io.IOException;
import java.util.List;

/**
 * Load charts
 */
public class ChartLoaderTest {

    /**
     * Will throw up unless the "/charts.json" file has a proper format
     */
    @Test
    public void loadCharts() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // Load charts
        List<ChartVo> charts = mapper.readValue(getClass().getResource("/charts.json"), new TypeReference<List<ChartVo>>(){});

        mapper.writeValue(System.out, charts);

    }
}
