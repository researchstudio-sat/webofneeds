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

angular.module('won.owner').controller('CreateNeedCtrlNew', function ($scope,  $location, $http, $routeParams, needService,applicationStateService, mapService, userService, utilService, wonService) {
    $scope.menuposition = $routeParams.menuposition;
    $scope.title = $routeParams.title;

    $scope.$on(won.EVENT.NEED_CREATED, onNeedCreated = function(event, eventData){

        $scope.needURI = eventData.needURI;
        applicationStateService.setCurrentNeedURI($scope.needURI);
        applicationStateService.addNeed(linkedDataService.getNeed($scope.needURI));
        $location.path("/private-link");

    });
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
        if($scope.menuposition == 0){
            return "DEMAND";
        }else if($scope.menuposition == 1){
            return "SUPPLY";
        } else if($scope.menuposition == 2){
            return "DO_TOGETHER";
        } else if($scope.menuposition == 3){
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

        /*connectMessage = new window.won.ConnectMessageBuilder(connectionJson)
            .addMessageGraph()
            .eventURI("2345432343")
            .hasSenderNeed(needJson_2["@context"]["@base"])
            .hasSenderNode("http://localhost:8080/won")
            .hasReceiverNeed(needJson["@context"]["@base"])
            .hasReceiverNode("http://localhost:8080/won")
            .sender()
            .receiver()
            .build();

        sender = messageService.sendMessage(connectMessage);  */
        // messageFactory.generateCreateNeedMessage($scope.need);
       // console.log("expandedJson: "+expandedJson);
        /*needService.saveDraft($scope.need, $scope.currentStep,userService.getUserName()).then(function(){
           $scope.successShow = true;

        }
        );       */
   // }
	/*$scope.save = function () {
        connectionRandomId = utilService.getRandomInt(1,9223372036854775807);
        connectionURI = needJson["@context"]["@base"]+$scope.connectionURIPath+"/"+connectionRandomId;
        connectionJson = new window.won.ConnectionBuilder()
            .setContext()
            .connectionURI(connectionURI)
            .build();
        wonService.connect(connectionJson, $scope.needURI2, $scope.needURI);    */
		/*needService.save($scope.need).then(function() {
			$scope.need = $scope.getCleanNeed();
			$scope.successShow = true;
		});    */
	//};
    $scope.saveDraft = function(){
        needService.saveDraft($scope.need, $scope.currentStep,userService.getUnescapeUserName()).then(function(){
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

	$scope.publish = function () {
        //TODO logic
		/*needService.save($scope.need).then(function() {
			$scope.need = $scope.getCleanNeed();
			$scope.successShow = true;
		}); */

        var needRandomId = utilService.getRandomInt(1,9223372036854775807);
        var needURI = $scope.wonNodeURI+$scope.needURIPath+"/"+needRandomId;
        console.log(needURI);

        // creating need object
        var needBuilderObject = new window.won.NeedBuilder().setContext();
        if ($scope.need.basicNeedType == 'DEMAND') {
            needBuilderObject.demand();
        } else if ($scope.need.basicNeedType == 'SUPPLY') {
            needBuilderObject.supply();
        } else if ($scope.need.basicNeedType == 'DO_TOGETHER') {
            needBuilderObject.doTogether();
        } else {
            needBuilderObject.critique();
        }

        needBuilderObject.title($scope.need.title)
            .needURI(needURI)
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

        // building need as JSON object
        var needJson = needBuilderObject.build();

        //console.log(needJson);
        var newNeedUriPromise = wonService.createNeed(needJson);
        //console.log('promised uri: ' + newNeedUriPromise);

        //$scope.need = $scope.getCleanNeed();      TODO decide what to do
        $scope.successShow = true;
	};


	$scope.cancel = function () {
		$location.path("/");
	};

    $scope.validatePostForm = function() {
        return ($scope.need.basicNeedType != 'undefined' && $scope.need.basicNeedType != null && $scope.need.basicNeedType != '') &&
            ($scope.need.title != 'undefined' && $scope.need.title != null && $scope.need.title != '') &&
            ($scope.need.textDescription != 'undefined' && $scope.need.textDescription != null && $scope.need.textDescription != '') &&
            ($scope.need.tags != 'undefined' &&  $scope.need.tags != null &&  $scope.need.tags != '');
    }

    $scope.validateDateTimeRange = function() {
        // check date values
        if (($scope.need.startDate == '' && $scope.need.endDate != '') ||
            ($scope.need.startDate != '' && $scope.need.endDate == '')) {
            // date value is missing
            return false
        }

        // check time values
        if (($scope.need.startTime == '' && $scope.need.endTime != '') ||
            ($scope.need.startTime != '' && $scope.need.endTime == '')) {
            // time value is missing
            return false;
        }

        // check datetime values
        if ($scope.need.startDate == '' && $scope.need.endDate == '' && $scope.need.startTime == '' && $scope.need.endTime == '') {
            return true;
        } else if ($scope.need.startDate != '' && $scope.need.endDate != '' && $scope.need.startTime != '' && $scope.need.endTime != '') {
            return true;
        } else {
            // date specified but not time or vice versa
            return false;
        }
    }

    $scope.skipToPreviewButtonDisabled = function() {
        if ($scope.currentStep == 1) {
            return !$scope.validatePostForm();
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
    $scope.menuposition = $routeParams.menuposition;


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
        if($scope.menuposition == 0) return "Add photos of similar things or sketches to give people a better idea what you have in mind."
        if($scope.menuposition == 1) return "Add photos or sketches to give people a better idea what you're offering.";
        if($scope.menuposition == 2) return "If you want you can add an image or photo here to illustrate the activity.";
        if($scope.menuposition == 3) return "Add a photo, sketch (or screenshot) of the problem you want to point out.";
    }
});


