package data;

import common.event.BulkHintEvent;
import common.event.HintEvent;
import org.la4j.io.MatrixMarketStream;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.functor.MatrixProcedure;

import java.io.*;
import java.util.ArrayList;

/**
 * User: hfriedrich
 * Date: 23.06.2015
 */
public class HintReader
{

  public static BulkHintEvent readHints(String folder) throws IOException {

    // read the header file
    ArrayList<String> needHeaders = new ArrayList<>();
    FileInputStream fis = new FileInputStream(folder + "/"+ RescalMatchingData.HEADERS_FILE);
    BufferedReader br = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
    String line = null;
    int i = 0;
    while ((line = br.readLine()) != null) {
      if (line.startsWith(RescalMatchingData.NEED_PREFIX)) {
        String originalHeaderEntry = line.substring(RescalMatchingData.NEED_PREFIX.length());
        needHeaders.add(i, originalHeaderEntry);
      }
      i++;
    }
    br.close();

    // read the hint matrix (supposed to contain new hints only, without the connection entries)
    MatrixMarketStream mms = new MatrixMarketStream(
      new FileInputStream(new File(folder + "/hints.mtx")));
    Matrix hintMatrix = mms.readMatrix();

    // create the hint events and return them in one bulk hint object
    BulkHintEventMatrixProcedure hintProcedure = new BulkHintEventMatrixProcedure(needHeaders);
    hintMatrix.eachNonZero(hintProcedure);
    return hintProcedure.getBulkHintEvent();
  }

  private static class BulkHintEventMatrixProcedure implements MatrixProcedure
  {
    private BulkHintEvent hints;
    private ArrayList<String> needUris;

    public BulkHintEventMatrixProcedure(ArrayList<String> needUris) {
      hints = new BulkHintEvent();
      this.needUris = needUris;
    }

    @Override
    public void apply(final int i, final int j, final double value) {

      String needUri1 = needUris.get(i);
      String needUri2 = needUris.get(j);
      if (needUri1 != null && needUri2 != null) {
        HintEvent hint = new HintEvent(needUri1, needUri2, value);
        hints.addHintEvent(hint);
      }
    }

    public BulkHintEvent getBulkHintEvent() {
      return hints;
    }
  }
}
