//owner.home.controller
createNeedModule = angular.module('owner.createneed', ['ui.map', 'ui.bootstrap.buttons', 'owner.service.need', 'blueimp.fileupload']);

createNeedModule.controller('CreateNeedCtrl', function ($scope, $location, $http, needService, userService) {
	$scope.mapOptions = {
		center : new google.maps.LatLng(35.784, -78.670),
		zoom : 15,
		mapTypeId:google.maps.MapTypeId.ROADMAP
	};

	$scope.uploadOptions = {
		maxFileSize:5000000,
		acceptFileTypes:/(\.|\/)(gif|jpe?g|png)$/i
	};

	$scope.succesShow = false;

	$scope.marker = null;

	$scope.getCleanNeed = function() {
		return {
			title:'',
			textDescription:'',
			contentDescription:'',
			state:'ACTIVE',
			basicNeedType:'DEMAND',
			tags:[],
			startTime:'',
			endTime:'',
			wonNode:'',
			binaryFolder:md5((new Date().getTime() + Math.random(1)).toString())
		};
	}

	$scope.need = $scope.getCleanNeed();

	$scope.onClickMap = function($event, $params) {
		if (this.marker == null) {
			this.marker = new google.maps.Marker({
				position : $params[0].latLng,
				map : this.myMap
			});
		} else {
			this.marker.setPosition($params[0].latLng);
		}
		$scope.need.latitude = $params[0].latLng.lat();
		$scope.need.longitude = $params[0].latLng.lng();
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
		needService.save($scope.need).then(function() {
			$scope.need = $scope.getCleanNeed();
			$scope.succesShow = true;
		});
	}

	$scope.cancel = function () {
		$location.path("/");
	}


});

createNeedModule.directive('wonGallery', function factory() {
	return {
		restrict : 'A',
		templateUrl : "app/create-need/won-gallery.html",
		scope : {
			need : '=need'
		},
		link : function (scope, element, attrs) {

			$('#photo').change(function () {
				angular.element("#photo-form").scope().submit();
			});
		},
		controller : function($scope) {
			$scope.selectedPhoto = 0;
			$scope.getCleanPhotos = function() {
				return [
					{url:''},
					{url:''},
					{url:''}
				];
			}
			$scope.photos = $scope.getCleanPhotos();

			$scope.onClickPhoto = function(num) {
				$scope.selectedPhoto = num;
				console.log($scope.selectedPhoto);
			}
			$scope.$on('fileuploadsubmit', function (e, data) {
				var filename = data.files[0].name;
				$scope.lastExtension =  extension = filename.substr(filename.lastIndexOf(".") , filename.lenght);
			});

			$scope.$watch('need', function (newVal, oldVal) {
				if(newVal.binaryFolder != oldVal.binaryFolder) {
					$scope.photos = $scope.getCleanPhotos();
				}
			});

			$scope.$on('fileuploadstop', function (e, data) {
				$scope.photos[$scope.selectedPhoto].url = 'http://localhost:8080/owner/rest/needphoto/' + $scope.need.binaryFolder + "/" + $scope.selectedPhoto + $scope.lastExtension + '/';
			});
		}
	};
});