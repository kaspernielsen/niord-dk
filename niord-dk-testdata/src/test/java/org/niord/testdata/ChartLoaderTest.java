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

package org.niord.testdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import org.niord.model.message.ChartVo;

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
