angular.module('won.owner').factory('userService', function ($window, $http, $rootScope) {

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
		},
		logIn : function(user) {
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
		},
		logOut : function() { //TODO directly pass promise
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
		}
	}
});
