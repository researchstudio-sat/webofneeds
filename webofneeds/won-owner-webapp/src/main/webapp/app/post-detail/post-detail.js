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
angular.module('won.owner').controller('PostDetailCtrl', function ($scope, $location, mapService, $compile, $routeParams,applicationControlService, applicationStateService, userService) {
    //$scope.postId = $routeParams.phoneId;
    //alert($routeParams.postId);
    $scope.showPublic = function(){
        return userService.isAuth();
    }
    $scope.clickOnCopy = function(){
        $location.path("create-need/1/"+applicationControlService.getMenuPositionForNeedType($scope.need.basicNeedType));
    }

    $scope.hoverCopyToolTip ="I want this too";
    //$scope.need = $scope.$parent.need;
    $scope.need = {};

    linkedDataService.getNeed(applicationStateService.getCurrentNeedURI()).then(function(need){
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
        console.log('img' + index);
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
    /*
    $('#time_from').datepicker({
        format:'dd.mm.yyyy',
        todayHighlight:true,
        changeMonth:true,
        changeYear:true,
        startDate: $scope.need.startDate
    });
    $('#time_to').datepicker({
        format:'dd.mm.yyyy',
        changeMonth:true,
        changeYear:true,
        endDate:$scope.need.endDate
    }); */

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
        // console.log('location toggle');
    };

    $scope.timeInputFieldCollapsed = true;
    $scope.timeInputFieldCollapsedClick = function () {
        $scope.timeInputFieldCollapsed = !$scope.timeInputFieldCollapsed;
        // console.log('time toggle ' + $scope.timeInputFieldCollapsed);
    };

    /*
    $scope.getMapOptions = function(){

        return {
            center:mapService.getGeolocation(),
            zoom:15,
            mapTypeId:google.maps.MapTypeId.ROADMAP
        };
    }
    $scope.mapOptions = $scope.getMapOptions();

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
    */

    $scope.contactFormActiv = false;
    $scope.clickOnContact = function(){
        console.log('contact clicked');
        $scope.contactFormActiv = !$scope.contactFormActiv;
    }


    /*$scope.mapOptions = $scope.getMapOptions()
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
     };      */

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
        controller : function($scope){
            $scope.message = '';
            $scope.sendStatus = false; //todo refresh this var each time when we click on show contact form
            $scope.email = '';
            $scope.postTitle = 'LG TV 40"';//todo set value normaly
            $scope.privateLink = 'https://won.com/la3f#private';//todo set value normaly
            $scope.dummyUri = '';

            $scope.sendMessage = function() {
                //TODO Put here logic
                // creating need object
                var needBuilderObject = new window.won.NeedBuilder().setContext();
                if ($scope.need.basicNeedType == won.WON.BasicNeedTypeDemand) {
                    needBuilderObject.supply;
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
                    .active()                   // mandatory: active or inactive

                // building need as JSON object
                var needJson = needBuilderObject.build();

                //console.log(needJson);
                var newNeedUriPromise = wonService.createNeed(needJson);
                //console.log('promised uri: ' + newNeedUriPromise);

                newNeedUriPromise.then(function(uri){
                    wonService.connect(uri, $scope.need.uri, $scope.message);
                })

                //$scope.need = $scope.getCleanNeed();      TODO decide what to do
                $scope.successShow = true;
              //  if(!$scope.sendStatus)$scope.sendStatus = true;
            };



            $scope.copyLinkToClipboard = function() {
                //todo maybe we can use http://zeroclipboard.org/
            };
            $scope.showPublic = function() {
                return userService.isAuth();
            };


        } ,
        link: function(scope, element, attrs){
            console.log("Contact form");
        }
    }
});

