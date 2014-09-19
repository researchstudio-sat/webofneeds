package won.cryptography.service;

import java.util.Random;

/**
 * User: fsalcher
 * Date: 09.09.2014
 */
public class RandomNumberServiceImpl implements RandomNumberService
{

  // ToDo (FS): check how good this generator is and replace it if necessary
  Random random = new Random();

  @Override
  public String generateRandomString(int length) {

    // ToDo (FS): use not only numbers
    return String.valueOf(random.nextInt((int) Math.round(Math.pow(10, length))));

  }



}
