package rescal;

import common.event.BulkHintEvent;
import common.event.HintEvent;
import org.la4j.matrix.SparseMatrix;
import org.la4j.matrix.functor.MatrixProcedure;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * Used to read a hint matrix mtx file and create (bulk) hint event objects from it.
 *
 * User: hfriedrich
 * Date: 23.06.2015
 */
public class HintReader
{

  public static BulkHintEvent readHints(String folder, RescalMatchingData matchingData) throws IOException {

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
    fis = new FileInputStream(folder + "/hints.mtx");
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
    StringBuffer stringBuffer = new StringBuffer();
    line = null;
    while ((line=bufferedReader.readLine())!=null) {
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
    SparseMatrix hintMatrix = SparseMatrix.fromMatrixMarket(hintMatrixString);

    // create the hint events and return them in one bulk hint object
    BulkHintEventMatrixProcedure hintProcedure = new BulkHintEventMatrixProcedure(needHeaders, matchingData);
    hintMatrix.eachNonZero(hintProcedure);
    return hintProcedure.getBulkHintEvent();
  }

  private static class BulkHintEventMatrixProcedure implements MatrixProcedure
  {
    private BulkHintEvent hints;
    private ArrayList<String> needUris;
    private RescalMatchingData matchingData;

    public BulkHintEventMatrixProcedure(ArrayList<String> needUris, RescalMatchingData matchingData) {
      hints = new BulkHintEvent();
      this.needUris = needUris;
      this.matchingData = matchingData;
    }

    @Override
    public void apply(final int i, final int j, final double value) {

      String needUri1 = needUris.get(i);
      String needUri2 = needUris.get(j);
      if (needUri1 != null && needUri2 != null) {

        String fromWonNodeUri = matchingData.getWonNodeOfNeed(needUri1);
        String toWonNodeUri = matchingData.getWonNodeOfNeed(needUri2);
        HintEvent hint = new HintEvent(fromWonNodeUri, needUri1, toWonNodeUri,
                                       needUri2, "http://rescalmatcher", value);
        hints.addHintEvent(hint);
      }
    }

    public BulkHintEvent getBulkHintEvent() {
      return hints;
    }
  }
}
