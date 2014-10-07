angular.module('won.owner').factory('userService', function ($window, $http, $log) {

	var user = {};
    var registered = false;
	user = $window.user;

	return {
        verifyAuth:function() {
            return $http.post('/owner/rest/users/isSignedIn', user).then (
                function(response){ //success
                    $log.debug(response);
                    $log.debug(response.status);
                    user.isAuth = true;
                    return true;
                },
                function(response){ //error
                    $log.debug(response);
                    $log.debug(response.status);
                    this.resetAuth();
                    return false;

                    //just pass future? errors are better handled with more context information in this case

                    //redirect to /login?
                    //whenever trying to access sthg without permission (e.g. postbox)


                }
            );
        },
		isAuth:function () { //TODO verify auth first (or advise against using this variant)
            //console.log(new Error().stack);
            //this.verifyAuth(); //TODO tmp, sthg causes an automatic redirect (resulting in a loop) if we set off the request here
			return (user.isAuth == true);
		},
		setAuth:function(username) { //TODO verify auth with server first
			user.isAuth = true;
			user.username = username;
            //this.verifyAuth(); //TODO tmp
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
            //TODO also deactive session at server
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
					return {status:"OK"};
				},
				function (data, status) {
					console.log("FATAL ERROR");
				}
			);
		}
	}
});
