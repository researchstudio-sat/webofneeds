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
 * Created by LEIH-NB on 20.10.2014.
 */
angular.module('won.owner').directive('removeControlData',function factory(){
    return {
        restrict: 'E',
        scope: {
            text: '@',
            removeItem:"&",
            displayConfirmation: "=",
            chosenItem:"="
        },
        templateUrl: 'app/draft/remove-dialogue.html',
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
angular.module('won.owner').directive('applicationControl', function factory(){
    return {
        restrict: 'A',
        controller: function($scope, applicationControlService){
            $scope.getTypePicURI = applicationControlService.getTypePicURI;
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

        },
        link: function(scope, elem, attr){
            scope.type = attr.type;
            scope.collapsed = (scope.collapsed === 'true')
        }
    }
})
angular.module('won.owner').controller("PostInboxCtrl", function($scope,$location, applicationStateService, applicationControlService, userService) {
    $scope.postData = {};
    $scope.recordsToDisplay = 4;
    $scope.displayConfirmation = false;
    $scope.postsSortedField = 'timestamp';
    $scope.postsReversedSort = true;
    $scope.templateUrl = 'app/draft/post-inbox-table.html';
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
    $scope.setSortParams = function(fieldName) {
        if ($scope.postsSortedField == fieldName) {
            $scope.postsReversedSort = !$scope.postsReversedSort;
        } else {
            $scope.postsReversedSort = false;
            $scope.postsSortedField = fieldName;
        }
    }
})
angular.module('won.owner').controller("DraftCtrl", function($scope,$location, applicationStateService, applicationControlService, userService) {
    $scope.draftData = {};
    $scope.recordsToDisplay = 4;
    $scope.displayConfirmation = false;
    $scope.draftsSortedField = 'timestamp';
    $scope.draftsReversedSort = true;
    $scope.templateUrl='app/draft/draft-inbox-table.html';

    $scope.removeDraft = function(){
       return userService.removeDraft($scope.draftData.uri);
    }
    $scope.clickOnDraft = function(){
        //$location.path("create-need/"+draft.currentStep+"/"+draft.menuposition);
        $scope.draftData.chosenDraft = $scope.allDrafts[$scope.draftData.uri];
        $location.path("create-need/"+$scope.draftData.chosenDraft.meta.currentStep+"/"+$scope.draftData.chosenDraft.meta.menuposition+"/"+$scope.draftData.chosenDraft.title).search({"draft": $scope.draftData.chosenDraft.uri});

    }
    $scope.allDrafts = applicationStateService.getAllDrafts();
    // TODO call here backend method
    $scope.draftsCollapseClick = function () {
        $scope.draftsCollapsed = !$scope.draftsCollapsed;
    };
    $scope.clickOnRemoveButton = function () {
        $scope.displayConfirmation = true;
    }
    $scope.hasDrafts = function () {
        return applicationStateService.getAllDraftsCount() > 0;
    }
    $scope.setSortParams = function(fieldName) {
        if ($scope.draftsSortedField == fieldName) {
            $scope.draftsReversedSort = !$scope.draftsReversedSort;
        } else {
            $scope.draftsReversedSort = false;
            $scope.draftsSortedField = fieldName;
        }
    }
})
