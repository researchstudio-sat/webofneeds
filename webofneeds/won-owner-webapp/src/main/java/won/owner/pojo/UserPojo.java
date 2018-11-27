/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.pojo;

import org.hibernate.validator.constraints.NotEmpty;

/**
 * User: t.kozel
 * Date: 11/12/13
 */
public class UserPojo {

	@NotEmpty
	private String username;

	@NotEmpty
	private String password;

    private boolean privateIdUser;

	public String getUsername() {
		return username;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

    public boolean isPrivateIdUser() {
        return privateIdUser;
    }

    public void setPrivateIdUser(boolean privateIdUser) {
        this.privateIdUser = privateIdUser;
    }
}
