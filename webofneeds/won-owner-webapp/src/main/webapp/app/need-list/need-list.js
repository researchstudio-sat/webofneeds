//owner.home.controller
needListModule = angular.module('owner.needlist', ['ui.map', 'ui.bootstrap', 'owner.service.need', 'owner.service.connection']);

needListModule.controller('NeedListCtrl', function ($scope, $location, $http, needService, connectionService) {

	$scope.isCollapse =  true;

	$scope.needs = [];

	$scope.inMatchesMode = false;

	$scope.noMatches = false;

	$scope.matches = [];

	needService.getAllNeeds().then(function(response) {
		$scope.needs = response.data;
	});

	$scope.goToDetail = function($id) {
		$location.path("/need-detail/" + $id);
	}

	$scope.showMatches = function($event) {
		$event.preventDefault();

		needService.getNeedMatches(this.need.needId).then(function(response) {
			$scope.inMatchesMode = true;
			if(response.length == 0) {
				$scope.noMatches = true;
			} else {
				$scope.matches = response;
			}
		});

		this.need.selected = 'selected';
		var elm = $($event.target).closest('.panel-need');
		$("#need-list").children().not(elm).each(function(index, needPnl) {
			var myScope = $scope;
			$(needPnl).animate({
				opacity:0
			}, 700, function () {
				myScope.$apply(function() {
					$(needPnl).scope().need.hidden = true;
				})
			});
		});
	}

	$scope.hideMatches = function($event) {
		$event.preventDefault();
		$scope.inMatchesMode = false;
		$scope.noMatches = false;
		$scope.matches = [];

		angular.forEach(this.needs, function(need) {
			need.hidden = false;
		});
		var elm = $($event.target).closest('.panel-need');
		$("#need-list").children().not(elm).each(function (index, needPnl) {
			var myScope = $scope;
			$(needPnl).animate({
				opacity:1
			}, 700, function () {
			});
		});
		this.need.selected = '';
	}

	$scope.connect = function() {
	}

	$scope.connect = function () {
	}

	$scope.connect = function () {
	}

});

