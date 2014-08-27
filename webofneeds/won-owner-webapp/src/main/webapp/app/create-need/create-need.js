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

angular.module('won.owner').controller('CreateNeedCtrlNew', function ($scope,  $location, $http, $routeParams, needService, mapService, userService, $rootScope) {
    /*Text constants for new Need form*/
    /*$rootScope.createNewPost = {

        step1Texts :{
            typeText : "Type *",
            titleText : "Title *",
            typeChangeText : "(change)",
            titlePlaceholderText : "Roughly, what's it about?",
            descriptionText : "Description *",
            descriptionPlaceholderText : "And now with details! (By the way, there's specialised textboxes for things like pickup adress or time of availability",
            tagsText : "Tags",
            tagsPlaceholderText : "e.g. furniture, electronics, for children,...",
            kindOfTypes : {
                want : {title : "Want", comment : "I'm looking for..."},
                offer : {title : "Offer", comment : "I'm offering..."},
                activity : {title : "Activity", comment : "Looking for people to..."},
                critique : {title : "Critique", comment : "Let's do something about..."}
            }
        },
        step2Texts :{
            comment1 : "Filling out the following categories makes it easier for people, who need what you have to offer, to find this post and start messaging with you.",
            comment2 : "You can add ",
            comment3 : "photos or sketches",
            comment4 : " of the thing you're offering, or specify ",
            comment5 : "where",
            comment6 : "and ",
            comment7 : "when",
            comment8 : "it will be available.",
            images : {
                title : "Images",
                commentWant : "Add photos of similar things or sketches to give people a better idea what you have in mind.",
                commentOffer : "Add photos or sketches to give people a better idea what you're offering.",
                commentActivity : "If you want you can add an image or photo here to illustrate the activity.",
                commentCritique : "Add a photo, sketch (or screenshot) of the problem you want to point out."
            }

        }
    }          */
    /*Block for working with checking another post type */
    $scope.getCurrentTypeOfOffer = function(){
        if($rootScope.iPost.menuposition == 0) return "Want - I'm looking for...";
        if($rootScope.iPost.menuposition == 1) return "Offer - I'm offering...";
        if($rootScope.iPost.menuposition == 2) return "Together - Looking for people to...";
        if($rootScope.iPost.menuposition == 3) return "Change - Let's do something about...";
    }

    $scope.showPublicChangeTypeOfNeed = false;
    $scope.clickOnChangeTypeOfNeed = function(){
        $scope.showPublicChangeTypeOfNeed = !$scope.showPublicChangeTypeOfNeed;
        $('#changePostMenuItem' + $rootScope.iPost.menuposition).addClass('active');
    }

    $scope.onClickChangePostMenuItem = function(item) {
        if(item > -1){
            if($rootScope.iPost.menuposition > -1){
                $('#changePostMenuItem' + $rootScope.iPost.menuposition).removeClass('active');
            }
            $rootScope.iPost.menuposition = item;
            $('#changePostMenuItem' + $rootScope.iPost.menuposition).addClass('active');
            $scope.showPublicChangeTypeOfNeed = false;
        }
    }

    $scope.tooltipText = 'Required so other people can find the post.';

	$scope.uploadOptions = {
		maxFileSize:5000000,
		acceptFileTypes:/(\.|\/)(gif|jpe?g|png)$/i
	};

    $scope.currentStep = 1;
    $scope.numberOfSteps = 3;
    $scope.toJump = 0;
	$scope.successShow = false;

    $scope.previousButton = false;
    $scope.saveDraftButton = true;
    $scope.nextButton = true;
    $scope.previewButton = true;
    $scope.publishButton = false;
    $scope.collapsed = false;

    $scope.setShowButtons = function(step){
        /*if(step == 1){
            $scope.previousButton = false;
            $scope.saveDraftButton = false;
            $scope.nextButton = false;
            $scope.previewButton = false;
        }else*/ if(step == 1){//2  ){
            $scope.previousButton = false;
            $scope.saveDraftButton = true;
            $scope.nextButton = true;
            $scope.previewButton = true;
            $scope.publishButton = false;
        } else if(step == 2){//3){
            if($scope.collapsed == true){
                $scope.previousButton = true;
                $scope.saveDraftButton = true;
                $scope.nextButton = false;
                $scope.previewButton = false;
                $scope.publishButton = true;
            } else {
                $scope.previousButton = true;
                $scope.saveDraftButton = true;
                $scope.nextButton = false;
                $scope.previewButton = true;
                $scope.publishButton = false;
            }
        }else if(step == 3){
            $scope.previousButton = true;
            $scope.saveDraftButton = true;
            $scope.nextButton = false;
            $scope.previewButton = false;
            $scope.publishButton = true;
        }

    }
    /*$scope.needType = function($routeParams){
        if($routeParams.needType == "want"){
            return "DEMAND";
        }else if($routeParams.needType == "offer"){
            return "SUPPLY";
        } else if($routeParams.needType == "activity"){
            return "DO_TOGETHER";
        } else if($routeParams.needType == "critique"){
            return "CRITIQUE";
        }
    }       */

    $scope.needType = function(){
        if($rootScope.iPost.menuposition == 0){
            return "DEMAND";
        }else if($rootScope.iPost.menuposition == 1){
            return "SUPPLY";
        } else if($rootScope.iPost.menuposition == 2){
            return "DO_TOGETHER";
        } else if($rootScope.iPost.menuposition == 3){
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
			title               :$rootScope.iPost.title,
			textDescription     :'',
			contentDescription  :'',
			state               : 'ACTIVE',
			basicNeedType       : $scope.needType(),
			tags                :[],
			startTime           :'',
			endTime             :'',
			wonNode             :'',
			binaryFolder        :md5((new Date().getTime() + Math.random(1)).toString())
		};
	};

	$scope.need = $scope.getCleanNeed();
    $scope.need.basicNeedType = $scope.needType();

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

    /*$scope.saveDraft = function () {
      //  needService.saveDraft($scope.need);
        if($scope.currentStep <= $scope.numberOfSteps) {
            $scope.currentStep ++;
        }
    };  */
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
        needService.saveDraft($scope.need, $scope.currentStep,userService.getUnescapeUserName()).then(function(){
           $scope.successShow = true;

        });
    }
	$scope.publish = function () {
        //TODO logic
		/*needService.save($scope.need).then(function() {
			$scope.need = $scope.getCleanNeed();
			$scope.successShow = true;
		}); */
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
                }/*else if(currentStep == 4){
                    $scope.processSteps.fourthStep = true;
                } */
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
angular.module('won.owner').controller('AdditionalInfoCtrl', function ($scope,  $location, $http, $routeParams, needService, mapService, userService, $rootScope){
    $scope.imageInputFieldCollapsed = true;
    $scope.locationInputFieldCollapsed = true;
    $scope.timeInputFieldCollapsed = true;

    $scope.imageCollapseClick = function(){
        $scope.imageInputFieldCollapsed = !$scope.imageInputFieldCollapsed;
    };

    $scope.locationCollapseClick = function(){
        $scope.locationInputFieldCollapsed = !$scope.locationInputFieldCollapsed;
    };

    $scope.timeCollapseClick = function(){
        $scope.timeInputFieldCollapsed = !$scope.timeInputFieldCollapsed;
    };

    $scope.getImagesComment = function(){
        if($rootScope.iPost.menuposition == 0) return "Add photos of similar things or sketches to give people a better idea what you have in mind."
        if($rootScope.iPost.menuposition == 1) return "Add photos or sketches to give people a better idea what you're offering.";
        if($rootScope.iPost.menuposition == 2) return "If you want you can add an image or photo here to illustrate the activity.";
        if($rootScope.iPost.menuposition == 3) return "Add a photo, sketch (or screenshot) of the problem you want to point out.";
    }
});


