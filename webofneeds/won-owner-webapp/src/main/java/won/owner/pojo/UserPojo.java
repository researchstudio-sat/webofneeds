/*
 * This file is subject to the terms and conditions defined in file
 * 'LICENSE.txt', which is part of this source code package.
 */
package won.owner.pojo;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * User: t.kozel Date: 11/12/13
 */
public class UserPojo extends UsernamePojo {
    @NotEmpty
    private String password;
    private String privateId;

    public String getPassword() {
        return password;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getPrivateId() {
        return privateId;
    }

    public void setPrivateId(String privateId) {
        this.privateId = privateId;
    }
}
