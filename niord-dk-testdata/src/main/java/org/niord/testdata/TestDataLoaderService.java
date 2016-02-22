package org.niord.testdata;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.niord.core.model.Chart;
import org.niord.core.service.BaseService;
import org.niord.core.service.ChartService;
import org.niord.model.vo.ChartVo;
import org.niord.model.vo.geojson.PolygonVo;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.util.List;

/**
 * Loads test base data into an empty system
 */
@Singleton
@Startup
public class TestDataLoaderService extends BaseService {

    @Inject
    Logger log;

    @Inject
    ChartService chartService;

    ObjectMapper mapper = new ObjectMapper();

    /**
     * Called when the system starts up. Loads base data
     */
    @PostConstruct
    public void init() {

        // Check if we need to load charts
        if (count(Chart.class) == 0) {
            loadCharts();
        }
    }

    /**
     * Loads the charts from the "/charts.json" file into the system
     */
    private void loadCharts() {
        try {

            // Load charts
            List<ExtentChartVo> charts = mapper.readValue(getClass().getResource("/charts.json"), new TypeReference<List<ExtentChartVo>>(){});
            for (ExtentChartVo c : charts) {

                // Convert the extent into a geometry
                c.computeGeometry();

                // Save the chart
                saveEntity(new Chart(c));
            }
            log.info("**** loaded " + charts.size() + " charts");

        } catch (Exception e) {
            log.error("Failed loading charts", e);
        }
    }


    /** Matches the format of the charts.json file **/
    public static class ExtentChartVo extends ChartVo {
        Double lowerLeftLatitude, lowerLeftLongitude;
        Double upperRightLatitude, upperRightLongitude;

        /** Converts the extent into a geometry **/
        public void computeGeometry() {
            if (lowerLeftLatitude != null && lowerLeftLongitude != null &&
                    upperRightLatitude != null && upperRightLongitude != null) {
                double[][] coordinates = {
                        { lowerLeftLatitude, lowerLeftLongitude },
                        { upperRightLatitude, lowerLeftLongitude },
                        { upperRightLatitude, upperRightLongitude },
                        { lowerLeftLatitude, upperRightLongitude },
                        { lowerLeftLatitude, lowerLeftLongitude }
                };
                setGeometry(new PolygonVo(new double[][][] { coordinates }));
            }
        }

        public Double getLowerLeftLatitude() {
            return lowerLeftLatitude;
        }

        public void setLowerLeftLatitude(Double lowerLeftLatitude) {
            this.lowerLeftLatitude = lowerLeftLatitude;
        }

        public Double getLowerLeftLongitude() {
            return lowerLeftLongitude;
        }

        public void setLowerLeftLongitude(Double lowerLeftLongitude) {
            this.lowerLeftLongitude = lowerLeftLongitude;
        }

        public Double getUpperRightLatitude() {
            return upperRightLatitude;
        }

        public void setUpperRightLatitude(Double upperRightLatitude) {
            this.upperRightLatitude = upperRightLatitude;
        }

        public Double getUpperRightLongitude() {
            return upperRightLongitude;
        }

        public void setUpperRightLongitude(Double upperRightLongitude) {
            this.upperRightLongitude = upperRightLongitude;
        }

    }


}
