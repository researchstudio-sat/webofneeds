angular.module('won.owner').factory('userService', function ($window
    , $http
    , $log
    , $q
    , $rootScope
    , applicationStateService
    , utilService
    , messageService) {


    var userService = {};
    var privateData = {
        registered:false,
        private:false,
        user : $window.user
    }
    if (privateData.user != null
        && "&#91;ROLE&#95;PRIVATE&#93;" == privateData.user.authorities) {
        privateData.private = true;
    }
    privateData.verifiedOnce = false; //gets reset on reload, will be set true after verifying the session cookie with the server

    userService.fetchPosts = function() {
        return $http.get(
            '/owner/rest/needs/',
            privateData.user
        ).then(
            function (needs) {
                if(utilService.isString(needs.data)) {
                    // unexpected response data, probably a redirect to another web-page
                    $log.error("ERROR: unexpected response data for /owner/rest/needs/");
                    return {status: "ERROR", message: "unexpected response data"};
                } else {
                    if (needs.data.length>0){
                        return applicationStateService.addNeeds(needs);
                    }
                    // success
                    //return {status:"OK", "data": needs.data};
                }

            },
            function (response) {
                switch(response.status) {
                    case 403:
                        // normal error
                        return {status: "ERROR", message: "getting needs of a user failed"};
                    default:
                        // system error
                        return {status:"FATAL_ERROR", message: "getting needs of a user failed"};
                        break;
                }
            }
        )

    }


    userService.fetchPostsAndDrafts = function() {
        //if(applicationStateService.getAllNeedsCount()>=0){
        $http.get(
            '/owner/rest/needs/',
            privateData.user
        ).then(
        function (needs) {
            // when we were having a bug that results in redirect here returning owner web-page
            // instead of array of needs, we still get the http status 200 at this point. This
            // is because the redirect is already handled by the browser by this time and the
            // redirected page returns OK 200 that we see here. Therefore, here is just a hack
            // to check if the response data is not a string (we except array here).
            if(utilService.isString(needs.data)) {
                // unexpected response data, probably a redirect to another web-page
                $log.error("ERROR: unexpected response data for /owner/rest/needs/");
                return {status: "ERROR", message: "unexpected response data"};
            } else {
                if (needs.data.length>0){
                    applicationStateService.addNeeds(needs);
                }
                // success
                return {status:"OK"};
            }

        },
        function (response) {
            switch(response.status) {
                case 403:
                    // normal error
                    return {status: "ERROR", message: "getting needs of a user failed"};
                default:
                    // system error
                    return {status:"FATAL_ERROR", message: "getting needs of a user failed"};
                    break;
            }
        }
        )
        //}
        //if(applicationStateService.getAllDraftsCount()>=0){
        $http.get(
            '/owner/rest/needs/drafts/',
            privateData.user
        ).then(
            function (drafts) {
                if(utilService.isString(drafts.data)) {
                    // unexpected response data, probably a redirect to another web-page
                    $log.error("ERROR: unexpected response data for /owner/rest/drafts/");
                    return {status: "ERROR", message: "unexpected response data"};
                } else {
                    if(drafts.data.length>0){
                        applicationStateService.addDrafts(drafts)
                    }
                    // success
                    return {status:"OK"};
                }
            },
            function (response) {
                switch(response.status) {
                    case 403:
                        // normal error
                        return {status: "ERROR", message: "getting drafts of a user failed"};
                    default:
                        // system error
                        return {status:"FATAL_ERROR", message: "getting drafts of a user failed"};
                        break;
                }
            }
        )
        //}
    }

    userService.removeDraft = function(draftUri) {
        var deferred = $q.defer();
        $http.delete(
            '/owner/rest/needs/drafts/draft/?uri=' + encodeURIComponent(draftUri),
            privateData.user
        ).then(
            function success(response) {
                applicationStateService.removeDraft(draftUri);
                deferred.resolve(draftUri);
                // TODO broadcast success notification?
            },
            function error(response) {
                var errorResponse = {};
                errorResponse.message = "getting drafts of a user failed";
                switch(response.status) {
                    case 403:
                        // normal error
                        errorResponse.status = "ERROR";
                        deferred.reject(errorResponse);
                    default:
                        // system error
                        errorResponse.status = "FATAL_ERROR";
                        deferred.reject(errorResponse);
                        break;
                }
                // TODO broadcast error notification?
            }
        )
        return deferred.promise;
    }

    userService.verifyAuth = function() {
        promise = $http.get('rest/users/isSignedInRole').then (

            function(response){ //success
                if (angular.isArray(response.data) && response.data.length == 1 && response.data[0].authority  === "ROLE_PRIVATE") {
                    privateData.private = true;
                }
                privateData.user.isAuth = true;
                return true;
            },
            function(response){ //error
                userService.resetAuth();
                return false;
            }
        );//.done() make sure exceptions aren't lost?
        return promise
        //return privateData.user.isAuth //TODO bad as it can return before the http request resolves (see "TODO fix bug" below)
    };
    
    // TODO fix bug: looks like a bug: returns before userService.verifyAuth() returns
    // ^--> make isAuth always return a future
    userService.isAuth = function () {
        /*if(privateData.user.isAuth == false && privateData.verifiedOnce == false) {
            userService.verifyAuth();
            privateData.verifiedOnce = true;
        }*/
        return (privateData.user.isAuth == true);
    };
    userService.setAuth = function(username) { //TODO deletme?
        privateData.user.isAuth = true;
        privateData.user.username = username;
    };
    userService.getUserName = function () {
        return privateData.user.username;
    };
    userService.getUnescapeUserName = function() {
        if(privateData.user.username != null) return privateData.user.username.replace("&#64;", '@').replace("&#46;", '.');
        else return null;
    };
    userService.isRegistered = function (){
        return privateData.registered;
    };
    userService.isPrivateUser = function (){
        return (userService.isAuth() && privateData.private);
    };
    userService.isAccountUser = function (){
        return (userService.isAuth() && !privateData.private);
    };

    userService.resetAuth = function () {
        //TODO also deactive session at server
        privateData.user = {
            isAuth : false
        }
        privateData.registered = false
        privateData.private = false
    };
    userService.registerUser = function(user) {
        return $http.post(
                '/owner/rest/users/',
                user
        ).then(
            function(response) {
                // success
                privateData.registered = true;
                return {status : "OK"};
            },
            function(response) {
                switch (response.status) {
                    case 409:
                        // normal error
                        return {status:"ERROR", message: "Email address already in use."};
                    break;
                    default:
                        // system error
                        return {status:"FATAL_ERROR", message: "Unknown error occured."};
                    break;
                }
            }
        );
    };
    userService.registerPrivateLinkUser = function() {
        var user = {};
        user.username = 'temp';
        user.password = 'dummy';
        user.passwordAgain = 'dummy';
        return $http.post(
            '/owner/rest/users/private',
            user
        ).then(
            function(response) {
                // success
                privateData.registered = true;
                return {status : "OK", privateLink: response.data};
            },
            function(response) {
                switch (response.status) {
                    case 409:
                        // normal error
                        return {status:"ERROR", message: "Email address already in use."};
                        break;
                    default:
                        // system error
                        return {status:"FATAL_ERROR", message: "Unknown error occured."};
                        break;
                }
            }
        );
    };


    userService.logIn = function(user, asPrivateLink) {

        return $http.post(
            '/owner/rest/users/signin',
            user
        ).then(
            function () {
                // success
                userService.setAuth(user.username);
                privateData.registered = true;
                if (asPrivateLink == true) {
                    privateData.private = true;
                }
                messageService.reconnect();
                return {status: "OK"}
            },
            function (response) {
                switch (response.status) {
                    case 403:
                        // normal error
                        return {status: "ERROR", message: "Incorrect email adress or password"};
                    default:
                        // system error
                        return {status: "FATAL_ERROR", message: "Unknown error occured."};
                        break;
                }
            }
        );
    };

    userService.logInAndSetUpApplicationState = function(user, asPrivateLink) {
        return userService.logIn(user, asPrivateLink).then(
            function success(data) {
                applicationStateService.reset();
                if (data.status == "OK") {
                    if (userService.isPrivateUser()) {
                        return userService.fetchPosts().then(
                            function () {
                                var keys = Object.keys(applicationStateService.getAllNeeds());
                                if (keys.length == 1) {
                                    applicationStateService.setCurrentNeedURI(keys[0]);
                                    // the above line will also trigger won.EVENT.APPSTATE_CURRENT_NEED_CHANGED and reloadCurrentNeedData();
                                    return {status: "OK"};
                                } else if (keys.length == 0) {
                                    // also OK if the private link was just registered, the need is not created yet
                                    $rootScope.$broadcast(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED);
                                    return {status: "OK"};
                                } else {
                                    //TODO error
                                    $log.debug("Wrong number of needs for private link " + keys);
                                    return {status: "ERROR"};
                                }
                            }
                        );
                    } else {
                        userService.fetchPostsAndDrafts();
                        $rootScope.$broadcast(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED);
                        return {status: "OK"};
                    }
                } else {
                    return data;
                }
            });
    };


    userService.logOut = function() { //TODO directly pass promise
        return $http.post(
            '/owner/rest/users/signout'
        ).then(
            function success(data, status) {
                $log.debug("Successfully logged-out");
                userService.resetAuth();
                messageService.reconnect();
                return {status: "OK"};
            },
            function error(data, status) {
                $log.error("ERROR: failed to log-out");
                return {status: "FATAL_ERROR"};
            }
        );
	};

    userService.logOutAndSetUpApplicationState = function() {
        return userService.logOut().then(
            function success(data, status) {
                applicationStateService.reset();
                $rootScope.$broadcast(won.EVENT.APPSTATE_CURRENT_NEED_CHANGED);
                return {status: "OK"};
            },
            function error(data, status) {
                //TODO
                return {status: "FATAL_ERROR"};
            }
        );
    };

    userService.setUpRegistrationForUserWithPrivateLink = function (pLink) {
        if (userService.isAuth()) {
            // sign-out the current private link/account user, then sign-in with
            // the provided private link
            //TODO error handling
            return userService.logOutAndSetUpApplicationState().then(
                function(data) {
                    return userService.logInAndSetUpApplicationState({username:pLink, password:'dummy'}, true);
                }
            );
        } else {
            // sign-in with the provided private link
            //TODO error handling
            return userService.logInAndSetUpApplicationState({username: pLink, password: 'dummy'}, true);
        }
    }

    userService.setUpRegistrationForUserPublishingNeed = function () {
        if (userService.isAccountUser()) {
            // do nothing: sign-in user wants to publish another need - OK
            var deferred = $q.defer();
            deferred.resolve("OK");
            return deferred.promise;
        } else if (userService.isPrivateUser()) {
            // sign-out the current private link user, then register
            // and sign-in a new need with new private link
            //TODO error handling

            return userService.logOutAndSetUpApplicationState()
                .then(
                function(data) {
                    return  userService.registerPrivateLinkUser();
                })
                .then(
                function(data) {
                    return userService.logInAndSetUpApplicationState({username:data.privateLink, password:'dummy'}, true);
                }
            );
        } else {
            //register private link user account and sign him in
            //TODO error handling

            return userService.registerPrivateLinkUser().then(
                function(data) {
                    return userService.logInAndSetUpApplicationState({username:data.privateLink, password:'dummy'}, true);
                }
            );
        }
    }

    var verified = userService.verifyAuth(); //checking login status
    verified.then(function reloadWhileLoggedIn(loggedIn){
        if(loggedIn) {
            userService.fetchPostsAndDrafts();
        }
      //  $rootScope.$apply()
    });
    return userService;

});
