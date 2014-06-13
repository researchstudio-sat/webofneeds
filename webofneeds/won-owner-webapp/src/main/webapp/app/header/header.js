angular.module('won.owner').controller("HeaderCtrl", function($scope, $window,$location, userService) {

	$scope.isActive = function(where) {
		if ($location.path().indexOf(where) > -1) {
			return 'active';
		} else if(where == undefined && $location.path() == '/') {
			return 'active';
		}
	};

    $scope.authenticated = false;

	$scope.showPublic = function() {
        $scope.authenticated = !userService.isAuth();
		return  $scope.authenticated;
	};

    $scope.checkRegistered = function(){
        return userService.getRegistered();
    };
    $scope.userdata = { username : userService.getUserName()};
    $scope.message = "successfully registered";

    $scope.$watch(userService.isAuth, function(logged_in){
        if(logged_in){
            $scope.userdata = { username : userService.getUserName()};
            $scope.authenticated = true;
            //$window.location.reload();
          //  $location.path('/');

      //      $window.location.reload();
       //     $scope.showPublic();
      //      $scope.$apply();
        }
    })

	onResponseSignOut = function (result) {
		if (result.status == 'OK') {
			userService.resetAuth();
			$location.path("/");
		}
	};

	$scope.onClickSignOut = function() {
		userService.logOut().then(onResponseSignOut);
	};

});