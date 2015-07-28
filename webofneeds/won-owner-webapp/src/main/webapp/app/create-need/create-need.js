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

angular.module('won.owner').controller('CreateNeedCtrlNew', function
    ( $scope
    , $timeout
    , $location
    , $log
    , $http
    , $routeParams
    , $anchorScroll
    , needService
    , applicationStateService
    , userService
    , utilService
    , wonService
    , $q
    ) {
    $scope.currentStep = $routeParams.step;
    $scope.selectedType = $routeParams.selectedType;
    $scope.title = $routeParams.title;
    // we pass draft uri in query param "draft". Care should be taken to remove this param when redirecting with location.path(...).search("draft", null)
    $scope.draftURI = $routeParams.draft;

    $scope.$on(won.EVENT.NEED_CREATED, onNeedCreated = function (event, eventData) {
        $scope.needURI = eventData.needURI;
        applicationStateService.setCurrentNeedURI($scope.needURI);
        $location.url("/private-link");
    });

    $scope.showChangeType = false;
    $scope.clickOnChangeTypeOfNeed = function () {
        $scope.showChangeType = !$scope.showChangeType;
        $('#changePostMenuItem' + $scope.selectedType).addClass('active');
    }
    /*Block for working with checking another post type */
    var typeStrings = [
        "Want - I'm looking for...",
        "Offer - I'm offering...",
        "Together - Looking for people to...",
        "Change - Let's do something about..."]
    $scope.getCurrentTypeOfOffer = function () {
        if ($scope.selectedType == undefined || $scope.selectedType < 0
            || $scope.selectedType >= typeStrings.length)
            $scope.showChangeType = true;
        else
            return typeStrings[$scope.selectedType];
    }

    $scope.onClickChangePostMenuItem = function (item) {
        if (item > -1) {
            if ($scope.selectedType > -1) {
                $('#changePostMenuItem' + $scope.selectedType).removeClass('active');
            }
            $scope.selectedType = item;
            $('#changePostMenuItem' + $scope.selectedType).addClass('active');
            $scope.showChangeType = false;
            $scope.selectedType = item;
            $scope.need.basicNeedType = $scope.needType();
        }
    }

    var typeIcons = ["want", "offer", "todo", "change"].map(function (t) {
        return "/owner/images/type_posts/" + t + ".png";
    })

    $scope.currentTypeIcon = function () {
        return typeIcons[$scope.selectedType];
    }

    $scope.tooltipText = 'Required so other people can find the post.';

    var titlePlaceholder = [
        'Roughly, what are you looking for?',
        'Roughly, what are you offering?',
        'Roughly, what are you planning for which you need people?',
        'Roughly, what are you pointing out and want to change?'];

    $scope.getTitlePlaceholder = function () {
        return titlePlaceholder[$scope.selectedType];
    }

    var descriptionPlaceholder = [
        'And now with details! (By the way, there\'s specialised textboxes for specifying a pickup adress and time of availability)',
        'And now with details! (By the way, there\'s specialised textboxes for specifying a delivery adress and time of availability)',
        'And now with details! (By the way, there\'s specialised textboxes for specifying an event location and time)',
        'And now with details! (By the way, there\'s specialised textboxes for specifying when and where the thing you want to change occurred).'];

    $scope.getDescriptionPlaceholder = function () {
        return descriptionPlaceholder[$scope.selectedType];
    }

    var tagsPlaceholder = [
        'Shoes, Entertainment, For Children, ... ',
        'Shoes, Entertainment, For Children, ... ',
        'Soccer, Party, Discussion Group, Food Coop ...',
        'Clean Park Initiative, Recycling, Occupy, Privacy, FML, ... '];

    $scope.getTagsPlaceholder = function () {
        return tagsPlaceholder[$scope.selectedType];
    }

    $scope.uploadOptions = {
        maxFileSize: 5000000,
        acceptFileTypes: /(\.|\/)(gif|jpe?g|png)$/i
    };

    $scope.numberOfSteps = 3;
    $scope.toJump = 0;
    $scope.successShow = false;
    $scope.collapsed = false;

    $scope.setShowButtons = function (step) {
        /*if(step == 1){
         $scope.previousButton = false;
         $scope.saveDraftButton = false;
         $scope.nextButton = false;
         $scope.previewButton = false;
         }else*/
        if (step == 1) {//2  ){
            $scope.previousButton = false;

            $scope.saveDraftButton = userService.isAccountUser();
            $scope.nextButton = true;
            $scope.previewButton = true;
            $scope.publishButton = false;
        } else if (step == 2) {//3){
            if ($scope.collapsed == true) {
                $scope.previousButton = true;
                $scope.saveDraftButton = userService.isAccountUser();
                $scope.nextButton = false;
                $scope.previewButton = false;
                $scope.publishButton = true;
            } else {
                $scope.previousButton = true;
                $scope.saveDraftButton = userService.isAccountUser();
                $scope.nextButton = false;
                $scope.previewButton = true;
                $scope.publishButton = false;
            }
        } else if (step == 3) {
            $scope.previousButton = true;
            $scope.saveDraftButton = userService.isAccountUser();
            $scope.nextButton = false;
            $scope.previewButton = false;
            $scope.publishButton = true;
        } else { // default
            $scope.previousButton = false;
            $scope.saveDraftButton = userService.isAccountUser();
            $scope.nextButton = true;
            $scope.previewButton = true;
            $scope.publishButton = false;
        }

    };
    $scope.setShowButtons($scope.currentStep);

    $scope.needType = function () {
        if ($scope.selectedType == 0) {
            return won.WON.BasicNeedTypeDemand;
        } else if ($scope.selectedType == 1) {
            return won.WON.BasicNeedTypeSupply;
        } else if ($scope.selectedType == 2) {
            return won.WON.BasicNeedTypeDotogether;
        } else if ($scope.selectedType == 3) {
            return won.WON.BasicNeedTypeCritique;
        }
    }

    $scope.addressSelected = function(lat, lon, name) {
        $scope.need.latitude = lat;
        $scope.need.longitude = lon;
        $scope.need.name = name;
    };
    $scope.showPublic = function (num) {
        if (num == $scope.currentStep) {
            return true;
        } else {
            return false;
        }

    }
    $scope.getNeed = function () {
        if ($scope.draftURI == null) {
            return $scope.getCleanNeed();
        } else {
            var localDraft = applicationStateService.getDraft($scope.draftURI);
            if (localDraft == null) { // can happen if someone enters create-need-preview url without any of previous steps
                $scope.draftURI == null
                return $scope.getCleanNeed();
            } else {
                return localDraft;
            }
        }
    }

    $scope.getCleanNeed = function () {
        return {
            title: $scope.title,
            needURI: '',
            textDescription: '',
            contentDescription: '',
            state: 'ACTIVE',
            basicNeedType: $scope.needType(),
            tags: '',
            latitude: '',
            longitude: '',
            startDate: '',
            startTime: '',
            endDate: '',
            endTime: '',
            recursIn: 'P0D',
            recurTimes: 0,         // not used for now, 0 is default value
            wonNode: '',
            binaryFolder: md5((new Date().getTime() + Math.random(1)).toString())
        };
    };

    $scope.need = $scope.getNeed();
    $scope.need.basicNeedType = $scope.needType();


    // previewNeed object is intended for the input to detail preview directive
    // used in create-need page, because that directive that expects string
    // as tags value. Additionally,using previewNeed instead of need object avoids
    // constant calling of the detail preview directive functions due to every
    // change of need object during its construction
    $scope.getPreviewNeed = function () {
        var copy = angular.copy($scope.need);
        copy.tags = utilService.concatTags($scope.need.tags);
        return copy;
    }
    $scope.previewNeed = $scope.getPreviewNeed();

    $scope.addTag = function () {
        var tags = $scope.need.tags;
        var tagName = $("#inputTagName").val();
        if (tags.indexOf(tagName) == -1) {
            $scope.need.tags.push(tagName);
        }
        $("#inputTagName").val('');
    };

    $scope.removeTag = function (tagName) {
        $scope.need.tags.splice($scope.need.tags.indexOf(tagName), 1);
    };

    $scope.nextStep = function () {
        if ($scope.currentStep <= $scope.numberOfSteps) {
			// -(-1) instead of +1 is just a hack to interpret currentStep as int and not as string
            $scope.jumpToStep( $scope.currentStep - (-1));
        }

    }
    $scope.previousStep = function () {
        if ($scope.currentStep >= 1) {
			      $scope.jumpToStep( $scope.currentStep - 1)
        }
    }

    $scope.jumpToStep = function(num){
        $log.debug(num);
        $scope.saveDraft();
        if(num<=$scope.numberOfSteps){
            $scope.currentStep = num;
            $location.url("create-need/"+$scope.currentStep+"/"+$scope.selectedType +"/"+$scope.need.title).search({"draft": $scope.draftURI});

        }

    }

    $scope.saveDraft = function () {
        var draftBuilderObject = new window.won.DraftBuilder().setContext();
        draftBuilderObject.setCurrentStep($scope.currentStep);
        draftBuilderObject.setCurrentMenuposition($scope.selectedType);
        draftBuilderObject.setDraftObject($scope.need);
        console.log('create-need.js:saveDraft - saved need: ', $scope.need);
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

        // never called now, because location is not known for now   hasLocationSpecification(48.218748, 16.360783)
        draftBuilderObject.hasLocationSpecification($scope.need.latitude, $scope.need.longitude, $scope.need.name);

        if (hasTimeSpecification($scope.need)) {
            draftBuilderObject.hasTimeSpecification(createISODateTimeString($scope.need.startDate, $scope.need.startTime), createISODateTimeString($scope.need.endDate, $scope.need.endTime), $scope.need.recursIn != 'P0D' ? true : false, $scope.need.recursIn, $scope.need.recurTimes);
        }

        // building need as JSON object
        var draftJson = draftBuilderObject.build();
        if ($scope.need.needURI == null || 0 === $scope.need.needURI.length) {
            $scope.need.needURI = wonService.getDefaultWonNodeUri() + "/need/" + utilService.getRandomInt(1, 9223372036854775807);
            draftJson['@graph'][0]['@graph'][0]['@id'] = $scope.need.needURI;
        }
        var createDraftObject = {"draftURI": $scope.need.needURI, "draft": JSON.stringify(draftJson)};

        // save locally
        applicationStateService.addDraft(createDraftObject);
        $scope.draftURI = $scope.need.needURI;

        // save to the server if the user is logged in
        if (userService.isAccountUser()) {
            needService.saveDraft(createDraftObject).then(function(saveDraftResponse){
                if (saveDraftResponse.status === "OK") {
                    $scope.successShow = true;
                } else {
                    // TODO inform about an error
                }
            });
        }
    }

    $scope.validStep = function() {
        if($scope.currentStep > 1) { // after the first step, the need is required to have title, type, description and tags
            if (!$scope.need.basicNeedType || !$scope.need.title || !$scope.need.textDescription || !$scope.need.tags) {
                return false;
            }
        }
        return true;
    };
    if (!$scope.validStep()) {
        $scope.jumpToStep(1);
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

    function hasUri(need) {
        return need.needURI != '' && need.needURI != null;
    }


    var lock = false;
    $scope.publishClicked = function () {
        if(lock== false){
            lock = true;

            var buildCreateMessage = function() { return $scope.buildCreateMsgRefactored() }


            var needBuilder = $scope.partiallyInitNeedBuilder();

            // make sure the user is registered (either with account or private link),
            // then publish the need, so that it is under that account
            var newNeedUriPromise = userService.setUpRegistrationForUserPublishingNeed().then(
                function() {
                     return wonService.createNeed(buildCreateMessage, needBuilder);
                }
            );
            //TODO why are the following calls not part of the promise chain?

            // TODO: should the draft removing part be changed to run only on success from newNeedUriPromise?
            if ($scope.draftURI != null) {
                userService.removeDraft($scope.draftURI);
                $scope.draftURI = null;
            }

            $scope.successShow = true;

            newNeedUriPromise['finally'](function(){
                lock=false;
            });
        }
    }

    $scope.buildCreateMsgRefactored = function() {


        console.log('create-need.js:oiu; - need.images:', $scope.need.images);

        var type = won.WON.BasicNeedTypeDemandCompacted;



        var imgs = $scope.need.images;
        var attachmentUris = []
        console.log('create-need.js:qweorij - imgs:', imgs);
        if(imgs) {
            for (var img of imgs) {
                var uri = 'http://localhost:8080/won/resource/need/attachment/' + utilService.getRandomPosInt();
                attachmentUris.push(uri);
                img.uri = uri;
            }
        }
        console.log('create-need.js:qweorij - imgs:', imgs);



        var wonNodeUri = $location.protocol()+"://"+$location.host()+"/won/resource";
        var publishedContentUri = wonNodeUri + '/need/' + utilService.getRandomPosInt();

        //if type === create -> use needBuilder as well

        var contentRdf = won.buildNeedRdf({
            type : type, //mandatory
            title: $scope.need.title, //mandatory
            description: $scope.need.textDescription, //mandatory
            publishedContentUri: publishedContentUri, //mandatory
            tags: $scope.need.tags.map(function(t) {return t.text}).join(','),
            attachmentUris: attachmentUris, //optional
        });
        var msgJson = won.buildMessageRdf(contentRdf, {
            receiverNode : wonNodeUri, //mandatory
            msgType : won.WONMSG.createMessage, //mandatory
            publishedContentUri: publishedContentUri, //mandatory
            msgUri: wonNodeUri + '/event/' + utilService.getRandomPosInt(), //mandatory
            attachments: imgs //mandatory
        });

        console.log("create-need.js:434: ", msgJson);
        console.log("create-need.js:435: stringified ", JSON.stringify(msgJson));
        console.log("create-need.js:436 - need: ", $scope.need);

        return msgJson;

    }


	$scope.partiallyInitNeedBuilder = function () {

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
            .hasTag(utilService.concatTags($scope.need.tags))
            .hasContentDescription()    // mandatory
            //.hasPriceSpecification("EUR",5.0,10.0)
        
            needBuilderObject.hasLocationSpecification($scope.need.latitude, $scope.need.longitude, $scope.need.name);

        if (hasTimeSpecification($scope.need)) {
            needBuilderObject.hasTimeSpecification(createISODateTimeString($scope.need.startDate, $scope.need.startTime), createISODateTimeString($scope.need.endDate, $scope.need.endTime), $scope.need.recursIn != 'P0D' ? true : false, $scope.need.recursIn, $scope.need.recurTimes);
        }

        if (hasUri($scope.need)) {
            needBuilderObject.needUri($scope.need.needURI);
        }

        if($scope.need && $scope.need.images) {
            needBuilderObject.images($scope.need.images);
        }


        return needBuilderObject;

    };

    //make sure we've got an array for gathering the images
    $scope.need.images = $scope.need.images? $scope.need.images : [];

    $scope.onImagesPicked = function(images) {
        won.mergeIntoLast(images, $scope.need.images);
        //$scope.need.images = images;
        // TODO <testing>
        $scope.need.images = [];
        $scope.need.images[0] = images[0];
        console.log('create-need.js:imgPick - need.images:', $scope.need.images);
        // TODO </testing>
    }

    $scope.goToDetailPostPreview = function() {
        var detailPreviewElementId = 'detail-preview';
        $location.hash(detailPreviewElementId);
        $anchorScroll();
    }

	$scope.cancel = function () {
		$location.url("/");
	};


    $scope.validatePostForm = function () {
        var result = ($scope.need.basicNeedType != 'undefined' && $scope.need.basicNeedType != null && $scope.need.basicNeedType != '') &&
            ($scope.need.title != 'undefined' && $scope.need.title != null && $scope.need.title != '') &&
            ($scope.need.textDescription != 'undefined' && $scope.need.textDescription != null && $scope.need.textDescription != '') &&
            ($scope.need.tags != 'undefined' && $scope.need.tags != null && $scope.need.tags != '');

        $scope.$broadcast('validatePostFormEvent', result);
        return result;
    }

    $scope.validateDateTimeRange = function () {
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

    //TODO remove all those cases of double negatives (ng-disabled -> ng-show)
    $scope.goToPreviewButtonDisabled = function () {
        switch (parseInt($scope.currentStep)) {
            case 1:
                return !$scope.validatePostForm() || !$scope.validateDateTimeRange();
            case 2:
                return !$scope.validateDateTimeRange();
            default:
                return true;
        }
    }

    var previewButtonText = ["Skip to Preview", "Preview", ""]
    $scope.gotoPreviewButtonText = function () {
        return previewButtonText[$scope.currentStep - 1];
    }

    $scope.allDay = false;

    $scope.clickOnAllDay = function () {
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
angular.module('won.owner').directive('wonProgressTracker', function factory($log) {
    return {
        restrict: 'AE',
        templateUrl: "app/create-need/progress-tracker.html",
        scope: {
            numberOfSteps: '=numberOfSteps',
            currentStep: '=currentStep', //TODO bind this as integer
            jumpToStep: '&'
        },
        controller: function ($scope) {
            $scope.processSteps = {
                firstStep: false,
                secondStep: false,
                thirdStep: false,
                fourthStep: false
            };

            $scope.setFlagForCurrentStep = function () {
                if (currentStep == 1) {
                    $scope.processSteps.firstStep = true;
                } else if (currentStep == 2) {
                    $scope.processSteps.secondStep = true;
                } else if (currentStep == 3) {
                    $scope.processSteps.thirdStep = true;
                }
                /*else if(currentStep == 4){
                 $scope.processSteps.fourthStep = true;
                 } */
            };
            $scope.showPublic = function (num) {
                if ($scope.currentStep != num) {
                    return false;
                } else if ($scope.currentStep == num) {
                    return true;
                }
            };
            $scope.increaseStep = function () {
                $scope.currentStep++;
            }


            // states for menu items and event handling for form validation
            $scope.navStep1Disabled = '';
            $scope.navStep2Disabled = 'disabled';
            $scope.navStep3Disabled = 'disabled';

            $scope.$on('validatePostFormEvent', function (event, eventData) {
                if (eventData == true) {
                    $scope.navStep2Disabled = '';
                } else {
                    $scope.navStep2Disabled = 'disabled';
                }
            })

            $scope.$on('validateDateTimeRangeEvent', function (event, eventData) {
                if (eventData == true) {
                    $scope.navStep3Disabled = '';
                } else {
                    $scope.navStep3Disabled = 'disabled';
                }
            })
        },
        link: function (scope, element, attrs) {
            $log.debug("Progress Tracker");
        }
    }
})
angular.module('won.owner').directive('wonGallery', function factory() {
    return {
        restrict: 'A',
        templateUrl: "app/create-need/won-gallery.html",
        scope: {
            need: '=need'
        },
        link: function (scope, element, attrs) {

            $('#photo').change(function () {
                angular.element("#photo-form").scope().submit();
            });
        },
        controller: function ($scope, $location, $log) {
            $scope.selectedPhoto = 0;
            $scope.getCleanPhotos = function () {
                return [
                    {uri: ''},
                    {uri: ''},
                    {uri: ''}
                ];
            };
            $scope.photos = $scope.getCleanPhotos();

            $scope.onClickPhoto = function (num) {
                $scope.selectedPhoto = num;
                $log.debug($scope.selectedPhoto);
            };
            $scope.$on('fileuploadsubmit', function (e, data) {
                var filename = data.files[0].name;
                $scope.lastExtension = extension = filename.substr(filename.lastIndexOf("."), filename.lenght);
            });

            $scope.$watch('need', function (newVal, oldVal) {
                if (newVal.binaryFolder != oldVal.binaryFolder) {
                    $scope.photos = $scope.getCleanPhotos();
                }
            });

            $scope.$on('file uploadstop', function (e, data) {
                var absPath = $location.absUrl();
                var ownerPath = absPath.substr(0, absPath.indexOf('#'));
                $scope.photos[$scope.selectedPhoto].uri = ownerPath + 'rest/needphoto/'
                + $scope.need.binaryFolder + "/" + $scope.selectedPhoto + $scope.lastExtension + '/';
                $scope.need.images = [];
                angular.forEach($scope.photos, function (photo) {
                    if (photo.uri) {
                        $scope.need.images.push(angular.copy(photo));
                    }
                }, $scope);
            });
            $scope.currentStep = 1;

            $scope.onClickNeedType = function (currentStep) {
                $scope.currentStep = $scope.currentStep + 1;
            };
        }
    };
});
angular.module('won.owner').controller('AdditionalInfoCtrl',
    function ($scope, $location, $http, $log, needService, userService) {
        $scope.imageInputFieldCollapsed = true;
        $scope.locationInputFieldCollapsed = true;
        $scope.timeInputFieldCollapsed = true;

        $scope.imageCollapseClick = function () {
            $scope.imageInputFieldCollapsed = !$scope.imageInputFieldCollapsed;
            if ($scope.imageInputFieldCollapsed == false) {
                /*   $location.hash('imagesInfoTitleWell');
                 $anchorScroll();*/
            }
        };

        $scope.locationCollapseClick = function () {
            $scope.locationInputFieldCollapsed = !$scope.locationInputFieldCollapsed;
            if ($scope.locationInputFieldCollapsed == false) {

                /*  $location.hash('locationInfoTitleWell');
                 $anchorScroll();  */
            }
        };

        $scope.timeCollapseClick = function () {
            $scope.timeInputFieldCollapsed = !$scope.timeInputFieldCollapsed;
            if ($scope.timeInputFieldCollapsed == false) {
                /*   $location.hash('timeInfoTitleWell');
                 $anchorScroll();    */
            }
        };

        $scope.getImagesComment = function () {
            if ($scope.selectedType == 0) return "Add photos of similar things or sketches to give people a better idea what you have in mind."
            if ($scope.selectedType == 1) return "Add photos or sketches to give people a better idea what you're offering.";
            if ($scope.selectedType == 2) return "If you want you can add an image or photo here to illustrate the activity.";
            if ($scope.selectedType == 3) return "Add a photo, sketch (or screenshot) of the problem you want to point out.";
        }

        $scope.getLocationComment = function () {
            if ($scope.selectedType == 0) return "Where should the thing be available? i.e. where would you pick it up or where should it be delivered to?"
            if ($scope.selectedType == 1) return "Where\'s your offer available? i.e. where can people pick it up or where would you deliver it too?";
            if ($scope.selectedType == 2) return "Where's the action happening?";
            if ($scope.selectedType == 3) return "Where did the problem occur / where have things to be changed?";
        }

    });


