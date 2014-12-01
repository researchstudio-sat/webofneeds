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
 * Date: 8/15/14
 * Time: 8:08 AM
 * To change this template use File | Settings | File Templates.
 */
angular.module('won.owner').controller('PostBoxCtrl', function ($scope,$interval, $location, userService, applicationStateService, applicationControlService) {

});
angular.module('won.owner').directive('removeControlData',function factory(){
    return {
        restrict: 'E',
        scope: {
            text: '@',
            removeItem:"&",
            displayConfirmation: "=",
            chosenItem:"="
        },
        templateUrl: 'app/postbox/remove-dialogue.html',
        controller: function($scope){
            $scope.displayConfirmationDialogue = function() {
                return  $scope.displayConfirmation;
            }
            $scope.clickOnYesButton = function() {
                $scope.displayConfirmation = false;
                $scope.removeItem().then(function(){
                    $scope.chosenItem.uri = null;
                })
            }
            $scope.clickOnNoButton = function() {
                $scope.displayConfirmation = false;
            }
        }
    }
})
angular.module('won.owner').directive('inboxTable',function factory(){
    return {
        restrict: 'E',
        scope:{
            items: '=',
            collapsed: '@',
            clickOnItem: '&',
            removeItem:'&',
            hasItems:'&',
            clickOnRemove:'&',
            controlData:'=',
            templateUrl:'@'
        },
        template:'<div ng-attr-id="id" class="row col-lg-12" ng-include="templateUrl"></div>',
        controller: function($scope){
            $scope.postsSortedField = 'timestamp';
            $scope.postsReversedSort = true;
            $scope.clickOn=function(item){
                $scope.controlData.uri =item.uri;
                //  $scope.chosenItemUri=item.uri;
                $scope.clickOnItem();
            }
            $scope.isCollapsed = function(){
                console.log("type: "+$scope.type + "collapsed: "+$scope.collapsed);
                return $scope.collapsed;
            }
            $scope.clickOnRemove=function(item){
                $scope.controlData.uri = item.uri;
                $scope.removeItem();
            }
            $scope.recordsToDisplay = 4;
            $scope.sortedField = 'timestamp';
            $scope.reversedSort = true;
            $scope.collapseClick = function(){
                console.log("before click: "+$scope.collapsed) ;
                $scope.collapsed = !$scope.collapsed;
                console.log("after click: "+$scope.collapsed) ;
            }
            $scope.setSortParams = function(fieldName) {
                if ($scope.sortedField == fieldName) {
                    $scope.reversedSort = !$scope.reversedSort;
                } else {
                    $scope.reversedSort = false;
                    $scope.sortedField = fieldName;
                }
            }
            $scope.setSortParams = function(fieldName) {
                if ($scope.postsSortedField == fieldName) {
                    $scope.postsReversedSort = !$scope.postsReversedSort;
                } else {
                    $scope.postsReversedSort = false;
                    $scope.postsSortedField = fieldName;
                }
            }

        },
        link: function(scope, elem, attr){
            scope.type = attr.type;
            scope.collapsed = false;
            //scope.collapsed = (scope.collapsed === 'true')
        }
    }
})

angular.module('won.owner').controller("ClosedInboxCtrl",function($scope,$location, applicationStateService, applicationControlService, userService){
    $scope.closedData = {};

    $scope.templateUrl = 'app/postbox/closed-inbox-table.html';
    $scope.allClosed = applicationStateService.getAllClosed();
    $scope.clickOnClosed = function(){
        $scope.closedData.chosenPost = $scope.allClosed[$scope.closedData.uri];
        applicationStateService.setCurrentNeedURI($scope.closedData.uri);
        $location.path("/private-link");
        $scope.hasClosed = function(){
            return applicationStateService.getAllClosedCount()>0;

        }

    }
})
angular.module('won.owner').controller("PostInboxCtrl", function($scope,$location, applicationStateService, applicationControlService, userService) {
    $scope.postData = {};
    $scope.recordsToDisplay = 4;
    $scope.displayConfirmation = false;

    $scope.templateUrl = 'app/postbox/post-inbox-table.html';
    $scope.allNeeds = applicationStateService.getAllNeeds();
    $scope.clickOnPost = function(){
        //$location.path("create-need/"+draft.currentStep+"/"+draft.menuposition);
        $scope.postData.chosenPost = $scope.allNeeds[$scope.postData.uri];
        applicationStateService.setCurrentNeedURI($scope.postData.uri);
        $location.path("/private-link")

    }

    $scope.hasPosts = function () {
        return applicationStateService.getAllNeedsCount() > 0;
    }

})