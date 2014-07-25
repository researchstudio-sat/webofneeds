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

angular.module('won.owner').controller('CreateNeedCtrlNew', function ($scope,  $location, $http, $routeParams, needService, mapService, userService) {

	$scope.uploadOptions = {
		maxFileSize:5000000,
		acceptFileTypes:/(\.|\/)(gif|jpe?g|png)$/i
	};

    $scope.descriptionPlaceholder = "And now with details!" +
        "(By the way, there's specialised textboxes for things like pickup adress or time of availability";

    $scope.currentStep = 1;
    $scope.numberOfSteps = 4;
    $scope.toJump = 0;
	$scope.successShow = false;

    $scope.previousButton = false;
    $scope.saveButton = false;
    $scope.saveDraftButton = false;
    $scope.nextButton = false;
    $scope.previewButton = false;
    $scope.collapsed = false;

    $scope.setShowButtons = function(step){
        if(step == 1){
            $scope.previousButton = false;
            $scope.saveDraftButton = false;
            $scope.nextButton = false;
            $scope.previewButton = false;
            $scope.saveButton = false;
        }else if(step == 2  ){
            $scope.previousButton = true;
            $scope.saveDraftButton = true;
            $scope.nextButton = true;
            $scope.previewButton = true;
            $scope.saveButton = true;
        } else if(step == 3){
            if($scope.collapsed == true){
                $scope.previousButton = false;
                $scope.saveDraftButton = true;
                $scope.nextButton = false;
                $scope.previewButton = true;
                $scope.saveButton = true;

            } else {
                $scope.previousButton = true;
                $scope.saveDraftButton = true;
                $scope.nextButton = false;
                $scope.previewButton = true;
                $scope.saveButton = true;
            }
        }
    }
    $scope.needType = function($routeParams){
        if($routeParams.needType == "want"){
            return "DEMAND";
        }else if($routeParams.needType == "offer"){
            return "SUPPLY";
        } else if($routeParams.needType == "activity"){
            return "DO_TOGETHER";
        } else if($routeParams.needType == "critique"){
            return "CRITIQUE";
        }
    }
	$scope.marker = null;

    $scope.getMapOptions = function(){

        return {
            center:mapService.getGeolocation(),
            zoom:15,
            mapTypeId:google.maps.MapTypeId.ROADMAP
        };
    }
    $scope.mapOptions = $scope.getMapOptions();

    $scope.showPublic = function(num){
        if(num==$scope.currentStep){
            return true;
        }else{
            return false;
        }

    }

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
	};

	$scope.need = $scope.getCleanNeed();
    $scope.need.basicNeedType = $scope.needType($routeParams);
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
	};

	$scope.addTag = function() {
		var tags = $scope.need.tags;
		var tagName = $("#inputTagName").val();
		if(tags.indexOf(tagName) == -1) {
			$scope.need.tags.push(tagName);
		}
		$("#inputTagName").val('');
	};

	$scope.removeTag = function (tagName) {
		$scope.need.tags.splice($scope.need.tags.indexOf(tagName),1);
	};

    $scope.nextStep = function(){
        if($scope.currentStep <= $scope.numberOfSteps) {

            $scope.currentStep ++;
            $scope.successShow = false;
            $scope.setShowButtons($scope.currentStep);
        }

    }
    $scope.previousStep = function(){
        if($scope.currentStep >=1) {

            $scope.currentStep --;
            $scope.successShow = false;
            $scope.setShowButtons($scope.currentStep);
        }

    }
    $scope.jumpToStep = function(num){
        console.log(num);
        if(num<=$scope.numberOfSteps){
            $scope.currentStep = num;
            $scope.successShow = false;
            $scope.setShowButtons($scope.currentStep);

        }

    }

    $scope.saveDraft = function(){
        needService.saveDraft($scope.need, $scope.currentStep,userService.getUserName()).then(function(){
           $scope.successShow = true;

        });
    }
	$scope.save = function () {
		needService.save($scope.need).then(function() {
			$scope.need = $scope.getCleanNeed();
			$scope.successShow = true;
		});
	};


	$scope.cancel = function () {
		$location.path("/");
	};




});
angular.module('won.owner').directive('wonProgressTracker',function factory(){
    return {
        restrict: 'AE',
        templateUrl : "app/create-need/progress-tracker.html",
        scope : {
            numberOfSteps : '=numberOfSteps',
            currentStep : '=currentStep',
            jumpToStep : '&'
        } ,
        controller : function($scope){
            $scope.processSteps = {firstStep : false,
                                     secondStep: false,
                                     thirdStep: false,
                                     fourthStep: false};

            $scope.setFlagForCurrentStep = function(){
                if(currentStep == 1){
                    $scope.processSteps.firstStep = true;
                }else if(currentStep == 2){
                    $scope.processSteps.secondStep = true;
                }else if(currentStep == 3) {
                    $scope.processSteps.thirdStep = true;
                }else if(currentStep == 4){
                    $scope.processSteps.fourthStep = true;
                }
            };
            $scope.showPublic = function(num) {
                if($scope.currentStep != num){
                    return false;
                }else if($scope.currentStep == num){
                    return true;
                }
            };
            $scope.increaseStep = function(){
                $scope.currentStep++;
            }


        } ,
        link: function(scope, element, attrs){
            console.log("Progress Tracker");
        }
    }
})
angular.module('won.owner').directive('wonGallery', function factory() {
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
		controller : function($scope, $location) {
			$scope.selectedPhoto = 0;
			$scope.getCleanPhotos = function() {
				return [
					{uri:''},
					{uri:''},
					{uri:''}
				];
			};
			$scope.photos = $scope.getCleanPhotos();

			$scope.onClickPhoto = function(num) {
				$scope.selectedPhoto = num;
				console.log($scope.selectedPhoto);
			};
			$scope.$on('fileuploadsubmit', function (e, data) {
				var filename = data.files[0].name;
				$scope.lastExtension =  extension = filename.substr(filename.lastIndexOf(".") , filename.lenght);
			});

			$scope.$watch('need', function (newVal, oldVal) {
				if(newVal.binaryFolder != oldVal.binaryFolder) {
					$scope.photos = $scope.getCleanPhotos();
				}
			});

			$scope.$on('file uploadstop', function (e, data) {
				var absPath = $location.absUrl();
				var ownerPath = absPath.substr(0,absPath.indexOf('#'));
				$scope.photos[$scope.selectedPhoto].uri = ownerPath + 'rest/needphoto/' + $scope.need.binaryFolder + "/" + $scope.selectedPhoto + $scope.lastExtension + '/';
				$scope.need.images = [];
				angular.forEach($scope.photos, function(photo) {
					if(photo.uri) {
						$scope.need.images.push(angular.copy(photo));
					}
				}, $scope);
			});
            $scope.currentStep = 1;

            $scope.onClickNeedType = function(currentStep) {
                $scope.currentStep = $scope.currentStep+1;
            };
		}
	};
});
angular.module('won.owner').controller('AdditionalInfoCtrl', function ($scope,  $location, $http, $routeParams, needService, mapService, userService){
  $scope.imageInputFieldCollapsed = true;
  $scope.locationInputFieldCollapsed = true;
  $scope.timeInputFieldCollapsed = true;

  $scope.imageCollapseClick = function(){
        $scope.imageInputFieldCollapsed = !$scope.imageInputFieldCollapsed;
    };

    $scope.locationCollapseClick = function(){
        $scope.locationInputFieldCollapsed = !$scope.locationInputFieldCollapsed;
    };
});

