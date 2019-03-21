/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import won.owner.model.User;
import won.owner.repository.UserRepository;

/**
 * User: t.kozel
 * Date: 11/7/13
 */
public class WONUserDetailService implements UserDetailsService {

	UserRepository userRepository;

  public WONUserDetailService() {
  }

  @Autowired
  public void setUserRepository(final UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
	public UserDetails loadUserByUsername(final String username) throws UsernameNotFoundException {
		User user = userRepository.findByUsername(username);
		if(user == null) {
			throw new UsernameNotFoundException("User " + username + " not found!");
		} else {
			return user;
		}
	}

	public User save(User user) {
		return userRepository.save(user);
	}

}
