headerModule = angular.module('owner.header', []);

headerModule.controller("HeaderCtrl", function($scope, $location) {

	$scope.isActive = function(where) {
		if ($location.path().indexOf(where) > -1) {
			return 'active';
		} else if(where == undefined && $location.path() == '/') {
			return 'active';
		}
	}
});