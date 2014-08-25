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

angular.module('won.owner').controller('DraftsCtrl', function ($scope, $location, userService) {
	$scope.search = '';
    $scope.displayConfirmationDialog = false;
    var indexOfChosenDraft;

    // TODO call here backend method
    function deleteDraft(index) {
        if (index >= 0) {
            $scope.drafts.splice(index, 1);
        }
    }

	// TODO call backend method here
	$scope.drafts = [
		{type:'Together', title:'Car sharing to Prague', datetime:'Yesterday'},
		{type:'Want', title:'Moved recently ...', datetime:'Yesterday'},
		{type:'Change', title:'Let\'s clean ...', datetime:'Mon, 28.6. 2014'},
		{type:'Offer', title:'Friendly Bicycle ...', datetime:'April 2014'},
		{type:'Offer', title:'Old children\'s clothes ..', datetime:'Sep 2013'}
	];

	// TODO call backend method here
	$scope.recent4drafts = [
		{type:'Together', title:'Car sharing to Prague', datetime:'Yesterday'},
		{type:'Want', title:'Moved recently ...', datetime:'Yesterday'},
		{type:'Change', title:'Let\'s clean ...', datetime:'Mon, 28.6. 2014'},
		{type:'Offer', title:'Friendly Bicycle ...', datetime:'April 2014'}
	]

	$scope.clickOnRemoveButton = function (index) {
        $scope.displayConfirmationDialog = true;
        indexOfChosenDraft = index;
	}

    $scope.getTypePicURI = function (type) {
        if(type=='Want') return "/images/type_posts/want.png";
        else if(type=='Change') return "/images/type_posts/change.png";
        else if(type=='Offer') return "/images/type_posts/offer.png";
        else return "/images/type_posts/todo.png";
    };

    $scope.clickOnYesButton = function() {
        deleteDraft(indexOfChosenDraft);
        $scope.displayConfirmationDialog = false;
    }

    $scope.clickOnNoButton = function() {
        $scope.displayConfirmationDialog = false;
    }
});
