angular.module('won.owner').factory('userService', function ($window, $http, $log, $rootScope, applicationStateService) {

	var user = {};
    var registered = false;
	user = $window.user;
    var userService = {};
    var privateData = {}
    privateData.verifiedOnce = false; //gets reset on reload, will be set true after verifying the session cookie with the server

    userService.fetchPostsAndDrafts = function() {
        //if(applicationStateService.getAllNeedsCount()>=0){
        //TODO move all this stuff here to a different function/object and also call it after reloading
        $http.get(
            '/owner/rest/needs/',
            user
        ).then(
            function (needs) {
                if(needs.data.length>0){
                    applicationStateService.addNeeds(needs);
                }
                // success
                return {status:"OK"};
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
            user
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
        return $http.get('rest/users/isSignedIn').then (

            function(response){ //success
                user.isAuth = true;
                return true;
            },
            function(response){ //error
                userService.resetAuth();
                return false;
            }
        );
    };
    userService.isAuth = function () {
        if(user.isAuth == false && privateData.verifiedOnce == false) {
            userService.verifyAuth();
            privateData.verifiedOnce = true;
        }
        return (user.isAuth == true);
    };
    userService.setAuth = function(username) { //TODO deletme?
        user.isAuth = true;
        user.username = username;
    };
    userService.getUserName = function () {
        return user.username;
    };
    userService.getUnescapeUserName = function() {
        if(user.username != null) return user.username.replace("&#64;", '@').replace("&#46;", '.');
        else return null;
    };
    userService.getRegistered = function (){
        return registered;
    };
    userService.resetAuth = function () {
        //TODO also deactive session at server
        user = {
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
                $rootScope.$broadcast(won.EVENT.USER_SIGNED_IN);
                registered = true;
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
                $rootScope.$broadcast(won.EVENT.USER_SIGNED_OUT);
                return {status:"OK"};
            },
            function (data, status) {
                console.log("FATAL ERROR");
            }
        );
	};
    //TODO implement fetching posts on page reload
    /*init = function () {
     if(userService.verifyAuth()) {
     userService.fetchPostsAndDrafts()
     $location.path('/postbox');
     }
     }
     init()*/
    return userService;

});
