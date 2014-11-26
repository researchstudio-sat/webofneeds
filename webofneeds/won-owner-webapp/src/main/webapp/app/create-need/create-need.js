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

angular.module('won.owner').controller('CreateNeedCtrlNew', function ($scope, $timeout, $location, $http, $routeParams, needService,applicationStateService, mapService, userService, utilService, wonService) {
    $scope.currentStep = $routeParams.step;
    $scope.menuposition = $routeParams.menuposition;
    $scope.title = $routeParams.title;
    // we pass draft uri in query param "draft". Care should be taken to remove this param when redirecting with location.path(...).search("draft", null)
    $scope.draftURI = $routeParams.draft;

    $scope.$on(won.EVENT.NEED_CREATED, onNeedCreated = function(event, eventData){
        $scope.needURI = eventData.needURI;
        applicationStateService.setCurrentNeedURI($scope.needURI);
        $location.path("/private-link").search("draft", null);
    });

    /*Block for working with checking another post type */
    $scope.getCurrentTypeOfOffer = function(){
        if($scope.menuposition == 0) return "Want - I'm looking for...";
        if($scope.menuposition == 1) return "Offer - I'm offering...";
        if($scope.menuposition == 2) return "Together - Looking for people to...";
        if($scope.menuposition == 3) return "Change - Let's do something about...";
    }

    $scope.showPublicChangeTypeOfNeed = false;
    $scope.clickOnChangeTypeOfNeed = function(){
        $scope.showPublicChangeTypeOfNeed = !$scope.showPublicChangeTypeOfNeed;
        $('#changePostMenuItem' + $scope.menuposition).addClass('active');
    }

    $scope.onClickChangePostMenuItem = function(item) {
        if(item > -1){
            if($scope.menuposition > -1){
                $('#changePostMenuItem' + $scope.menuposition).removeClass('active');
            }
            $scope.menuposition = item;
            $('#changePostMenuItem' + $scope.menuposition).addClass('active');
            $scope.showPublicChangeTypeOfNeed = false;
            $scope.menuposition = item;
            $scope.need.basicNeedType = $scope.needType();
        }
    }

    $scope.tooltipText = 'Required so other people can find the post.';

    var titlePlaceholder = [
        'Roughly, what are you looking for?',
        'Roughly, what are you offering?',
        'Roughly, what are you planning for which you need people?',
        'Roughly, what are you pointing out and want to change?'];

    $scope.getTitlePlaceholder = function() {
        return titlePlaceholder[$scope.menuposition];
    }

    var descriptionPlaceholder = [
        'And now with details! (By the way, there\'s specialised textboxes for specifying a pickup adress and time of availability)',
        'As demand, but "delivery adress" instead of "pickup adress"',
        'As demand but ending in "...for specifying an event location and time"',
        '...for specifying when and where the thing you want to change occurred.'];

    $scope.getDescriptionPlaceholder = function() {
        return descriptionPlaceholder[$scope.menuposition];
    }

    var tagsPlaceholder = [
    'Shoes, Entertainment, Suited For Small Children, ... ',
    'As demand',
    'Soccer, Party, Discussion Group, Food Coop ...',
    'FML, Clean Park Initiative, Recycling, Occupy Wallstreet, Privacy, ... '];

    $scope.getTagsPlaceholder = function() {
        return tagsPlaceholder[$scope.menuposition];
    }


	$scope.uploadOptions = {
		maxFileSize:5000000,
		acceptFileTypes:/(\.|\/)(gif|jpe?g|png)$/i
	};

    $scope.numberOfSteps = 3;
    $scope.toJump = 0;
	$scope.successShow = false;
    $scope.collapsed = false;

    $scope.setShowButtons = function(step){
        /*if(step == 1){
            $scope.previousButton = false;
            $scope.saveDraftButton = false;
            $scope.nextButton = false;
            $scope.previewButton = false;
        }else*/ if(step == 1){//2  ){
            $scope.previousButton = false;

            $scope.saveDraftButton = userService.isAuth();
            $scope.nextButton = true;
            $scope.previewButton = true;
            $scope.publishButton = false;
        } else if(step == 2){//3){
            if($scope.collapsed == true){
                $scope.previousButton = true;
                $scope.saveDraftButton = userService.isAuth();
                $scope.nextButton = false;
                $scope.previewButton = false;
                $scope.publishButton = true;
            } else {
                $scope.previousButton = true;
                $scope.saveDraftButton = userService.isAuth();
                $scope.nextButton = false;
                $scope.previewButton = true;
                $scope.publishButton = false;
            }
        }else if(step == 3){
            $scope.previousButton = true;
            $scope.saveDraftButton = userService.isAuth();
            $scope.nextButton = false;
            $scope.previewButton = false;
            $scope.publishButton = true;
        } else { // default
            $scope.previousButton = false;
            $scope.saveDraftButton = userService.isAuth();
            $scope.nextButton = true;
            $scope.previewButton = true;
            $scope.publishButton = false;
        }

    }
    $scope.setShowButtons($scope.currentStep);

    $scope.needType = function(){
        if($scope.menuposition == 0){
            return won.WON.BasicNeedTypeDemand;
        }else if($scope.menuposition == 1){
            return won.WON.BasicNeedTypeSupply;
        } else if($scope.menuposition == 2){
            return won.WON.BasicNeedTypeDotogether;
        } else if($scope.menuposition == 3){
            return won.WON.BasicNeedTypeCritique;
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
    $scope.getNeed = function() {
        if ($scope.draftURI == null) {
            return $scope.getCleanNeed();
        } else {
            return  applicationStateService.getDraft($scope.draftURI);
        }
    }

    $scope.getCleanNeed = function() {
        return {
            title               :$scope.title,
            needURI             :'',
            textDescription     :'',
            contentDescription  :'',
            state               : 'ACTIVE',
            basicNeedType       : $scope.needType(),
            tags                :'',
            latitude            :'',
            longitude           :'',
            startDate           :'',
            startTime           :'',
            endDate             :'',
            endTime             :'',
            recursIn            :'P0D',
            recurTimes          :0,         // not used for now, 0 is default value
            wonNode             :'',
            binaryFolder        :md5((new Date().getTime() + Math.random(1)).toString())
        };
    };

	$scope.need = $scope.getNeed();
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
         //   var newPath = '/create-need/'+$scope.currentStep+'/'+$scope.iPost.menuposition+'/'+$scope.need.title;
         //   $location.path(newPath);

        }

    }
    $scope.saveDraft = function(){
        var draftBuilderObject = new window.won.DraftBuilder().setContext();
        draftBuilderObject.setCurrentStep($scope.currentStep);
        draftBuilderObject.setCurrentMenuposition($scope.menuposition);
        draftBuilderObject.setDraftObject($scope.need);
        draftBuilderObject.setLastSavedTimestamp(new Date().getTime());

        if ($scope.need.basicNeedType == won.WON.BasicNeedTypeDemand) {
            draftBuilderObject.demand();
        } else if ($scope.need.basicNeedType == won.WON.BasicNeedTypeSupply) {
            draftBuilderObject.supply();
        } else if ($scope.need.basicNeedType == won.WON.BasicNeedTypeDotogether) {
            draftBuilderObject.doTogether();
        } else {
            draftBuilderObject.critique();
        }

        draftBuilderObject.title($scope.need.title)
            .ownerFacet()               // mandatory
            .description($scope.need.textDescription)
            .hasTag($scope.need.tags)
            .hasContentDescription()    // mandatory
            //.hasPriceSpecification("EUR",5.0,10.0)
            .active()                   // mandatory: active or inactive

        if (hasLocationSpecification($scope.need)) {
            // never called now, because location is not known for now   hasLocationSpecification(48.218748, 16.360783)
            draftBuilderObject.hasLocationSpecification($scope.need.latitude, $scope.need.longitude);
        }

        if (hasTimeSpecification($scope.need)) {
            draftBuilderObject.hasTimeSpecification(createISODateTimeString($scope.need.startDate, $scope.need.startTime), createISODateTimeString($scope.need.endDate, $scope.need.endTime), $scope.need.recursIn != 'P0D' ? true : false, $scope.need.recursIn, $scope.need.recurTimes);
        }

        // building need as JSON object
        var draftJson = draftBuilderObject.build();
        if ($scope.need.needURI == null || 0 === $scope.need.needURI.length) {
            $scope.need.needURI =  wonService.getDefaultWonNodeUri() + "/need/" + utilService.getRandomInt(1,9223372036854775807);
            draftJson['@graph'][0]['@graph'][0]['@id'] = $scope.need.needURI;
        }
        var createDraftObject = {"draftURI":$scope.need.needURI,"draft":JSON.stringify(draftJson)};

        needService.saveDraft(createDraftObject).then(function(draftURI){
           $scope.successShow = true;

        });
    }

    // TODO does not update models
    /*
    $('#start_date').datepicker({
        format:'dd.mm.yyyy',
        autoclose: true
    });

    $('#end_date').datepicker({
        format:'dd.mm.yyyy',
        autoclose: true
    }); */

    function createISODateTimeString(date, time) {
        var d = date.split('-');
        var t = time.split(':');
        var datetime = new Date();
        datetime.setFullYear(d[0]);
        datetime.setMonth(d[1] - 1);
        datetime.setDate(d[2]);
        datetime.setHours(t[0]);
        datetime.setMinutes(t[1]);
        //datetime.setSeconds(0);
        //datetime.setMilliseconds(0);
        return datetime.toISOString();
    }

    function hasTimeSpecification(need) {
        return need.startDate != '' && need.startTime != '' && need.endDate != '' && need.endTime != '';
    }

    function hasLocationSpecification(need) {
        return need.latitude != '' && need.longitude != null;
    }

    function hasUri(need) {
        return need.needURI != '' && need.needURI != null;
    }
    var lock = false;
	$scope.publish = function () {
        if(lock== false){
            lock = true;
            // creating need object
            var needBuilderObject = new window.won.NeedBuilder().setContext();
            if ($scope.need.basicNeedType == won.WON.BasicNeedTypeDemand) {
                needBuilderObject.demand();
            } else if ($scope.need.basicNeedType == won.WON.BasicNeedTypeSupply) {
                needBuilderObject.supply();
            } else if ($scope.need.basicNeedType ==  won.WON.BasicNeedTypeDotogether) {
                needBuilderObject.doTogether();
            } else {
                needBuilderObject.critique();
            }

            needBuilderObject.title($scope.need.title)
                .ownerFacet()               // mandatory
                .description($scope.need.textDescription)
                .hasTag($scope.need.tags)
                .hasContentDescription()    // mandatory
                //.hasPriceSpecification("EUR",5.0,10.0)
                .active()                   // mandatory: active or inactive

            if (hasLocationSpecification($scope.need)) {
                // never called now, because location is not known for now   hasLocationSpecification(48.218748, 16.360783)
                needBuilderObject.hasLocationSpecification($scope.need.latitude, $scope.need.longitude);
            }

            if (hasTimeSpecification($scope.need)) {
                needBuilderObject.hasTimeSpecification(createISODateTimeString($scope.need.startDate, $scope.need.startTime), createISODateTimeString($scope.need.endDate, $scope.need.endTime), $scope.need.recursIn != 'P0D' ? true : false, $scope.need.recursIn, $scope.need.recurTimes);
            }

            if (hasUri($scope.need)) {
                needBuilderObject.uri($scope.need.needURI);
            }

            // building need as JSON object
            var needJson = needBuilderObject.build();

            //console.log(needJson);
            var newNeedUriPromise = wonService.createNeed(needJson);

            // TODO: should the draft removing part be changed to run only on success from newNeedUriPromise?
            if ($scope.draftURI != null) {
                userService.removeDraft($scope.draftURI);
                $scope.draftURI = null;
            }
            //console.log('promised uri: ' + newNeedUriPromise);

            //$scope.need = $scope.getCleanNeed();      TODO decide what to do
            $scope.successShow = true;
            newNeedUriPromise.then(function(){
                lock=false;
            });
        }

	};

	$scope.cancel = function () {
		$location.path("/");
	};

    $scope.validatePostForm = function() {
        var result =  ($scope.need.basicNeedType != 'undefined' && $scope.need.basicNeedType != null && $scope.need.basicNeedType != '') &&
            ($scope.need.title != 'undefined' && $scope.need.title != null && $scope.need.title != '') &&
            ($scope.need.textDescription != 'undefined' && $scope.need.textDescription != null && $scope.need.textDescription != '') &&
            ($scope.need.tags != 'undefined' &&  $scope.need.tags != null &&  $scope.need.tags != '');

        $scope.$broadcast('validatePostFormEvent', result);
        return result;
    }

    $scope.validateDateTimeRange = function() {
        // check date values
        if (($scope.need.startDate == '' && $scope.need.endDate != '') ||
            ($scope.need.startDate != '' && $scope.need.endDate == '')) {
            // date value is missing
            $scope.$broadcast('validateDateTimeRangeEvent', false);
            return false
        }

        // check time values
        if (($scope.need.startTime == '' && $scope.need.endTime != '') ||
            ($scope.need.startTime != '' && $scope.need.endTime == '')) {
            // time value is missing
            $scope.$broadcast('validateDateTimeRangeEvent', false);
            return false;
        }

        // check datetime values
        if ($scope.need.startDate == '' && $scope.need.endDate == '' && $scope.need.startTime == '' && $scope.need.endTime == '') {
            $scope.$broadcast('validateDateTimeRangeEvent', true);
            return true;
        } else if ($scope.need.startDate != '' && $scope.need.endDate != '' && $scope.need.startTime != '' && $scope.need.endTime != '') {
            $scope.$broadcast('validateDateTimeRangeEvent', true);
            return true;
        } else {
            // date specified but not time or vice versa
            $scope.$broadcast('validateDateTimeRangeEvent', false);
            return false;
        }
    }

    $scope.skipToPreviewButtonDisabled = function() {
        if ($scope.currentStep == 1) {
            return !$scope.validatePostForm() || !$scope.validateDateTimeRange();
        } else if ($scope.currentStep == 2) {
            return !$scope.validateDateTimeRange();
        }
    }

    $scope.allDay = false;

    $scope.clickOnAllDay = function() {
        $scope.allDay = !$scope.allDay;
        if ($scope.allDay) {
            $("#start_time").prop('type', 'text');
            $('#end_time').prop('type', 'text');

            $scope.need.startTime = '00:00';
            $scope.need.endTime = '23:59';

            //$("#start_time").val('12:00 AM');     // does not help, model has higher priority
            //$("#end_time").val('11:59 PM');
        } else {
            $("#start_time").prop('type', 'time');
            $('#end_time').prop('type', 'time');

            $scope.need.startTime = '';
            $scope.need.endTime = '';
        }
    }

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


            // states for menu items and event handling for form validation
            $scope.navStep1Disabled = '';
            $scope.navStep2Disabled = 'disabled';
            $scope.navStep3Disabled = 'disabled';

            $scope.$on('validatePostFormEvent', function(event, eventData) {
                if (eventData == true) {
                    $scope.navStep2Disabled = '';
                } else {
                    $scope.navStep2Disabled = 'disabled';
                }
            })

            $scope.$on('validateDateTimeRangeEvent', function(event, eventData) {
                if (eventData == true) {
                    $scope.navStep3Disabled = '';
                } else {
                    $scope.navStep3Disabled = 'disabled';
                }
            })
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
angular.module('won.owner').controller('AdditionalInfoCtrl', function ($scope,  $location, $http, needService, mapService, userService){
    $scope.imageInputFieldCollapsed = true;
    $scope.locationInputFieldCollapsed = true;
    $scope.timeInputFieldCollapsed = true;

    $scope.imageCollapseClick = function(){
        $scope.imageInputFieldCollapsed = !$scope.imageInputFieldCollapsed;
        if($scope.imageInputFieldCollapsed==false){
         /*   $location.hash('imagesInfoTitleWell');
            $anchorScroll();*/
        }
    };

    $scope.locationCollapseClick = function(){
        $scope.locationInputFieldCollapsed = !$scope.locationInputFieldCollapsed;
        if($scope.locationInputFieldCollapsed==false){

          /*  $location.hash('locationInfoTitleWell');
            $anchorScroll();  */
        }
    };

    $scope.timeCollapseClick = function(){
        $scope.timeInputFieldCollapsed = !$scope.timeInputFieldCollapsed;
        if($scope.timeInputFieldCollapsed==false){
         /*   $location.hash('timeInfoTitleWell');
            $anchorScroll();    */
        }
    };

    $scope.getImagesComment = function(){
        if($scope.menuposition == 0) return "Add photos of similar things or sketches to give people a better idea what you have in mind."
        if($scope.menuposition == 1) return "Add photos or sketches to give people a better idea what you're offering.";
        if($scope.menuposition == 2) return "If you want you can add an image or photo here to illustrate the activity.";
        if($scope.menuposition == 3) return "Add a photo, sketch (or screenshot) of the problem you want to point out.";
    }

    $scope.getLocationComment = function() {
        if($scope.menuposition == 0) return "Where should the thing be available? i.e. where would you pick it up or where should it be delivered to?"
        if($scope.menuposition == 1) return "Where\'s your offer available? i.e. where can people pick it up or where would you deliver it too?";
        if($scope.menuposition == 2) return "Where's the action happening?";
        if($scope.menuposition == 3) return "Where did the problem occur / where have things to be changed?";
    }

});


