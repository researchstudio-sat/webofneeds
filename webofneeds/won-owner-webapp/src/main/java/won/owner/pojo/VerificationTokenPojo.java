package won.owner.pojo;

import org.hibernate.validator.constraints.NotEmpty;

public class VerificationTokenPojo {
  @NotEmpty
  private String token;

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
