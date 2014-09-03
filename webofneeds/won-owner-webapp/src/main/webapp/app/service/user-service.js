angular.module('won.owner').factory('userService', function ($window, $http) {

    var user = {};
    var registered = false;
    user = $window.user;

    return {
        isAuth:function () {
            return (user.isAuth == true);
        },
        setAuth:function(username) {
            user.isAuth = true;
            user.username = username;
        },
        getUserName:function () {
            return user.username;
        },
        getUnescapeUserName:function() {
            if(user.username != null) return user.username.replace("&#64;", '@').replace("&#46;", '.');
            else return null;
        },
        getRegistered:function (){
            return registered;
        },
        resetAuth:function () {
            user = {
                isAuth : false
            }
        },
        registerUser : function(user) {
            return $http.post(
                '/owner/rest/users/',
                user
            ).then(
                function() {
                    // success
                    registered = true;
                    return {status : "OK"};
                },
                function(response) {
                    alert('register status ' + response.status);
                    switch (response.status) {
                        case 409:
                            // normal error
                            return {status:"ERROR", message: "Username is already used"};
                            break;
                        default:
                            // system error
                            console.log("FATAL ERROR");
                            break;
                    }
                }
            );
        },
        logIn : function(user) {
            return $http.post(
                '/owner/rest/users/signin',
                user
            ).then(
                function () {
                    return {status:"OK"};
                },
                function (response) {
                    alert('login status ' + response.status);
                    switch (response.status) {
                        case 403:
                            // normal error
                            return {status:"ERROR", message:"Bad username or password"};
                            break;
                        default:
                            // system error
                            console.log("FATAL ERROR");
                            break;
                    }
                }
            );
        },
        logOut : function() {
            return $http.post(
                '/owner/rest/users/signout'
            ).then(
                function (data, status) {
                    return {status:"OK"};
                },
                function (data, status) {
                    console.log("FATAL ERROR");
                }
            );
        }
    }
});
