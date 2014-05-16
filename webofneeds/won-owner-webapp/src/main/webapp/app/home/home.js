angular.module('won.owner').controller('HomeCtrl', function ($scope, $location, userService) {

	$scope.goToNewNeed = function() {
		if(userService.isAuth()) {
			$location.path("/create-need");
		} else {
			$location.path("/signin");
		}
	}

	$scope.goToAllNeeds = function () {
		if (userService.isAuth()) {
			$location.path("/need-list");
		} else {
			$location.path("/signin");
		}
	}

	$scope.forms = new function() {
		this.signin = ($location.path().indexOf("signin") > -1);
		this.register = ($location.path().indexOf("register") > -1);
		this.reset = function() {
			this.signin = false;
			this.register = false;
		}
	}

});

angular.module('won.owner').controller('SignInCtrl', function ($scope,$route,$window,$location, userService) {

	$scope.user = {
		username:'',
		password:''
	};

	$scope.error = '';

	onLoginResponse = function(result) {
		if (result.status == 'OK') {
			userService.setAuth($scope.username);
			$location.path('/');

		} else {
			$scope.error = result.message;
		}
	}

	$scope.onClickSignIn = function () {
		$scope.error = '';
		if($scope.signInForm.$valid) {
			userService.logIn($scope.user).then(onLoginResponse);
		}
	}


});

angular.module('won.owner').controller('RegisterCtrl', function ($scope, $route,$window, $location, userService) {

	$scope.registerUser = new function(){
		this.reset = function() {
			this.username = '';
			this.password = '';
			this.passwordAgain = '';
		}

		this.isSamePassword = function() {
			return (this.password == this.passwordAgain);
		}

		this.reset();
	};

	$scope.error = '';
	$scope.success = '';
  $scope.registered = false;

	onRegisterResponse = function(result) {
		if(result.status == "OK") {
			$scope.error = '';
			$scope.success = '';

			angular.resetForm($scope, "registerForm");
		//	$scope.success = 'You\'ve been successfully registered. Please try to Sign in';
            $scope.registered = true;
            userService.logIn($scope.registerUser).then(onLoginResponse);
            $scope.registerUser.reset();
            //$location.path("/");
        } else {
			$scope.error = result.message;
		}
	}
    onLoginResponse = function(result) {
        if (result.status == 'OK') {
            userService.setAuth($scope.username);
            $window.location.href = '/owner';

        } else {
            $scope.error = result.message;
        }
    }
	$scope.onClickRegister = function () {
		var validPass = true;
		if(!$scope.registerUser.isSamePassword()) {
			$scope.error = 'Filled in passwords should be same';
			validPass = false;
		}
		if ($scope.registerForm.$valid && validPass) {
			userService.registerUser($scope.registerUser).then(onRegisterResponse);
		}
	}

});

