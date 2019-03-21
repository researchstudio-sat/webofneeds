/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.web.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import won.owner.model.User;
import won.owner.pojo.UserPojo;
import won.owner.service.impl.WONUserDetailService;

/**
 * User: t.kozel Date: 11/12/13
 */
@Component
public class UserRegisterValidator implements Validator {
  private final static Logger log = LoggerFactory.getLogger(UserRegisterValidator.class);

  private final Validator validator;

  private final WONUserDetailService wonUserDetailService;

  @Autowired
  public UserRegisterValidator(final Validator validator, final WONUserDetailService wonUserDetailService) {
    this.validator = validator;
    this.wonUserDetailService = wonUserDetailService;
  }

  @Override
  public boolean supports(final Class<?> clazz) {
    return clazz.equals(UserPojo.class);
  }

  @Override
  public void validate(final Object target, final Errors errors) {
    UserPojo user = (UserPojo) target;

    validator.validate(target, errors);

    if (user.getPassword().length() < 6) {
      errors.rejectValue("password", "passwordTooShort", "Password needs to be at least 6 Characters long");
    }

    if (errors.getFieldError("username") != null) {
      User userInDb = (User) wonUserDetailService.loadUserByUsername(user.getUsername());
      if (userInDb != null) {
        errors.reject("userIsAlreadyInDb", "Username already exists, please choose a different one");
      }
    }
  }
}
