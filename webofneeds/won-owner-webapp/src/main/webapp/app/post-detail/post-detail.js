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

/**
 * Created with IntelliJ IDEA.
 * User: alexey.sidelnikov
 * Date: 8/19/14
 * Time: 10:01 AM
 * To change this template use File | Settings | File Templates.
 */
angular.module('won.owner')
    .directive('starFavourite', function factory(){
        return {
            restrict : 'E',
            templateUrl : 'app/post-detail/favourite.html',
            scope : {
                showPublic: '&'
            },
            priority: 1,
            link : function(scope, elem, attrs){

                scope.hoverStarToolTip = 'save post as favourite';
                scope.activeState= false;
                scope.hoverState = false;
            },
            controller : function($scope, userService){

                $scope.changeHoverState = function(){
                    $scope.hoverState = !$scope.hoverState;
                }
                $scope.getStar = function(){
                    if($scope.activeState == false && $scope.hoverState==false){
                        return 'glyphicon glyphicon-star-empty';
                    }else{
                        return 'glyphicon glyphicon-star';
                    }
                }
                $scope.changeActiveState = function($event){
                    $scope.activeState = !$scope.activeState;
                }
            }
        }
    });

angular.module('won.owner')
    .directive('wonPostDetail',
    function postDetailFactory($log, applicationControlService,userService) {

    var directive =  {
        scope:{
            need: '=need'
        },
        restrict:'AE',
        templateUrl:"app/post-detail/post-detail-content.html",
        link: function(scope, element, attrs){
            $log.debug("wonPostDetail");
            scope.getTypePicURI = applicationControlService.getTypePicURI
            scope.humanReadableType = applicationControlService.humanReadableType
        },//,
        controller: function($scope,applicationStateService){
            $scope.contactFormActiv = false;
            $scope.showPublic = function(){
                return userService.isAuth();
            }
            $scope.canBeContacted = function(){

                // if it is own need, cannot be contacted
                if (applicationStateService.getAllNeeds()[$scope.need.uri]) {
                    return false;
                }
                // TODO check with storyboard people:
                // if it is another need, but communication already established,
                // probably also should not be contacted?

                return true;
            }
            $scope.clickOnContact = function(){
                $log.debug('contact clicked');
                $scope.contactFormActiv = !$scope.contactFormActiv;
            }

        }

        //scope:{},
   }

    return directive
});
angular.module('won.owner').controller('PostDetailCtrl',
    function
        ( $scope
        , $log
        , $location
        , mapService
        , $compile
        , $routeParams
        , applicationControlService
        , applicationStateService
        , userService
        )
    {

    //$scope.postId = $routeParams.phoneId;
    //alert($routeParams.postId);

    $scope.clickOnCopy = function(){
        $location.url("create-need/1/"+applicationControlService.getMenuPositionForNeedType($scope.need.basicNeedType));
    }


    $scope.hoverCopyToolTip ="I want this too";
    //$scope.need = $scope.$parent.need;
    $scope.need = {};

    //linkedDataService.getNeed(applicationStateService.getCurrentNeedURI()).then(function(need){
    linkedDataService.getNeed($routeParams.need).then(function(need){
        $scope.need = need;
        $scope.need.uri = need['uri'];
        $scope.need.title = need['title'];
        $scope.need.tags = need['tags'];
        $scope.need.basicNeedType = need['basicNeedType'];
        $scope.need.textDescription = need['textDescription'];
        $scope.need.creationDate = need['creationDate'];
        $scope.need.longitude = need['longitude'];
        $scope.need.latitude = need['latitude'];
        $scope.need.endTime = need['endTime'];
        $scope.need.recurInfinite = need['recurInfinite'];
        $scope.need.recursIn = need['recursIn'];
        $scope.need.startTime = need['startTime'];
    });

    //TODO move these to a service

    //TODO: location, date, needCreated date


    var imagesPerPage = 6;

    <!-- TODO call here backend to load images of the post -->
    $scope.images = [{url: '/owner/images/thumbnail_demo.jpg'},
        {url: '/owner/images/thumbnail_demo_blue.jpg'},
        {url: '/owner/images/thumbnail_demo_brown.jpg'},
        {url: '/owner/images/thumbnail_demo_green.jpg'},
        {url: '/owner/images/thumbnail_demo_red.jpg'},
        {url: '/owner/images/thumbnail_demo_yellow.jpg'},
        {url: '/owner/images/thumbnail_demo.jpg'},
        {url: '/owner/images/thumbnail_demo_blue.jpg'},
        {url: '/owner/images/thumbnail_demo_brown.jpg'},
        {url: '/owner/images/thumbnail_demo_green.jpg'}];

    // just simple for now
    $scope.bigImage = $scope.images[0];

    $('#bigImage').attr('href', $scope.images[0].url);
    $('#obr').attr('src', $scope.images[0].url);

    $scope.clickOnThumbnail = function(index) {
        $log.debug('img' + index);
        if (index >= 0 && index <= $scope.images.length) {
            //$scope.bigImage = $scope.images[index];

            $('#bigImage').attr('href', $scope.images[index].url);
            $('#obr').attr('src', $scope.images[index].url);
            addImagesOnPageForLightbox(index);
        }
    }

    $scope.clickOnThumbnail(0);

    function addImagesOnPageForLightbox(index) {
        $('.lightbox-image').remove();
        for(var i = 0; i < $scope.images.length; i++) {
            if (i != index) {
                var lightboxElement = '<a class="lightbox-image" href="' + $scope.images[i].url +'" ' + 'data-lightbox="roadtrip"'+'></a>';
                if (i < index) {
                    $('#bigImage').before(lightboxElement);
                } else {
                    $('#bigImageArea').append(lightboxElement);
                }
            }
        }
    }

    function displayFirstPageOfGallery(imagesPerPage) {
        var imageElements = '';
        for(var i = 0; i < imagesPerPage; i++) {
            imageElements += '<a href="" ng-click="clickOnThumbnail(' + i + ');" ><img class="galleryImage" src="' + $scope.images[i].url + '" ng-click="clickOnThumbnail(' + i + ')"></a>';
        }
        $(".gallery").html($compile(imageElements)($scope));
    }

    $scope.createPaginatedGallery = function(imagesPerPage) {
        displayFirstPageOfGallery(imagesPerPage);
        var totalPages = Math.floor($scope.images.length / imagesPerPage) + (($scope.images.length % imagesPerPage > 0)? 1 : 0);
        $('.pager').bootpag({
            total: totalPages,
            page: 1,
            maxVisible: imagesPerPage
        }).on("page", function(event, num){
                $('.galleryImage').remove();
                var startIndex = (num - 1) * imagesPerPage;
                var lastIndex = (startIndex + imagesPerPage) > $scope.images.length ? $scope.images.length : startIndex + imagesPerPage;
                var imageElements = "";
                for(var i = startIndex; i < lastIndex; i++) {
                    imageElements += '<a href="" ng-click="clickOnThumbnail(' + i + ');" ><img class="galleryImage" src="' + $scope.images[i].url + '" ng-click="clickOnThumbnail(' + i + ');"></a>';
                }
                $(".gallery").html($compile(imageElements)($scope));
                $(this).bootpag({total: totalPages, maxVisible: imagesPerPage});
            });
    };

    $scope.createPaginatedGallery(imagesPerPage);

    // TODO fix start and end date
    // TODO fix when date is empty
    $scope.toDateString = function(date) {
        var d = date.split('-');
        var datetime = new Date();
        datetime.setFullYear(d[0]);
        datetime.setMonth(d[1] - 1);
        datetime.setDate(d[2]);
        return datetime.toDateString();
    }

    $scope.location = 'Thurngasse 8, 1080 Vienna, Austria';

    $scope.locationOutputFieldCollapsed = true;
    $scope.outputLocationCollapseClick = function () {
        $scope.locationOutputFieldCollapsed = !$scope.locationOutputFieldCollapsed;
    };

    $scope.timeInputFieldCollapsed = true;
    $scope.timeInputFieldCollapsedClick = function () {
        $scope.timeInputFieldCollapsed = !$scope.timeInputFieldCollapsed;
    };





    $scope.previewRegime = false;
    $scope.previewRegimeOn = function(){
        $scope.previewRegime = true;
    }
});

angular.module('won.owner').directive('wonContact',function factory(userService, wonService){
    return {
        restrict: 'AE',
        templateUrl : "app/post-detail/contact.html",
        scope: {
            need : '='
        },
        controller : function($scope, applicationStateService){
            $scope.message = '';
            $scope.sendStatus = false; //todo refresh this var each time when we click on show contact form
            $scope.email = '';
            $scope.postTitle = 'LG TV 40"';//todo set value normaly
            $scope.privateLink = 'https://won.com/la3f#private';//todo set value normaly
            $scope.dummyUri = '';
            $scope.post = null;//the post that is selected to connect with the current post. if no post is selected, a dummy post will be created
            $scope.allNeeds = applicationStateService.getAllNeeds();
            $scope.dropdownText = 'Select your post'
            $scope.clickOnPost = function(post){
                $scope.dropdownText = post.title;
                $scope.post = $scope.allNeeds[post.uri];
            }
            $scope.sendMessage = function() {
                var needBuilderObject = new window.won.NeedBuilder().setContext();
                if($scope.post == undefined){
                    if ($scope.need.basicNeedType == won.WON.BasicNeedTypeDemand) {
                        needBuilderObject.supply();
                    } else if ($scope.need.basicNeedType == won.WON.BasicNeedTypeSupply) {
                        needBuilderObject.demand();
                    } else if ($scope.need.basicNeedType == won.WON.BasicNeedTypeDotogether) {
                        needBuilderObject.doTogether();
                    } else {
                        needBuilderObject.critique();
                    }
                    needBuilderObject.title('Request for converstion to '+$scope.need.title)
                        .ownerFacet()               // mandatory
                        .description('')
                        .hasTag('')
                        .hasContentDescription('')    // mandatory
                        //.hasPriceSpecification("EUR",5.0,10.0)
                }
                //TODO Put here logic
                // creating need object




                // building need as JSON object
                var needJson = needBuilderObject.build();

                if($scope.post == undefined){
                    var newNeedUriPromise = wonService.createNeed(needJson);

                    newNeedUriPromise.then(function(uri){
                        wonService.connect(uri, $scope.need.uri, $scope.message);
                    }).then(function(){
                        $scope.sendStatus= true;
                    })
                } else{
                    wonService.connect($scope.post.uri, $scope.need.uri, $scope.message).then(function(){
                        $scope.sendStatus = true;
                    });
                }




                //$scope.need = $scope.getCleanNeed();      TODO decide what to do
                $scope.successShow = true;
              //  if(!$scope.sendStatus)$scope.sendStatus = true;
            };

            $scope.clickHandler = function(e){
                e.target.dispatchEvent(new DataTrans("copy"));
            }

            $scope.copyHandler = function(e) {
                e.clipboardData.setData("text/plain",$scope.privateLink);
                //todo maybe we can use http://zeroclipboard.org/
            };
            $scope.showPublic = function() {
                return userService.isAuth();
            };


        } ,
        link: function(scope, element, attrs){

        }
    }
});

