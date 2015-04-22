package won.cryptography.rdfsign;

/**
 * User: ypanchenko
 * Date: 30.03.2015
 */
public class WonEccPublicKey
{

  private String curveId;
  private String algorithm;
  //private BigInteger qx;
  //private BigInteger qy;
  private String qx;
  private String qy;

  public WonEccPublicKey(final String curveId, final String algorithm, final String qx, final String qy) {
    this.curveId = curveId;
    this.algorithm = algorithm;
//    this.qx = new BigInteger(qx, 16);
//    this.qy = new BigInteger(qy, 16);
    this.qx = qx;
    this.qy = qy;
  }

  public String getCurveId() {
    return curveId;
  }

  public String getAlgorithm() {
    return algorithm;
  }

//  public BigInteger getQx() {
//    return qx;
//  }
//
//  public BigInteger getQy() {
//    return qy;
//  }


  public String getQx() {
    return qx;
  }

  public String getQy() {
    return qy;
  }
}
