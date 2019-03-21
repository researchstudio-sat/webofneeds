package won.cryptography.service;

import won.protocol.util.ExpensiveSecureRandomString;

/**
 * User: fsalcher Date: 09.09.2014
 */
public class SecureRandomNumberServiceImpl implements RandomNumberService {

  ExpensiveSecureRandomString randomString = new ExpensiveSecureRandomString();

  @Override
  public String generateRandomString(int length) {
    return randomString.nextString(length);
  }

}
