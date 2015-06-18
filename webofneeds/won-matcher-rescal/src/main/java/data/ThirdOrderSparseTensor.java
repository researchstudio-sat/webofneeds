package data;

import org.la4j.io.MatrixMarketStream;
import org.la4j.matrix.Matrix;
import org.la4j.matrix.functor.MatrixProcedure;
import org.la4j.matrix.sparse.CCSMatrix;
import org.la4j.vector.Vector;
import org.la4j.vector.sparse.CompressedVector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * Sparse third order tensor based on la4j implementation of sparse matrices.
 *
 * User: hfriedrich
 * Date: 09.07.2014
 */
public class ThirdOrderSparseTensor
{
  private CCSMatrix[] slices;
  private int[] dims;

  public ThirdOrderSparseTensor(int dimX1, int dimX2, int dimX3, int nzMaxPerSlice) {

    dims = null;
    slices = null;
    resize(dimX1, dimX2, dimX3, nzMaxPerSlice);
  }

  public void resize(int dimX1, int dimX2, int dimX3, int nzMaxPerSlice) {

    CCSMatrix[] newSlices = new CCSMatrix[dimX3];
    for (int x3 = 0; x3 < dimX3; x3++) {
      if (dims != null && x3 < dims[2]) {
        int[] newDims = {dimX1, dimX2};

        // !!! Dont use the resize method cause there is a bug in version la4j 0.4.9 !!!
        // => WonMatchingDataTest breaks
        // slices[x3] = (CCSMatrix) slices[x3].resize(dimX1, dimX2);
        // newSlices[x3] = slices[x3];

        // Have to copy the matrix in order to resize
        CCSMatrix copy = new CCSMatrix(dimX1, dimX2);
        slices[x3].eachNonZero(new CopyToMatrix(copy));
        slices[x3] = copy;
        newSlices[x3] = slices[x3];

      } else {
        newSlices[x3] = new CCSMatrix(dimX1, dimX2);
      }
    }
    dims = new int[]{dimX1, dimX2, dimX3};
    slices = newSlices;
  }

  public void setEntry(double value, int x1, int x2, int x3) {
    slices[x3].set(x1, x2, value);
  }

  public double getEntry(int x1, int x2, int x3) {
    return slices[x3].get(x1, x2);
  }

  public void zeroAllRows(int x1) {
    for (int x3 = 0; x3 < dims[2]; x3++) {
      Vector zeroVector = new CompressedVector(dims[0]);
      slices[x3].setRow(x1, zeroVector);
    }
  }

  public void zeroAllColumns(int x2) {
    for (int x3 = 0; x3 < dims[2]; x3++) {
      Vector zeroVector = new CompressedVector(dims[1]);
      slices[x3].setColumn(x2, zeroVector);
    }
  }

  public void copyAllRows(int fromX1, int toX1) {
    for (int x3 = 0; x3 < dims[2]; x3++) {
      slices[x3].setRow(toX1, slices[x3].getRow(fromX1));
    }
  }

  public void copyAllColumns(int fromX2, int toX2) {
    for (int x3 = 0; x3 < dims[2]; x3++) {
      slices[x3].setColumn(toX2, slices[x3].getColumn(fromX2));
    }
  }

  public Collection<Integer> getNonZeroIndicesOfRow(int x1, int x3) {
    NonZeroVectorProcedure nz = new NonZeroVectorProcedure();
    slices[x3].getRow(x1).eachNonZero(nz);
    return nz.getNonZeroIndices();
  }

  public boolean hasNonZeroEntryInRow(int x1, int x3) {
    return (slices[x3].getRow(x1).max() > 0.0d);
  }

  public Collection<Integer> nonZeroRowIndices(int x1, int x3) {
    Vector row = slices[x3].getRow(x1);
    NonZeroVectorProcedure nzProcedure = new NonZeroVectorProcedure();
    row.eachNonZero(nzProcedure);
    return nzProcedure.getNonZeroIndices();
  }

  public int getNonZeroEntries(int dimX3) {
    return slices[dimX3].cardinality();
  }

  public int[] getDimensions() {
    return dims;
  }

  public void writeToFile(String folder, String filePrefix) throws IOException {

    int i = 0;
    for (CCSMatrix slice : slices) {
      MatrixMarketStream mms = new MatrixMarketStream(
        new FileOutputStream(new File(folder + filePrefix + "-" + i + ".mtx")));
      mms.writeMatrix(slice);
      i++;
    }
  }

  public void writeSliceToFile(String fileName, int slice) throws IOException {
    MatrixMarketStream mms = new MatrixMarketStream(
      new FileOutputStream(new File(fileName)));
    mms.writeMatrix(slices[slice]);
  }

  private static class CopyToMatrix implements MatrixProcedure
  {
    private Matrix copy;

    public CopyToMatrix(CCSMatrix copy) {
      this.copy = copy;
    }

    @Override
    public void apply(int i, int j, double value) {
      copy.set(i, j, value);
    }
  }
}
