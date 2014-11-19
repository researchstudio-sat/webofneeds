angular.module('won.owner').factory('userService', function ($window, $http, $log, $rootScope, applicationStateService, utilService) {


    var userService = {};
    var privateData = {
        registered:false,
        user : $window.user
    }
    privateData.verifiedOnce = false; //gets reset on reload, will be set true after verifying the session cookie with the server

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
                console.error("ERROR: unexpected response data for /owner/rest/needs/");
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
                if(drafts.data.length>0){
                    applicationStateService.addDrafts(drafts)
                }
                // success
                return {status:"OK"};
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

    userService.verifyAuth = function() {
        promise = $http.get('rest/users/isSignedIn').then (

            function(response){ //success
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
    userService.getRegistered = function (){
        return privateData.registered;
    };
    userService.resetAuth = function () {
        //TODO also deactive session at server
        privateData.user = {
            isAuth : false
        }
    };
    userService.registerUser = function(user) {
        return $http.post(
                '/owner/rest/users/',
                user
        ).then(
            function() {
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
    userService.logIn = function(user) {
        return $http.post(
                '/owner/rest/users/signin',
                user
        ).then(
            function () {
                // success
                userService.setAuth(user.username);
                $rootScope.$broadcast(won.EVENT.USER_SIGNED_IN);
                return {status:"OK"};
            },
            function (response) {
                switch(response.status) {
                    case 403:
                        // normal error
                        return {status: "ERROR", message: "Incorrect email adress or password"};
                    default:
                        // system error
                        return {status:"FATAL_ERROR", message: "Unknown error occured."};
                    break;
                }
            }
        );
    };
    userService.logOut = function() { //TODO directly pass promise
        return $http.post(
                '/owner/rest/users/signout'
        ).then(
            function (data, status) {
                userService.resetAuth();
                $rootScope.$broadcast(won.EVENT.USER_SIGNED_OUT);
                return {status:"OK"};
            },
            function (data, status) {
                console.log("FATAL ERROR");
            }
        );
	};

    var verified = userService.verifyAuth(); //checking login status
    verified.then(function reloadWhileLoggedIn(loggedIn){
        if(loggedIn) {
            userService.fetchPostsAndDrafts();
        }
    });
    return userService;

});
