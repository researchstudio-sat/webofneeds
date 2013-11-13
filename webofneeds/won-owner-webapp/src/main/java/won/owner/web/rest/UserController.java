/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.web.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import won.owner.model.User;
import won.owner.pojo.UserPojo;
import won.owner.service.impl.WONUserDetailService;
import won.owner.web.validator.UserRegisterValidator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * User: t.kozel
 * Date: 11/12/13
 */
@Controller
@RequestMapping("/rest/user")
public class UserController {

	private WONUserDetailService wonUserDetailService;

	private AuthenticationManager authenticationManager;

	private SecurityContextRepository securityContextRepository;

	private UserRegisterValidator userRegisterValidator;

	@Autowired
	public UserController(final WONUserDetailService wonUserDetailService, final AuthenticationManager authenticationManager,
	                      final SecurityContextRepository securityContextRepository, final UserRegisterValidator userRegisterValidator) {
		this.wonUserDetailService = wonUserDetailService;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.userRegisterValidator = userRegisterValidator;
	}

	@ResponseBody
	@RequestMapping(
			value = "/",
			method = RequestMethod.POST
	)
	public ResponseEntity registerUser(@RequestBody UserPojo user, Errors errors) {
		try {
			userRegisterValidator.validate(user, errors);
			if (errors.hasErrors()) {
				if (errors.getFieldErrorCount() > 0) {
					// someone trying to go around js validation
					return new ResponseEntity(HttpStatus.BAD_REQUEST);
				} else {
					// username is already in database
					return new ResponseEntity(HttpStatus.CONFLICT);
				}
			} else {
				PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
				wonUserDetailService.save(new User(user.getUsername(), passwordEncoder.encode(user.getPassword())));
			}
		} catch (DataIntegrityViolationException e) {
			// username is already in database
			return new ResponseEntity(HttpStatus.CONFLICT);
		}
		return new ResponseEntity(HttpStatus.OK);
	}

	@RequestMapping(
			value = "/login",
			method = RequestMethod.POST
	)
	public ResponseEntity logIn(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
		UsernamePasswordAuthenticationToken token =	new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
		try {
			Authentication auth = authenticationManager.authenticate(token);
			SecurityContextHolder.getContext().setAuthentication(auth);
			securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
			return new ResponseEntity(HttpStatus.OK);
		} catch (BadCredentialsException ex) {
			return new ResponseEntity(HttpStatus.FORBIDDEN);
		}
	}

	@RequestMapping(
			value = "/logout",
			method = RequestMethod.POST
	)
	public ResponseEntity logOut() {
		SecurityContext context = SecurityContextHolder.getContext();
		if(context != null) {
			context.setAuthentication(null);
		}
		return new ResponseEntity(HttpStatus.OK);
	}

}
