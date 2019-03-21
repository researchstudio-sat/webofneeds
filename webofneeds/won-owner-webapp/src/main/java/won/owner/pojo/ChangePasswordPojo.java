package won.owner.pojo;

public class ChangePasswordPojo {
  private String username;
  private String newPassword;
  private String oldPassword;

  public ChangePasswordPojo() {
  }

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String password) {
    this.newPassword = password;
  }

  public String getOldPassword() {
    return oldPassword;
  }

  public void setOldPassword(String oldPassword) {
    this.oldPassword = oldPassword;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }
}
