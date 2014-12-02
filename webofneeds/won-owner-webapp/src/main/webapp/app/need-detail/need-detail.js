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

angular.module('won.owner').controller('NeedDetailCtrl', function ($scope, $log,$location, $http, $routeParams, needService,mapService) {
	$scope.mapOptions = {
		center: mapService.getInitialLocation(),//new google.maps.LatLng(35.784, -78.670),
		zoom:15,
		mapTypeId:google.maps.MapTypeId.ROADMAP
	};

	$scope.need = {
		title:'',
		textDescription:'',
		contentDescription:'',
		state:'ACTIVE',
		basicNeedType:'DEMAND',
		tags:[],
		startTime:'',
		endTime:'',
		wonNode:''
	};

	needService.getNeedById($routeParams.needId).then(function (response) {
		data = response.data;
		data.tags = data.tags.split(',');
		$.extend($scope.need, data);
		if(data.latitude && data.longitude) {
			var latLng = new google.maps.LatLng(data.latitude, data.longitude);
			$scope.myMap.setCenter(latLng);
			this.marker = new google.maps.Marker({
				position :latLng,
				map : $scope.myMap
			});
		}
        $log.debug(response);
	});

	$scope.toogleActive = function () {
	}

	$scope.back = function () {
        $location.url("/need-list");
	}

});
