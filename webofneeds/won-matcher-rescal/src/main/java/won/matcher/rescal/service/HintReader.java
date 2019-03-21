package won.matcher.rescal.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.la4j.matrix.SparseMatrix;
import org.la4j.matrix.functor.MatrixProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.matcher.rescal.config.RescalMatcherConfig;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.HintEvent;
import won.matcher.utils.tensor.TensorMatchingData;

/**
 * Used to read a hint matrix mtx file and create (bulk) hint event objects from it.
 * <p>
 * User: hfriedrich
 * Date: 23.06.2015
 */
@Component
public class HintReader {
    private static final Logger log = LoggerFactory.getLogger(HintReader.class);

    @Autowired
    private RescalMatcherConfig config;

    public BulkHintEvent readHints(TensorMatchingData matchingData) throws
            IOException {

        // read the header file
        ArrayList<String> needHeaders = new ArrayList<>();
        FileInputStream fis = new FileInputStream(config.getExecutionDirectory() + "/" + TensorMatchingData.HEADERS_FILE);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        String line = null;
        int i = 0;
        while ((line = br.readLine()) != null) {
            needHeaders.add(i, line);
            i++;
        }
        br.close();

        // read the hint matrix (supposed to contain new hints only, without the connection entries)
        fis = new FileInputStream(config.getExecutionDirectory() + "/output/hints.mtx");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        StringBuffer stringBuffer = new StringBuffer();

        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith("%%MatrixMarket")) {
                if (!line.contains("row-major") || !line.contains("column-major")) {
                    stringBuffer.append(line).append(" row-major\n");
                } else {
                    stringBuffer.append(line).append("\n");
                }
            } else if (!line.startsWith("%")) {
                stringBuffer.append(line).append("\n");
            }
        }
        String hintMatrixString = stringBuffer.toString();

        SparseMatrix hintMatrix;
        try {
            hintMatrix = SparseMatrix.fromMatrixMarket(hintMatrixString);
        } catch (Exception e) {
            // IllegalArgumentException can occur here if we have no needs and thus now hints, catch this case and return
            // null to indicate no hints were found
            log.warn("Cannot load hint matrix file. This can happen if no hints were created");
            log.debug("Exception was: ", e);
            return null;
        }

        // create the hint events and return them in one bulk hint object
        BulkHintEventMatrixProcedure hintProcedure = new BulkHintEventMatrixProcedure(needHeaders, matchingData);
        hintMatrix.eachNonZero(hintProcedure);
        return hintProcedure.getBulkHintEvent();
    }

    private class BulkHintEventMatrixProcedure implements MatrixProcedure {
        private BulkHintEvent hints;
        private ArrayList<String> needUris;
        private TensorMatchingData matchingData;

        public BulkHintEventMatrixProcedure(ArrayList<String> needUris, TensorMatchingData matchingData) {
            hints = new BulkHintEvent();
            this.needUris = needUris;
            this.matchingData = matchingData;
        }

        @Override
        public void apply(final int i, final int j, final double value) {

            String needUri1 = needUris.get(i);
            String needUri2 = needUris.get(j);

            List<String> matchingDataNeeds = matchingData.getNeeds();

            if (needUri1 != null && needUri2 != null && matchingDataNeeds.contains(needUri1) && matchingDataNeeds.contains(needUri2)) {

                // wonNodeUri must have been set as attribute before to be able to read it here
                String fromWonNodeUri = matchingData.getFirstAttributeOfNeed(needUri1, "wonNodeUri");
                String toWonNodeUri = matchingData.getFirstAttributeOfNeed(needUri2, "wonNodeUri");
                HintEvent hint = new HintEvent(fromWonNodeUri, needUri1, toWonNodeUri,
                        needUri2, config.getPublicMatcherUri(), value);
                hints.addHintEvent(hint);
            } else {
                throw new IllegalStateException("MatchingData does not contain needs with URI " + needUri1 + " or " + needUri2);
            }
        }

        public BulkHintEvent getBulkHintEvent() {
            return hints;
        }
    }
}
