/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.pojo;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * User: t.kozel
 * Date: 11/12/13
 */
public class UsernamePojo {

  @NotEmpty private String username;

  public String getUsername() {
    return username;
  }

  public void setUsername(final String username) {
    this.username = username;
  }
}
