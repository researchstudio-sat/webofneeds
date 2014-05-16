angular.module('won.owner').controller('NeedDetailCtrl', function ($scope, $location, $http, $routeParams, needService,mapService) {
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
		console.log(response);
	});

	$scope.toogleActive = function () {
	}

	$scope.back = function () {
		$location.path("/need-list");
	}

});
