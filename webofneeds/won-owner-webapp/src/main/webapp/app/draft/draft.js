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

angular.module('won.owner').controller("DraftCtrl", function($scope,$location, applicationStateService, applicationControlService) {
    var indexOfChosenDraft;
    $scope.recordsToDisplay = 4;

    function deleteDraft(index) {
        if (index >= 0) {
            $scope.drafts.splice(index, 1);
        }
    }
    $scope.clickOnDraft = function(draft){
        $location.path("create-need/"+draft.currentStep+"/"+draft.menuposition);
    }
    $scope.allDrafts = applicationStateService.getAllDrafts();
    // TODO call here backend method

    $scope.draftsCollapsed = true;
    $scope.draftsCollapseClick = function () {
        $scope.draftsCollapsed = !$scope.draftsCollapsed;
    };
    $scope.clickOnRemoveButton = function (index) {
        $scope.displayConfirmationDialog = true;
        indexOfChosenDraft = index;
    }

    $scope.clickOnYesButton = function() {
        deleteDraft(indexOfChosenDraft);
        $scope.displayConfirmationDialog = false;
    }
    $scope.clickOnNoButton = function() {
        $scope.displayConfirmationDialog = false;
    }
})
