
/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.*;
import won.owner.model.User;
import won.owner.model.UserNeed;
import won.owner.pojo.UserPojo;
import won.owner.pojo.UserSettingsPojo;
import won.owner.repository.UserNeedRepository;
import won.owner.repository.UserRepository;
import won.owner.service.impl.WONUserDetailService;
import won.owner.web.WonOwnerMailSender;
import won.owner.web.validator.UserRegisterValidator;
import won.protocol.util.CheapInsecureRandomString;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * User: t.kozel
 * Date: 11/12/13
 */
@Controller
@RequestMapping("/rest/users")
public class RestUserController
{

  private final Logger logger = LoggerFactory.getLogger(getClass());

  private WONUserDetailService wonUserDetailService;

  private AuthenticationManager authenticationManager;

  private SecurityContextRepository securityContextRepository;

  private UserRegisterValidator userRegisterValidator;

  private WonOwnerMailSender emailSender;

  private UserNeedRepository userNeedRepository;

  private UserRepository userRepository;

  @Autowired
  ServletContext context;

  @Autowired
  public RestUserController(final WONUserDetailService wonUserDetailService, final AuthenticationManager authenticationManager,
                            final SecurityContextRepository securityContextRepository,
                            final UserRegisterValidator userRegisterValidator,
                            final WonOwnerMailSender emailSender,
                            final UserRepository userRepository,
                            final UserNeedRepository userNeedRepository) {
    this.wonUserDetailService = wonUserDetailService;
    this.authenticationManager = authenticationManager;
    this.securityContextRepository = securityContextRepository;
    this.userRegisterValidator = userRegisterValidator;
    this.emailSender = emailSender;
    this.userRepository = userRepository;
    this.userNeedRepository = userNeedRepository;
  }

  /**
   * registers user
   *
   * @param user   registration data of a user
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
          return new ResponseEntity("Cannot create user: name is already in use.", HttpStatus.CONFLICT);
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


  @ResponseBody
  @RequestMapping(
    value = "/email",
    method = RequestMethod.POST
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public ResponseEntity sendEmail(@RequestBody JsonNode input) {

    String type = input.get("type").asText();
    String to = input.get("to").asText();
    User user = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

    if ("PRIVATE_LINK".equals(type)) {
      if ("ROLE_PRIVATE".equals(user.getRole())) {
        try{
          emailSender.sendPrivateLink(to, user.getUsername());
        }
        catch (Exception ex) { // org.springframework.mail.MailException
          logger.error("Email could not be sent", ex);
          return new ResponseEntity("Email could not be sent", HttpStatus.INTERNAL_SERVER_ERROR);
        }
      } else {
        return new ResponseEntity("Cannot send private link to not private user", HttpStatus.BAD_REQUEST);
      }
    } else {
      return new ResponseEntity("Mail type " + type + " not supported", HttpStatus.BAD_REQUEST);
    }

    return new ResponseEntity("Email sent", HttpStatus.OK);
  }

  @ResponseBody
  @RequestMapping(
    value = "/settings",
    produces = MediaType.APPLICATION_JSON_VALUE,
    method = RequestMethod.GET
  )

  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public UserSettingsPojo getUserSettings(@RequestParam("uri") String uri) {

    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    // cannot use user object from context since hw doesn't know about created in this session need,
    // therefore, we have to retrieve the user object from the user repository
    User user = userRepository.findByUsername(username);
    UserSettingsPojo userSettingsPojo = new UserSettingsPojo(user.getUsername(), user.getEmail());
    URI needUri = null;
    try {
      needUri = new URI(uri);
      userSettingsPojo.setNeedUri(uri);
      for (UserNeed userNeed : user.getUserNeeds()) {
        if (userNeed.getUri().equals(needUri)) {
          userSettingsPojo.setNotify(userNeed.isMatches(), userNeed.isRequests(), userNeed.isConversations());
          //userSettingsPojo.setEmail(user.getEmail());
          break;
        }
      }
    } catch (URISyntaxException e) {
      // TODO error response
      logger.warn(uri + " need uri problem", e);
    }
    return userSettingsPojo;
  }

  @ResponseBody
  @RequestMapping(
    value = "/settings",
    produces = MediaType.APPLICATION_JSON_VALUE,
    method = RequestMethod.POST
  )

  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public ResponseEntity setUserSettings(@RequestBody UserSettingsPojo userSettingsPojo) {

    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    // cannot use user object from context since hw doesn't know about created in this session need,
    // therefore, we have to retrieve the user object from the user repository
    User user = userRepository.findByUsername(username);
    if (!user.getUsername().equals(userSettingsPojo.getUsername())) {
      logger.warn("user name wrong");
      return new ResponseEntity("user name problem", HttpStatus.BAD_REQUEST);
    }

    if (user.getEmail() == null) {
      //TODO validate email server-side?
      // set email:
      user.setEmail(userSettingsPojo.getEmail());
      userRepository.save(user);
    } else if (!user.getEmail().equals(userSettingsPojo.getEmail())) {
      //TODO validate email server-side?
      // change email:
      user.setEmail(userSettingsPojo.getEmail());
      userRepository.save(user);
      logger.info("change email requested - email changed");
    }

    // retrieve UserNeed
    URI needUri = null;
    try {
      needUri = new URI(userSettingsPojo.getNeedUri());
      for (UserNeed userNeed : user.getUserNeeds()) {
        if (userNeed.getUri().equals(needUri)) {
          userNeed.setMatches(userSettingsPojo.isNotifyMatches());
          userNeed.setRequests(userSettingsPojo.isNotifyRequests());
          userNeed.setConversations(userSettingsPojo.isNotifyConversations());
          userNeedRepository.save(userNeed);
          break;
        }
      }
    } catch (URISyntaxException e) {
      logger.warn(userSettingsPojo.getNeedUri() + " need uri problem.", e);
      return new ResponseEntity(userSettingsPojo.getNeedUri() + " need uri problem.", HttpStatus.BAD_REQUEST);
    }
   return new ResponseEntity("Settings created", HttpStatus.CREATED);
  }

  /**
   * registers user
   *
   * @param user   registration data of a user
   * @param errors
   * @return ResponseEntity with Http Status Code
   */
  @ResponseBody
  @RequestMapping(
    value = "/private",
    method = RequestMethod.POST
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public ResponseEntity registerPrivateLinkAsUser(@RequestBody UserPojo user, Errors errors) {
    String privateLink = null;
    try {
      privateLink = (new CheapInsecureRandomString()).nextString(32); // TODO more secure random alphanum string
      user.setUsername(privateLink);
      userRegisterValidator.validate(user, errors);
      if (errors.hasErrors()) {
        if (errors.getFieldErrorCount() > 0) {
          // someone trying to go around js validation
          return new ResponseEntity(errors.getAllErrors().get(0).getDefaultMessage(), HttpStatus.BAD_REQUEST);
        } else {
          // username is already in database
          return new ResponseEntity("Cannot create user: name is already in use.", HttpStatus.CONFLICT);
        }
      } else {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        User userDetails = new User(user.getUsername(), passwordEncoder.encode(user.getPassword()), "ROLE_PRIVATE");
        wonUserDetailService.save(userDetails);
      }
    } catch (DataIntegrityViolationException e) {
      // username is already in database
      return new ResponseEntity("Cannot create user: name is already in use.", HttpStatus.CONFLICT);
    }
    return new ResponseEntity(privateLink, HttpStatus.CREATED);
  }

  /**
   * check authentication and returrn ResponseEntity with HTTP status code
   *
   * @param user     user object
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
    UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(user.getUsername(),
                                                                                        user.getPassword());
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
    method = RequestMethod.GET
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  //public ResponseEntity isSignedIn(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
  public ResponseEntity isSignedIn() {
    // Execution will only get here, if the session is still valid, so sending OK here is enough. Spring sends an error
    // code by itself if the session isn't valid any more
    SecurityContext context = SecurityContextHolder.getContext();
    //if(context.getAuthentication() )
    if (context == null || context.getAuthentication() == null) {
      return new ResponseEntity("User not signed in.", HttpStatus.UNAUTHORIZED);
    } else if ("anonymousUser".equals(context.getAuthentication().getPrincipal())) {
      return new ResponseEntity("User not signed in.", HttpStatus.UNAUTHORIZED);
    } else {
      return new ResponseEntity("Current session is still valid. asdf", HttpStatus.OK);
    }
  }

  @RequestMapping(
    value = "/isSignedInRole",
    method = RequestMethod.GET
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  //public ResponseEntity isSignedIn(@RequestBody User user, HttpServletRequest request, HttpServletResponse response) {
  public ResponseEntity isSignedInRole() {
    // Execution will only get here, if the session is still valid, so sending OK here is enough. Spring sends an error
    // code by itself if the session isn't valid any more
    SecurityContext context = SecurityContextHolder.getContext();
    //if(context.getAuthentication() )
    if (context == null || context.getAuthentication() == null) {
      return new ResponseEntity("User not signed in.", HttpStatus.UNAUTHORIZED);
    } else if ("anonymousUser".equals(context.getAuthentication().getPrincipal())) {
      return new ResponseEntity("User not signed in.", HttpStatus.UNAUTHORIZED);
    } else {
      return new ResponseEntity(SecurityContextHolder.getContext().getAuthentication().getAuthorities(), HttpStatus.OK);
    }
  }

  /**
   * @return
   */
  @RequestMapping(
    value = "/signout",
    method = RequestMethod.POST
  )
  //TODO: move transactionality annotation into the service layer
  @Transactional(propagation = Propagation.SUPPORTS)
  public ResponseEntity logOut(HttpServletRequest request, HttpServletResponse response) {
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
  public ResponseEntity saveAsFavourite() {
    return null;
  }

  @RequestMapping(
    value = "/{userId}/resetPassword",
    method = RequestMethod.POST
  )
  @Transactional(propagation = Propagation.SUPPORTS)
  public ResponseEntity resetPassword(@RequestBody String password) {
    return null;
  }

  private static void myLogoff(HttpServletRequest request, HttpServletResponse response) {
    CookieClearingLogoutHandler cookieClearingLogoutHandler = new CookieClearingLogoutHandler(
      AbstractRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY);
    SecurityContextLogoutHandler securityContextLogoutHandler = new SecurityContextLogoutHandler();
    cookieClearingLogoutHandler.logout(request, response, null);
    securityContextLogoutHandler.logout(request, response, null);
  }


  /**
   * Provides a possibility to ping the app. A temporary solution
   * for the client to avoid session timeout when the user is not
   * making http requests to the app but is still active (i.e. gui
   * events or chatting over web-socket) is to call this method.
   *
   * @return 'pong' as answer to ping
   */
  @ResponseBody
  @RequestMapping(
    value = "/ping",
    produces = MediaType.APPLICATION_JSON_VALUE,
    method = RequestMethod.GET
  )
  public String doPing() {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      logger.info("ping from a user with auth null");
    } else {
      if (SecurityContextHolder.getContext()
                               .getAuthentication().getDetails() == null) {

        logger.info("ping from a user "
                      + SecurityContextHolder.getContext().getAuthentication().getName());
      } else {
        logger.info("ping from a user "
                      + SecurityContextHolder.getContext().getAuthentication().getName()
                      + " with session id "
                      + ((WebAuthenticationDetails) SecurityContextHolder.getContext()
                                                                         .getAuthentication().getDetails())
          .getSessionId());
      }
    }
    return "pong";
  }

}
