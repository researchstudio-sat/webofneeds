package won.matcher.rescal.service;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

import org.la4j.matrix.SparseMatrix;
import org.la4j.matrix.functor.MatrixProcedure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import won.matcher.rescal.config.RescalMatcherConfig;
import won.matcher.service.common.event.AtomHintEvent;
import won.matcher.service.common.event.BulkHintEvent;
import won.matcher.service.common.event.Cause;
import won.matcher.service.common.event.HintEvent;
import won.matcher.utils.tensor.TensorMatchingData;

/**
 * Used to read a hint matrix mtx file and create (bulk) hint event objects from
 * it.
 * <p>
 * User: hfriedrich Date: 23.06.2015
 */
@Component
public class HintReader {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    @Autowired
    private RescalMatcherConfig config;

    public BulkHintEvent readHints(TensorMatchingData matchingData) throws IOException {
        // read the header file
        ArrayList<String> atomHeaders = new ArrayList<>();
        FileInputStream fis = new FileInputStream(
                        config.getExecutionDirectory() + "/" + TensorMatchingData.HEADERS_FILE);
        BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        String line = null;
        int i = 0;
        while ((line = br.readLine()) != null) {
            atomHeaders.add(i, line);
            i++;
        }
        br.close();
        // read the hint matrix (supposed to contain new hints only, without the
        // connection entries)
        fis = new FileInputStream(config.getExecutionDirectory() + "/output/hints.mtx");
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        StringBuilder stringBuffer = new StringBuilder();
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
            // IllegalArgumentException can occur here if we have no atoms and thus now
            // hints, catch this case and return
            // null to indicate no hints were found
            logger.warn("Cannot load hint matrix file. This can happen if no hints were created");
            logger.debug("Exception was: ", e);
            return null;
        }
        // create the hint events and return them in one bulk hint object
        BulkHintEventMatrixProcedure hintProcedure = new BulkHintEventMatrixProcedure(atomHeaders, matchingData);
        hintMatrix.eachNonZero(hintProcedure);
        return hintProcedure.getBulkHintEvent();
    }

    private class BulkHintEventMatrixProcedure implements MatrixProcedure {
        private BulkHintEvent hints;
        private ArrayList<String> atomUris;
        private TensorMatchingData matchingData;

        public BulkHintEventMatrixProcedure(ArrayList<String> atomUris, TensorMatchingData matchingData) {
            hints = new BulkHintEvent();
            this.atomUris = atomUris;
            this.matchingData = matchingData;
        }

        @Override
        public void apply(final int i, final int j, final double value) {
            String atomUri1 = atomUris.get(i);
            String atomUri2 = atomUris.get(j);
            List<String> matchingDataAtoms = matchingData.getAtoms();
            if (atomUri1 != null && atomUri2 != null && matchingDataAtoms.contains(atomUri1)
                            && matchingDataAtoms.contains(atomUri2)) {
                // wonNodeUri must have been set as attribute before to be able to read it here
                String fromWonNodeUri = matchingData.getFirstAttributeOfAtom(atomUri1, "wonNodeUri");
                String toWonNodeUri = matchingData.getFirstAttributeOfAtom(atomUri2, "wonNodeUri");
                HintEvent hint = new AtomHintEvent(atomUri1, fromWonNodeUri, atomUri2, toWonNodeUri,
                                config.getPublicMatcherUri(), value, Cause.MATCHED_OFFLINE);
                hints.addHintEvent(hint);
            } else {
                throw new IllegalStateException(
                                "MatchingData does not contain atoms with URI " + atomUri1 + " or " + atomUri2);
            }
        }

        public BulkHintEvent getBulkHintEvent() {
            return hints;
        }
    }
}
