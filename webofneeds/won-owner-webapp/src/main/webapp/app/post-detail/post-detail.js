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
angular.module('won.owner').controller('PostDetailCtrl', function ($scope, $location, mapService, $compile, $rootScope, $routeParams) {
    //$scope.postId = $routeParams.phoneId;
    //alert($routeParams.postId);
    var imagesPerPage = 6;

    <!-- TODO call here backend to load images of the post -->
    $scope.images = [{url: '/images/thumbnail_demo.jpg'},
        {url: '/images/thumbnail_demo_blue.jpg'},
        {url: '/images/thumbnail_demo_brown.jpg'},
        {url: '/images/thumbnail_demo_green.jpg'},
        {url: '/images/thumbnail_demo_red.jpg'},
        {url: '/images/thumbnail_demo_yellow.jpg'},
        {url: '/images/thumbnail_demo.jpg'},
        {url: '/images/thumbnail_demo_blue.jpg'},
        {url: '/images/thumbnail_demo_brown.jpg'},
        {url: '/images/thumbnail_demo_green.jpg'}];

    // just simple for now
    $scope.bigImage = $scope.images[0];

    $scope.clickOnThumbnail = function(index) {
        if (index >= 0 && index <= $scope.images.length) {
            $scope.bigImage = $scope.images[index];
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
            imageElements += '<a href="" ng-click="clickOnThumbnail(' + i + ');" ><img class="galleryImage" src="' + $scope.images[i].url + '" ng-click="clickOnThumbnail(' + i + ');"></a>';
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

    $('#time_from').datepicker({
        changeMonth:true,
        changeYear:true,
        defaultDate:'08/11/2014'
    });
    $('#time_to').datepicker({
        changeMonth:true,
        changeYear:true,
        defaultDate:'08/13/2014'
    });


    $scope.location = 'Thurngasse 8, 1080 Vienna, Austria';
    $scope.locationOutputFieldCollapsed = true;
    $scope.time = 'Mon, Aug 11 - Wed, Aug 13 2014';
    $scope.timeInputFieldCollapsed = true;

    $scope.outputLocationCollapseClick = function () {
        $scope.locationOutputFieldCollapsed = !$scope.locationOutputFieldCollapsed;
    };

    $scope.timeInputFieldCollapsedClick = function () {
        $scope.timeInputFieldCollapsed = !$scope.timeInputFieldCollapsed;
    };

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

    $scope.contactFormActiv = false;
    $scope.clickOnContact = function(){
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

angular.module('won.owner').directive('wonContact',function factory(userService){
    return {
        restrict: 'AE',
        templateUrl : "app/post-detail/contact.html",
        scope: {},
        /*scope : {
         numberOfSteps : '=numberOfSteps',
         currentStep : '=currentStep',
         jumpToStep : '&'
         } ,     */
        controller : function($scope){
            $scope.message = '';
            $scope.sendStatus = false; //todo refresh this var each time when we click on show contact form
            $scope.email = '';
            $scope.postTitle = 'LG TV 40"';//todo set value normaly
            $scope.privateLink = 'https://won.com/la3f#private';//todo set value normaly

            $scope.sendMessage = function() {
                //TODO Put here logic
                if(!$scope.sendStatus)$scope.sendStatus = true;
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

