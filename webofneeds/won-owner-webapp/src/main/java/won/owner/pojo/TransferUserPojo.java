package won.owner.pojo;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by fsuda on 25.05.2018.
 */
public class TransferUserPojo extends UserPojo {
  @NotEmpty
  private String privatePassword;

  @NotEmpty
  private String privateUsername;

  public String getPrivateUsername() {
    return privateUsername;
  }

  public void setPrivateUsername(String privateUsername) {
    this.privateUsername = privateUsername;
  }

  public String getPrivatePassword() {
    return privatePassword;
  }

  public void setPrivatePassword(String privatePassword) {
    this.privatePassword = privatePassword;
  }
}
