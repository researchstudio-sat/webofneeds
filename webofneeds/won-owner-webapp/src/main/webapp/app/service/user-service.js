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
            var response = $http.post(
					'/owner/rest/user/',
					user);
            response.success(function() {registered = true;}); //will be called in addition to any other success handler
            return response;
		},
		logIn : function(user) {
			return $http.post(
					'/owner/rest/user/login',
					user
			)
		},
		logOut : function() { //TODO directly pass promise
			return $http.post(
					'/owner/rest/user/logout'
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
