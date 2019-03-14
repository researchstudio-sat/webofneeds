
/*
 * This file is subject to the terms and conditions defined in file 'LICENSE.txt', which is part of this source code package.
 */

package won.owner.web.rest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.NullRememberMeServices;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.Errors;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;

import won.owner.model.EmailVerificationToken;
import won.owner.model.KeyStoreIOException;
import won.owner.model.User;
import won.owner.model.UserNeed;
import won.owner.pojo.AnonymousLinkPojo;
import won.owner.pojo.ChangePasswordPojo;
import won.owner.pojo.RestStatusResponse;
import won.owner.pojo.TransferUserPojo;
import won.owner.pojo.UserPojo;
import won.owner.pojo.UserSettingsPojo;
import won.owner.pojo.UsernamePojo;
import won.owner.pojo.VerificationTokenPojo;
import won.owner.repository.UserNeedRepository;
import won.owner.service.impl.KeystoreEnabledPersistentRememberMeServices;
import won.owner.service.impl.KeystoreEnabledUserDetails;
import won.owner.service.impl.UserAlreadyExistsException;
import won.owner.service.impl.UserNotFoundException;
import won.owner.service.impl.UserService;
import won.owner.service.impl.WrongOldPasswordException;
import won.owner.web.WonOwnerMailSender;
import won.owner.web.events.OnExportUserEvent;
import won.owner.web.events.OnPasswordChangedEvent;
import won.owner.web.events.OnRegistrationCompleteEvent;
import won.owner.web.validator.PasswordChangeValidator;
import won.owner.web.validator.UserRegisterValidator;

/**
 * User: t.kozel
 * Date: 11/12/13
 */
@Controller
@RequestMapping("/rest/users")
public class RestUserController {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private AuthenticationManager authenticationManager;

    private SecurityContextRepository securityContextRepository;

    private UserRegisterValidator userRegisterValidator;
    
    private PasswordChangeValidator passwordChangeValidator;

    private UserNeedRepository userNeedRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private KeystoreEnabledPersistentRememberMeServices keystoreEnabledPersistentRememberMeServices;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private WonOwnerMailSender emailSender;

    @Autowired
    ServletContext context;

    @Autowired
    RememberMeServices rememberMeServices = new NullRememberMeServices();

    @Autowired
    public RestUserController(final AuthenticationManager authenticationManager,
                              final SecurityContextRepository securityContextRepository,
                              final UserRegisterValidator userRegisterValidator,
                              final PasswordChangeValidator passwordChangeValidator,
                              final UserNeedRepository userNeedRepository) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.userRegisterValidator = userRegisterValidator;
        this.passwordChangeValidator = passwordChangeValidator;
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
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST
    )
    //TODO: move transactionality annotation into the service layer
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity registerUser(@RequestBody UserPojo user, Errors errors, WebRequest request) {
        try {
            userRegisterValidator.validate(user, errors);
            if (errors.hasErrors()) {
                if (errors.getFieldErrorCount() > 0) {
                    // someone trying to go around js validation
                    return generateStatusResponse(RestStatusResponse.SIGNUP_FAILED);
                } else {
                    // username is already in database
                    return generateStatusResponse(RestStatusResponse.USER_ALREADY_EXISTS);
                }
            }
            User createdUser = userService.registerUser(user.getUsername(), user.getPassword(), null, user.getPrivateId());

            if(!createdUser.isAnonymous() && !createdUser.isEmailVerified()) {
                eventPublisher.publishEvent(new OnRegistrationCompleteEvent(createdUser, request.getLocale(), request.getContextPath()));
            }
        } catch (UserAlreadyExistsException e) {
            // username is already in database
            return generateStatusResponse(RestStatusResponse.USER_ALREADY_EXISTS);
        }

        return generateStatusResponse(RestStatusResponse.USER_CREATED);
    }

    /**
     * Changes the user's password
     *
     * @param changePasswordPojo    password changing data
     * @param errors
     * @return ResponseEntity with Http Status Code
     */
    @ResponseBody
    @RequestMapping(
            value = "/changePassword",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST
    )
    //TODO: move transactionality annotation into the service layer
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity changePassword(@RequestBody ChangePasswordPojo changePasswordPojo, Errors errors, HttpServletRequest request, HttpServletResponse response) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        if (username == null) {
            return generateStatusResponse(RestStatusResponse.USER_NOT_SIGNED_IN);
        }
        if (!username.equals(changePasswordPojo.getUsername())) {
            return generateStatusResponse(RestStatusResponse.USERNAME_MISMATCH);
        }
        try {
            passwordChangeValidator.validate(changePasswordPojo, errors);
            if (errors.hasErrors()) {
                if (errors.getFieldErrorCount() > 0) {
                    return generateStatusResponse(RestStatusResponse.PASSWORDCHANGE_BAD_PASSWORD);
                } else {
                    // username is not found
                    return generateStatusResponse(RestStatusResponse.PASSWORDCHANGE_USER_NOT_FOUND);
                }
            }
            User user = userService.changePassword(changePasswordPojo.getUsername(), changePasswordPojo.getNewPassword(), changePasswordPojo.getOldPassword());
            eventPublisher.publishEvent(new OnPasswordChangedEvent(user, request.getLocale(), request.getContextPath()));
            return generateUserResponse(user);
        } catch (WrongOldPasswordException e) {
            return generateStatusResponse(RestStatusResponse.PASSWORDCHANGE_WRONG_OLD_PASSWORD);
        } catch (UserNotFoundException e) {
            return generateStatusResponse(RestStatusResponse.USER_NOT_FOUND);
        } catch (KeyStoreIOException e) {
            return generateStatusResponse(RestStatusResponse.PASSWORDCHANGE_KEYSTORE_PROBLEM);
        } 
    }
    
    /**
     * transfers a privateId user to a registered user
     *
     * @param errors
     * @return ResponseEntity with Http Status Code
     */
    @ResponseBody
    @RequestMapping(
            value = "/transfer",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST
    )
    //TODO: move transactionality annotation into the service layer
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity transferUser(@RequestBody TransferUserPojo transferUserPojo, Errors errors, WebRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // cannot use user object from context since hw doesn't know about created in this session need,
        // therefore, we have to retrieve the user object from the user repository
        User user = userService.getByUsername(username);
        if (user == null && !transferUserPojo.getPrivateUsername().equals(user.getUsername())) {
            return generateStatusResponse(RestStatusResponse.USERNAME_MISMATCH);
        }

        try {
            userRegisterValidator.validate(transferUserPojo, errors);
            if (errors.hasErrors()) {
                if (errors.getFieldErrorCount() > 0) {
                    // someone trying to go around js validation
                    return generateStatusResponse(RestStatusResponse.SIGNUP_FAILED);
                } else {
                    // username is already in database
                    return generateStatusResponse(RestStatusResponse.USER_ALREADY_EXISTS);
                }
            }
            User transferUser = userService.transferUser(transferUserPojo.getUsername(), transferUserPojo.getPassword(), transferUserPojo.getPrivateUsername(), transferUserPojo.getPrivatePassword());

            if(!transferUser.isEmailVerified()) {
                eventPublisher.publishEvent(new OnRegistrationCompleteEvent(transferUser, request.getLocale(), request.getContextPath()));
            }
        } catch (UserAlreadyExistsException e) {
            // username is already in database
            return generateStatusResponse(RestStatusResponse.USER_ALREADY_EXISTS);
        } catch (UserNotFoundException e) {
            return generateStatusResponse(RestStatusResponse.TRANSFERUSER_NOT_FOUND);
        }
        return generateStatusResponse(RestStatusResponse.USER_TRANSFERRED);
    }

    @ResponseBody
    @RequestMapping(
            value = "/settings",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET
    )
    public UserSettingsPojo getUserSettings(@RequestParam("uri") String uri) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // cannot use user object from context since hw doesn't know about created in this session need,
        // therefore, we have to retrieve the user object from the user repository
        User user = userService.getByUsername(username);
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
        User user = userService.getByUsername(username);
        if (!user.getUsername().equals(userSettingsPojo.getUsername())) {
            return generateStatusResponse(RestStatusResponse.USERNAME_MISMATCH);
        }

        if (user.getEmail() == null) {
            //TODO validate email server-side?
            // set email:
            user.setEmail(userSettingsPojo.getEmail());
            userService.save(user);
        } else if (!user.getEmail().equals(userSettingsPojo.getEmail())) {
            //TODO validate email server-side?
            // change email:
            user.setEmail(userSettingsPojo.getEmail());
            userService.save(user);
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
            return new ResponseEntity("\"" + userSettingsPojo.getNeedUri() + " need uri problem.\"", HttpStatus.BAD_REQUEST);
        }
        return generateStatusResponse(RestStatusResponse.SETTINGS_CREATED);
    }

    /**
     * check authentication and returrn ResponseEntity with HTTP status code
     *
     * @param request
     * @param response
     * @return
     */
    @ResponseBody
    @RequestMapping(
            value = "/signin",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST
    )
    //TODO: move transactionality annotation into the service layer
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity logIn(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            @RequestParam(name="privateId", required = false) String privateId,
            HttpServletRequest request,
            HttpServletResponse response) {
        Optional<User> user = Optional.empty();
        try {
            user = Optional.of(performLoginForUser(username, password, request, response));
        } catch (BadCredentialsException ex) {
            rememberMeServices.loginFail(request, response);
            return generateStatusResponse(RestStatusResponse.USER_BAD_CREDENTIALS);
        } catch (CredentialsExpiredException ex) {
            rememberMeServices.loginFail(request, response);
            return generateStatusResponse(RestStatusResponse.USER_NOT_VERIFIED);
        }
        if (user.isPresent()) {
            return generateUserResponse(user.get());
        } 
        return generateStatusResponse(RestStatusResponse.USER_NOT_SIGNED_IN);
    }

    private User performLoginForUser(String username, String password, HttpServletRequest request,
            HttpServletResponse response) throws BadCredentialsException, CredentialsExpiredException {
        SecurityContext context = SecurityContextHolder.getContext();
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(username,
            password);
        Authentication auth = authenticationManager.authenticate(token);
        SecurityContextHolder.getContext().setAuthentication(auth);
        securityContextRepository.saveContext(SecurityContextHolder.getContext(), request, response);
        rememberMeServices.loginSuccess(request, response, auth);
        return userService.getByUsername(username);
    }

    /**
     * Method only accessible if the user's still signed in / the session's still valid -> Use it to check the session cookie.
     */
    //* @param user user object
    //* @param request
    //* @param response
    //* @return
    //
    @ResponseBody
    @RequestMapping(
            value = "/isSignedIn",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.GET
    )
    @Transactional(propagation = Propagation.REQUIRED)
    //TODO: move transactionality annotation into the service layer
    public ResponseEntity isSignedIn(HttpServletRequest request, HttpServletResponse response) {
        // Execution will only get here, if the session is still valid, so sending OK here is enough. Spring sends an error
        // code by itself if the session isn't valid any more
        SecurityContext context = SecurityContextHolder.getContext();
        Authentication authentication = null;
        if (context != null) {
            authentication = context.getAuthentication();
        }
        if (authentication == null) {
            authentication = rememberMeServices.autoLogin(request, response);
        } else if (authentication instanceof AnonymousAuthenticationToken) {
            //if we're anonymous, try to see if we can reactivate a remember-me session
            Authentication anonAuth = authentication;
            authentication = rememberMeServices.autoLogin(request, response);
            if (authentication == null) {
                authentication = anonAuth;
            }
        }
        if (authentication == null || "anonymousUser".equals(authentication.getPrincipal())) {
            return generateStatusResponse(RestStatusResponse.USER_NOT_SIGNED_IN);
        } else {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            User authUser = ((KeystoreEnabledUserDetails) authentication.getPrincipal()).getUser();

            return generateUserResponse(userService.getByUsername(authUser.getUsername()));
        }
    }

    @RequestMapping(
            value = "/signout",
            method = RequestMethod.POST
    )
    public ResponseEntity logOut(HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.getContext();
        if (context.getAuthentication() == null) {
            return new ResponseEntity("\"No user is signed in, ignoring this request.\"", HttpStatus.NOT_MODIFIED);
        } else {
            keystoreEnabledPersistentRememberMeServices.logout(request, response, context.getAuthentication());
            new SecurityContextLogoutHandler().logout(request, response, context.getAuthentication());
        }
        return generateStatusResponse(RestStatusResponse.USER_SIGNED_OUT);
    }


    @RequestMapping(
            value = "/{userId}/favourites",
            method = RequestMethod.POST
    )
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity saveAsFavourite() {
        return null;
    }

    @ResponseBody
    @RequestMapping(
            value = "/confirmRegistration",
            method = RequestMethod.POST
    )
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity confirmRegistration(@RequestBody VerificationTokenPojo token) {
        EmailVerificationToken verificationToken = userService.getEmailVerificationToken(token.getToken());

        if(verificationToken == null) {
            return generateStatusResponse(RestStatusResponse.TOKEN_NOT_FOUND);
        }

        User user = verificationToken.getUser();
        Calendar cal = Calendar.getInstance();

        if((verificationToken.getExpiryDate().getTime() - cal.getTime().getTime()) <= 0) {
            return generateStatusResponse(RestStatusResponse.TOKEN_EXPIRED);
        }

        user.setEmailVerified(true);
        userService.save(user);

        return generateStatusResponse(RestStatusResponse.TOKEN_VERIFICATION_SUCCESS);
    }

    @ResponseBody
    @RequestMapping(
            value = "/exportAccount",
            method = RequestMethod.POST
    )
    public ResponseEntity exportAccount(@RequestParam(name = "email", required = false) String responseEmail, @RequestParam(name="keyStorePassword", required = false) String keyStorePassword) {

        SecurityContext securityContext = SecurityContextHolder.getContext();
        User authUser = ((KeystoreEnabledUserDetails) securityContext.getAuthentication().getPrincipal()).getUser();
        User user = userService.getByUsername(authUser.getUsername());

        if(responseEmail == null) {
            if(user.isAnonymous()) {
                return generateStatusResponse(RestStatusResponse.EXPORT_IS_ANONYMOUS);
            } else {
                responseEmail = user.getEmail();
            }
        }

        eventPublisher.publishEvent(new OnExportUserEvent(securityContext.getAuthentication(), keyStorePassword, responseEmail));

        return generateStatusResponse(RestStatusResponse.EXPORT_SUCCESS);
    }

    @ResponseBody
    @RequestMapping(
            value = "/acceptTermsOfService",
            produces = MediaType.APPLICATION_JSON_VALUE,
            method = RequestMethod.POST
    )
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity acceptTermsOfService() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        // cannot use user object from context since hw doesn't know about created in this session need,
        // therefore, we have to retrieve the user object from the user repository
        User user = userService.getByUsername(username);
        if (user == null) {
            return generateStatusResponse(RestStatusResponse.USER_NOT_FOUND);
        }

        if(user.isAcceptedTermsOfService()){
            return generateStatusResponse(RestStatusResponse.TOS_ACCEPT_SUCCESS);
        } else {
            user.setAcceptedTermsOfService(true);
            userService.save(user);
            return generateStatusResponse(RestStatusResponse.TOS_ACCEPT_SUCCESS);
        }
    }

    @ResponseBody
    @RequestMapping(
            value = "/resendVerificationEmail",
            method = RequestMethod.POST
    )
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity resendVerificationEmail(@RequestBody UsernamePojo usernamePojo) {
        User user = userService.getByUsername(usernamePojo.getUsername());

        if(user == null) {
            return generateStatusResponse(RestStatusResponse.USER_NOT_FOUND);
        }

        if(user.isAnonymous()) {
            return generateStatusResponse(RestStatusResponse.TOKEN_RESEND_FAILED_USER_ANONYMOUS);
        }

        if(user.isEmailVerified()) {
            return generateStatusResponse(RestStatusResponse.TOKEN_RESEND_FAILED_ALREADY_VERIFIED);
        }

        EmailVerificationToken verificationToken = userService.getEmailVerificationToken(user);

        if(verificationToken == null || verificationToken.isExpired()) {
            verificationToken = userService.createEmailVerificationToken(user);
        }
        if(verificationToken == null) {
            return generateStatusResponse(RestStatusResponse.TOKEN_CREATION_FAILED);
        }

        emailSender.sendVerificationMessage(user, verificationToken);
        return generateStatusResponse(RestStatusResponse.TOKEN_RESEND_SUCCESS);
    }

    @ResponseBody
    @RequestMapping(
            value = "/sendAnonymousLinkEmail",
            method = RequestMethod.POST
    )
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity sendAnonymousEmail(@RequestBody AnonymousLinkPojo anonymousLinkPojo) {
        emailSender.sendAnonymousLinkMessage(anonymousLinkPojo.getEmail(), anonymousLinkPojo.getPrivateId());
        return generateStatusResponse(RestStatusResponse.USER_ANONYMOUSLINK_SENT);
    }

    @RequestMapping(
            value = "/{userId}/resetPassword",
            method = RequestMethod.POST
    )
    @Transactional(propagation = Propagation.SUPPORTS)
    public ResponseEntity resetPassword(@RequestBody String password) {
        return null;
    }


    public void setRememberMeServices(RememberMeServices rememberMeServices) {
        this.rememberMeServices = rememberMeServices;
    }

    private static ResponseEntity generateStatusResponse(RestStatusResponse restStatusResponse) {
        //TODO: Maybe change to return a pojo
        Map values = new HashMap<String, String>();
        values.put("code", restStatusResponse.getCode());
        values.put("message", restStatusResponse.getMessage());

        return new ResponseEntity(values, restStatusResponse.getHttpStatus());
    }

    private static ResponseEntity generateUserResponse(User user) {
        //TODO: Maybe change to return a pojo
        Map values = new HashMap<String, String>();
        values.put("username", user.getUsername());
        values.put("authorities", user.getAuthorities());
        values.put("role", user.getRole());
        values.put("emailVerified", user.isEmailVerified());
        values.put("acceptedTermsOfService", user.isAcceptedTermsOfService());
        values.put("privateId", user.getPrivateId());
        values.put("isAnonymous", user.isAnonymous());

        return new ResponseEntity<Map>(values, HttpStatus.OK);
    }
}
