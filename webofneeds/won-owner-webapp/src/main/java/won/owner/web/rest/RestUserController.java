/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.web.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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
@RequestMapping("/rest/users")
public class RestUserController {

	private WONUserDetailService wonUserDetailService;

	private AuthenticationManager authenticationManager;

	private SecurityContextRepository securityContextRepository;

	private UserRegisterValidator userRegisterValidator;

	@Autowired
	public RestUserController(final WONUserDetailService wonUserDetailService, final AuthenticationManager authenticationManager,
	                          final SecurityContextRepository securityContextRepository, final UserRegisterValidator userRegisterValidator) {
		this.wonUserDetailService = wonUserDetailService;
		this.authenticationManager = authenticationManager;
		this.securityContextRepository = securityContextRepository;
		this.userRegisterValidator = userRegisterValidator;
	}

  /**
   * registers user
   * @param user registration data of a user
   * @param errors
   * @return ResponseEntity with Http Status Code
   */
	@ResponseBody
	@RequestMapping(
			value = "/",
			method = RequestMethod.POST
	)
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
	public ResponseEntity registerUser(@RequestBody UserPojo user, Errors errors) {
		try {
			userRegisterValidator.validate(user, errors);
			if (errors.hasErrors()) {
				if (errors.getFieldErrorCount() > 0) {
					// someone trying to go around js validation
					return new ResponseEntity(errors.getAllErrors().get(0).getDefaultMessage(), HttpStatus.BAD_REQUEST);
				} else {
					// username is already in database
					return new ResponseEntity("Cannot create user: name is already in use.",HttpStatus.CONFLICT);
				}
			} else {
				PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
				wonUserDetailService.save(new User(user.getUsername(), passwordEncoder.encode(user.getPassword())));
			}
		} catch (DataIntegrityViolationException e) {
			// username is already in database
			return new ResponseEntity("Cannot create user: name is already in use.", HttpStatus.CONFLICT);
		}
		return new ResponseEntity("New user was created", HttpStatus.CREATED);
	}

  /**
   * check authentication and returrn ResponseEntity with HTTP status code
   * @param user user object
   * @param request
   * @param response
   * @return
   */
	@RequestMapping(
			value = "/signin",
			method = RequestMethod.POST
    )
    //TODO: move transactionality annotation into the service layer
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity logIn(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken token =	new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        try {
            Authentication auth = authenticationManager.authenticate(token);
            SecurityContextHolder.getContext().setAuthentication(auth);
            securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
            return new ResponseEntity("Signed in.", HttpStatus.OK);
        } catch (BadCredentialsException ex) {
            return new ResponseEntity("No such username/password combination registered.", HttpStatus.FORBIDDEN);
        }
	}

    /**
    * Method only accessible if the user's still signed in / the session's still valid -> Use it to check the session cookie.
    */
    //* @param user user object
    //* @param request
    //* @param response
    //* @return
    //
	@RequestMapping(
			value = "/isSignedIn",
			method = RequestMethod.POST
    )
    //TODO: move transactionality annotation into the service layer
    @Transactional(propagation = Propagation.SUPPORTS)
    //public ResponseEntity isSignedIn(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
    public ResponseEntity isSignedIn() {
        // Execution will only get here, if the session is still valid, so sending OK here is enough. Spring sends an error
        // code by itself if the session isn't valid any more
        return new ResponseEntity("Current session is still valid.", HttpStatus.OK);
    }

    /**
     *
     * @return
     */
    @RequestMapping(
            value = "/signout",
            method = RequestMethod.POST
    )
    //TODO: move transactionality annotation into the service layer
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity logOut (HttpServletRequest request, HttpServletResponse response){
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() == null) {
            return new ResponseEntity("No user is signed in, ignoring this request.", HttpStatus.NOT_MODIFIED);
        }
        myLogoff(request, response);
        return new ResponseEntity("Signed out", HttpStatus.OK);
    }


  @RequestMapping(
    value = "/{userId}/favourites",
    method = RequestMethod.POST
  )
  @Transactional(propagation = Propagation.SUPPORTS)
  public ResponseEntity saveAsFavourite(){
    return null;
  }
  @RequestMapping(
    value="/{userId}/resetPassword",
    method = RequestMethod.POST
  )
  @Transactional(propagation = Propagation.SUPPORTS)
  public ResponseEntity resetPassword(@RequestBody String password){
     return null;
  }

  private static void myLogoff(HttpServletRequest request, HttpServletResponse response) {
    CookieClearingLogoutHandler cookieClearingLogoutHandler = new CookieClearingLogoutHandler(
      AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY);
    SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
    cookieClearingLogoutHandler.logout(request, response, null);
    securityContextLogoutHandler.logout(request, response, null);
  }

}
