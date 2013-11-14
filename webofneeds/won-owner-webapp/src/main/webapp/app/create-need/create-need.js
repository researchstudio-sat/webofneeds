//owner.home.controller
createNeedModule = angular.module('owner.createneed', ['ui.map', 'ui.bootstrap.buttons', 'owner.service.need']);

createNeedModule.controller('CreateNeedCtrl', function ($scope, $location, $http, needService, userService) {
	$scope.mapOptions = {
		center : new google.maps.LatLng(35.784, -78.670),
		zoom : 15,
		mapTypeId:google.maps.MapTypeId.ROADMAP
	};

	$scope.marker = null;

	$scope.need = {
		title : '',
		textDescription : '',
		contentDescription : '',
		state : 'ACTIVE',
		basicNeedType : 'DEMAND',
		tags : [],
		startTime : '',
		endTime : '',
		wonNode : '',
		uniqueKey : md5(userService.getUserName() + new Date().getTime())
	};

	$scope.onClickMap = function($event, $params) {
		if (this.marker == null) {
			this.marker = new google.maps.Marker({
				position : $params[0].latLng,
				map : this.myMap
			});
		} else {
			this.marker.setPosition($params[0].latLng);
		}
		$scope.need.latitude = $params[0].latLng.lb;
		$scope.need.longitude = $params[0].latLng.mb;
	}

	$scope.addTag = function() {
		var tags = $scope.need.tags;
		var tagName = $("#inputTagName").val();
		if(tags.indexOf(tagName) == -1) {
			$scope.need.tags.push(tagName);
		}
		$("#inputTagName").val('');
	}

	$scope.removeTag = function (tagName) {
		$scope.need.tags.splice($scope.need.tags.indexOf(tagName),1);
	}


	$scope.save = function () {
		needService.save($scope.need);
		$location.path("/create-need");
	}

	$scope.cancel = function () {
		$location.path("/");
	}


});

