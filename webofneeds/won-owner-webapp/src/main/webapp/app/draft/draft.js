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



angular.module('won.owner').controller("DraftCtrl", function($scope,$location, applicationStateService, applicationControlService, userService) {
    $scope.draftData = {};
    $scope.recordsToDisplay = 4;
    $scope.displayConfirmation = false;
    $scope.draftSortedField = "meta.timestamp";
    $scope.templateUrl='app/postbox/draft-inbox-table.html';

    $scope.removeDraft = function(){
       return userService.removeDraft($scope.draftData.uri);
    }
    $scope.clickOn = function(draftUri){
        $scope.draftData.uri = draftUri;
        $scope.clickOnDraft();
    }
    $scope.clickOnDraft = function(){
        //$location.path("create-need/"+draft.currentStep+"/"+draft.selectedType);
        $scope.draftData.chosenDraft = $scope.allDrafts[$scope.draftData.uri];
        $location.path("create-need/"+$scope.draftData.chosenDraft.meta.currentStep+"/"+$scope.draftData.chosenDraft.meta.selectedType+"/"+$scope.draftData.chosenDraft.title).search({"draft": $scope.draftData.chosenDraft.uri});

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

})
