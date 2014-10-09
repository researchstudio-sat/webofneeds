/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package won.owner.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import won.owner.model.User;
import won.owner.service.impl.WONUserDetailService;

/**
 * User: LEIH-NB
 * Date: 08.10.2014
 */
public class UserService
{
  /**
   * Gets the current user. If no user is authenticated, an Exception is thrown
   * @return
   */
  @Autowired
  private WONUserDetailService wonUserDetailService;

  public User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    if (username == null) throw new AccessDeniedException("client is not authenticated");
    return (User) wonUserDetailService.loadUserByUsername(username);
  }
}
