needServiceModule = angular.module('owner.service.user', []);

needServiceModule.factory('userService', function ($window) {

	var user = {};
	user = $window.user;

	return {
		isAuth:function () {
			return (user.isAuth == true);
		},
		getUserName:function () {
			return user.username;
		},
		resetAuth:function () {
			user = {
				isAuth : false
			}
		}
	}
});
