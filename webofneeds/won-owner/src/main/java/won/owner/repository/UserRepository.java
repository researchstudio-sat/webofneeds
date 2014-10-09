/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.repository;

import won.owner.model.User;
import won.protocol.repository.WonRepository;

/**
 * User: t.kozel
 * Date: 11/7/13
 */
public interface UserRepository extends WonRepository<User> {

	public User findByUsername(String username);

	public User findByUsernameAndPassword(String username, String password);

}
