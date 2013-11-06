//owner.home.controller
homeModule = angular.module('owner.home', [/*'ui.bootstrap.buttons'*/]);

homeModule.controller('HomeCtrl', function ($scope, $location) {

	$scope.goToNewNeed = function() {
		$location.path("/create-need");
	}

	$scope.goToAllNeeds = function () {
		$location.path("/need-list");
	}

});