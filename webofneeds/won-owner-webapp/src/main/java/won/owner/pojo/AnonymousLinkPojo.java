/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.pojo;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * User: t.kozel Date: 11/12/13
 */
public class AnonymousLinkPojo {

  @NotEmpty
  private String email;

  @NotEmpty
  private String privateId;

  public String getEmail() {
    return email;
  }

  public String getPrivateId() {
    return privateId;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public void setPrivateId(String privateId) {
    this.privateId = privateId;
  }
}
