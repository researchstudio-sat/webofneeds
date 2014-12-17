package won.cryptography.service;

/**
 * User: fsalcher
 * Date: 18.09.2014
 */
public interface RandomNumberService
{

  /**
   * generates a URI safe random string with the given length
   * @param length
   */
  public String generateRandomString(int length);

}
