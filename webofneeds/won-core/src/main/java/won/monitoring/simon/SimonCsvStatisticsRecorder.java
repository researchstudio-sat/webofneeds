/*
 * Copyright 2012 Research Studios Austria Forschungsges.m.b.H. Licensed under
 * the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License
 * at http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable
 * law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package won.monitoring.simon;

import org.javasimon.Sample;
import org.javasimon.Simon;
import org.javasimon.SimonManager;
import org.javasimon.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvMapWriter;
import org.supercsv.io.ICsvMapWriter;
import org.supercsv.prefs.CsvPreference;
import won.monitoring.AbstractFileOutputRecorder;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

/**
 * Recorder that writes the Simon stats to a csv file.
 */
public class SimonCsvStatisticsRecorder extends AbstractFileOutputRecorder {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Sets up the processors used.
     * 
     * @return the cell processors
     */
    private static CellProcessor[] getProcessors() {
        return new CellProcessor[] { new NotNull(), // name
                        new NotNull(), // type
                        new Optional(), // counter
                        new Optional(), // total
                        new Optional(), // min
                        new Optional(), // max
                        new Optional(), // mean
                        new Optional(), // std dev
                        new NotNull(), // first usage
                        new NotNull(), // last usage
                        new NotNull(), // last reset
                        new Optional() // note
        };
    }

    private static String[] header = new String[] { "Name", "Type", "Counter", "Total", "Min", "Max", "Mean", "StdDev",
                    "FirstUsage", "LastUsage", "LastReset", "Note" };

    @Override
    public void recordMonitoringStatistics() {
        ICsvMapWriter mapWriter = null;
        try {
            mapWriter = new CsvMapWriter(new FileWriter(createOutFileObject()), CsvPreference.STANDARD_PREFERENCE);
            final CellProcessor[] processors = getProcessors();
            // write the header
            mapWriter.writeHeader(header);
            // create a simon visitor that writes each line
            SimonVisitor visitor = new CsvSimonVisitor(mapWriter);
            // write the customer maps
            SimonVisitors.visitTree(SimonManager.getRootSimon(), visitor);
        } catch (IOException e) {
            logger.warn("could not write simon statistics", e);
        } finally {
            if (mapWriter != null) {
                try {
                    mapWriter.close();
                } catch (IOException e) {
                    logger.warn("could not close writer", e);
                }
            }
        }
    }

    private class CsvSimonVisitor implements SimonVisitor {
        private ICsvMapWriter mapWriter;

        public CsvSimonVisitor(final ICsvMapWriter mapWriter) {
            this.mapWriter = mapWriter;
        }

        @Override
        public void visit(final Simon simon) throws IOException {
            Map<String, Object> values = new HashMap<>(header.length);
            Sample sample = simon.sample();
            values.put(header[0], sample.getName());
            values.put(header[1], simon.getClass().getName());
            if (simon instanceof Stopwatch) {
                Stopwatch stopwatch = (Stopwatch) simon;
                values.put(header[2], stopwatch.getCounter());
                values.put(header[3], stopwatch.getTotal());
                values.put(header[4], stopwatch.getMin());
                values.put(header[5], stopwatch.getMax());
                values.put(header[6], stopwatch.getMean());
                values.put(header[7], stopwatch.getStandardDeviation());
            }
            values.put(header[8], simon.getFirstUsage());
            values.put(header[9], simon.getLastUsage());
            values.put(header[10], simon.getLastReset());
            values.put(header[11], simon.getNote());
            this.mapWriter.write(values, header, getProcessors());
        }
    }
}
