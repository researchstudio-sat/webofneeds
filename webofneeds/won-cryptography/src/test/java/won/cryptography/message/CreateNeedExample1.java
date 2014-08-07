package won.cryptography.message;

/**
 * User: ypanchenko
 * Date: 05.08.2014
 *
 */

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Scenario: the owner-app-server gets the Message from
 * the owner-app-browser. If it's a create need message,
 * it sends it to won-node-server. Won-node checks if
 * the need uri is already in use, it happen to be in use,
 * so the won-node allocates a new url, and sends it back
 * to owner-app-server, which sends it to owner-app-browser.
 * Owner-app-browser recreates need with the allocated uri,
 * and sends create message again. This time the won-node
 * creates (publishes) the need and sends back success
 * message to the owner-app-server -> owner-app browser.
 */
public class CreateNeedExample1
{
  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();

  @Test
  public void runCreateNeedExample1() throws Exception {

    // TODO
  }
}
