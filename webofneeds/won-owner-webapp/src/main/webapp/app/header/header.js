headerModule = angular.module('owner.header', []);

headerModule.controller("HeaderCtrl", function($scope, $location, userService) {

	$scope.isActive = function(where) {
		if ($location.path().indexOf(where) > -1) {
			return 'active';
		} else if(where == undefined && $location.path() == '/') {
			return 'active';
		}
	}

	$scope.showPublic = function() {
		return !userService.isAuth();
	}


	onResponseSignOut = function (result) {
		if (result.status == 'OK') {
			userService.resetAuth();
			$location.path("/");
		}
	}

	$scope.onClickSignOut = function() {
		userService.logOut().then(onResponseSignOut);
	}

});