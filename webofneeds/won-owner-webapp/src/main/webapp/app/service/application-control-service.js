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
 * Provides high-level application control functions that change application state and view.
 *
 */
angular.module('won.owner').directive('applicationControl', function factory(){
    return {
        restrict: 'A',
        controller: function($scope, applicationControlService){
            $scope.getTypePicURI = applicationControlService.getTypePicURI;
        }
    }
})
angular.module('won.owner').factory('applicationControlService', function (applicationStateService, $location) {

    //the service
    var applicationControlService = {};

        //private data only used inside the service
    var privateData = {};

    /**
     * Sets the currentNeedURI in the applicationStateService and opens the private-link page.
     * @param needURI
     */
    applicationControlService.openNeedDetailView = function openNeedDetailView(needURI){
        if (typeof needURI === 'undefined') {
            console.log("needURI must be defined!");
            return;
        }
        applicationStateService.setCurrentNeedURI(needURI);
        $location.path("/private-link");
    }
    applicationControlService.getTypePicURI = function (type) {
        if(type==won.WON.BasicNeedTypeDemand) return "/owner/images/type_posts/want.png";
        else if(type==won.WON.BasicNeedTypeCritique) return "/owner/images/type_posts/change.png";
        else if(type==won.WON.BasicNeedTypeSupply) return "/owner/images/type_posts/offer.png";
        else return "/owner/images/type_posts/todo.png";
    };
    applicationControlService.getMenuPositionForNeedType = function(needType){
        switch (needType){
            case won.WON.BasicNeedTypeDemand:
                return 1;
            case won.WON.BasicNeedTypeSupply:
                return 2;
            case won.WON.BasicNeedTypeDotogether:
                return 3;
            case won.WON.BasicNeedTypeCritique:
                return 4;
        }
    }
    return applicationControlService;
});