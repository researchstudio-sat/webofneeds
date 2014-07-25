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

angular.module('won.owner').factory('mapService', function () {
    var currentGeolocation = new google.maps.LatLng(35.784, -78.670);

	return {

        getInitialLocation:function(){
            //var initialLocation;
            if(navigator.geolocation){
                navigator.geolocation.getCurrentPosition(function(position) {
                    currentGeolocation = new google.maps.LatLng(position.coords.latitude,position.coords.longitude);
                    var temp =new google.maps.LatLng(position.coords.latitude,position.coords.longitude);
                    return currentGeolocation;
                },function(){
                    this.handleNoGeolocation();
                },{timeout:10000})
            }
            else {
                alert('gelocation not supported');
            }
        },
        handleNoGeolocation:function(){
            return currentGeolocation;
        },
        getGeolocation:function(){
            this.getInitialLocation();
            return currentGeolocation;
        }
    }


});
