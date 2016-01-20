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
            $scope.humanReadableType = applicationControlService.humanReadableType;
            $scope.getNeedType= applicationControlService.getNeedType;
        }
    }
})
angular.module('won.owner').factory('applicationControlService', function (applicationStateService,$log, $location) {

    //the service
    var applicationControlService = {};

        //private data only used inside the service
    var privateData = {};

    applicationControlService.goToNeedDetailView = function(needUri){
        $location.url("/post-detail").search({"need": needUri});
    }

    applicationControlService.goHome = function(){
        $location.url("/home");
    }


    /**
     * Sets the currentNeedURI in the applicationStateService and opens the private-link page.
     * @param needURI
     */
    applicationControlService.openNeedDetailView = function openNeedDetailView(needURI){
        if (typeof needURI === 'undefined') {
            $log.warn("needURI must be defined!");
            return;
        }
        applicationStateService.setCurrentNeedURI(needURI);
        $location.url("/private-link");
    }
    applicationControlService.getTypePicURI = function (type) {
        switch(type) {
            case won.WON.BasicNeedTypeDemand: return "/owner/images/type_posts/want.png";
            case won.WON.BasicNeedTypeCritique: return "/owner/images/type_posts/change.png";
            case won.WON.BasicNeedTypeSupply: return "/owner/images/type_posts/offer.png";
            case won.WON.BasicNeedTypeDotogether: return "/owner/images/type_posts/todo.png";
            default:
                $log.error("Tried to get icon url with the invalid type: " + type)
                return ""
        }
    };
    applicationControlService.humanReadableType = function (type) {
        switch(type) {
            case won.WON.BasicNeedTypeDemand: return "Demand";
            case won.WON.BasicNeedTypeCritique: return "Critique";
            case won.WON.BasicNeedTypeSupply: return "Supply";
            case won.WON.BasicNeedTypeDotogether: return "Together";
            default:
                $log.error("Tried to get human readable need type with the invalid type: " + type)
                return ""
        }
    };
    applicationControlService.getMachineReadableNeedState = function(needState){
        switch (needState){
            case 'Active':
                return won.WON.Active;
            case 'Inactive':
                return won.WON.Inactive;
        }
    }
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
    applicationControlService.getNeedType = function(typeNum){
        switch (typeNum){
            case 0: return won.WON.BasicNeedTypeDemand;
            case 1: return won.WON.BasicNeedTypeSupply;
            case 2: return won.WON.BasicNeedTypeDotogether;
            case 3: return won.WON.BasicNeedTypeCritique;
        }
    }
    return applicationControlService;
});