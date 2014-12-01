/*
 * Copyright 2012  Research Studios Austria Forschungsges.m.b.H.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

angular.module('won.owner').controller('NeedListCtrl', function ($scope,$log, $location, $http, $q, needService, connectionService) {

	NeedListPage = function() {
		this.btnConversations = 0;
		this.btnRequests = 0;
		this.btnSuggestions = 0;
		this.btnOneTimeNeeds = 0;
		this.selectedNeed = null;
		this.selectedNeedType = null;
		this.myNeeds = [];
		this.requestType = 'All Requests';
		this.categorizedNeeds = new CategorizedNeeds();

		this.resetTopPanel = function() {
		}

		this.startup = function() {
			if(this.categorizedNeeds.conversations.length > 0) {
				this.btnConversations = 1;
			} else if(this.categorizedNeeds.requests.received.length > 0) {
				this.btnRequests = 1;
			} else if (this.categorizedNeeds.requests.sent.length > 0) {
				this.btnRequests = 1;
			} else if (this.categorizedNeeds.suggestions.length > 0) {
				this.btnSuggestions = 1;
			}
		};
	};

	$scope.model = new NeedListPage();

	$scope.$watch('model.btnConversations', function(newVal, oldVal) {
		if(newVal == 1) {
			$scope.model.btnRequests = 0;
			$scope.model.btnSuggestions = 0;
		}
	});

	$scope.$watch('model.btnRequests', function (newVal, oldVal) {
		if (newVal == 1) {
			$scope.model.btnConversations = 0;
			$scope.model.btnSuggestions = 0;
		}
	});

	$scope.$watch('model.btnSuggestions', function (newVal, oldVal) {
		if (newVal == 1) {
			$scope.model.btnConversations = 0;
			$scope.model.btnRequests = 0;
		}
	});

	$scope.inMatchesMode = false;

	$scope.noMatches = false;

	$scope.matches = [];

	var categorizeNeeds = function(catNeeds) {
		var categorizedNeeds = new CategorizedNeeds();
		angular.forEach(catNeeds, function (catNeed) {
			if (catNeed) {
				categorizedNeeds.merge(catNeed);
			}
		});
        $log.debug(categorizedNeeds);
		$scope.model.categorizedNeeds = categorizedNeeds;
		$scope.model.startup();
	};

	needService.getAllNeeds().then(function(needs) {
		$scope.model.myNeeds = needs;
		var allConnsRequests = [];
		angular.forEach(needs, function (need) {
			allConnsRequests.push(needService.getNeedConnections(need.uri));
		});
		$q.allSettled(allConnsRequests).then(function(responses) {
			categorizeNeeds(responses);
		}, function(responses) {
			categorizeNeeds(responses);
		});
	});

	$scope.goToDetail = function($id) {
		$location.path("/need-detail/" + $id);
	};

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
				});
			});
		});
	};

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
	};

	$scope.connect = function() {
	};

});

