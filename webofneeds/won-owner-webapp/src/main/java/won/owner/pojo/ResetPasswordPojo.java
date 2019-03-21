package won.owner.pojo;

public class ResetPasswordPojo {
  private String username;
  private String newPassword;
  private String recoveryKey;
  private String verificationToken;

  public ResetPasswordPojo() {
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }

  public String getRecoveryKey() {
    return recoveryKey;
  }

  public void setRecoveryKey(String recoveryKey) {
    this.recoveryKey = recoveryKey;
  }

  public String getVerificationToken() {
    return verificationToken;
  }

  public void setVerificationToken(String verificationToken) {
    this.verificationToken = verificationToken;
  }

}
