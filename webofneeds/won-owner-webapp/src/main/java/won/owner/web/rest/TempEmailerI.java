package won.owner.web.rest;

/**
 * User: ypanchenko
 * Date: 17.02.2015
 */
public interface  TempEmailerI {

  void sendPrivateLink(String toEmail, String privateLink);

}
