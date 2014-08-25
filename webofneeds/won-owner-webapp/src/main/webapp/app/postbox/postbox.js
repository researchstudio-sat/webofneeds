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
angular.module('won.owner').controller('PostBoxCtrl', function ($scope, $location, userService) {
	$scope.line = {
		type:'',
		title:'',
		newMessages:0,
		megaphone:0,
		puzzle:0,
		date:''
	};

    $scope.displayConfirmationDialog = false;
    var indexOfChosenDraft;

	$scope.search = '';

    // TODO call here backend method
    function deleteDraft(index) {
        if (index >= 0) {
            $scope.drafts.splice(index, 1);
        }
    }

	// TODO call backend method here
	$scope.posts = [
		{type:1, title:'Playing soccer together', time:'2 days'},
		{type:2, title:'Looking for a flatscreen TV', time:'6 days'},
		{type:3, title:'Offering IKEA couch', time:'5 min'},
		{type:4, title:'Collect items for Caritas asylum', time:'1 day'}
	];

    $scope.getTypePicURI = function (type) {
        if(type==1) return "/images/type_posts/want.png";
        else if(type==2) return "/images/type_posts/change.png";
        else if(type==3) return "/images/type_posts/offer.png";
        else return "/images/type_posts/todo.png";
    };

    // TODO call backend method here
    $scope.drafts = [
        {type:'Together', title:'Car sharing to Prague', datetime:'Yesterday'},
        {type:'Want', title:'Moved recently ...', datetime:'Yesterday'},
        {type:'Change', title:'Let\'s clean ...', datetime:'Mon, 28.6. 2014'},
        {type:'Offer', title:'Friendly Bicycle ...', datetime:'April 2014'},
        {type:'Offer', title:'Old children\'s clothes ..', datetime:'Sep 2013'}
    ];


	$scope.clickOnMessage = function () {
		//TODO Put here logic
	};

	$scope.clickOnMegaphone = function () {
		//TODO Put here logic
	};

	$scope.clickOnPuzzle = function () {
		//TODO Put here logic
	};

	$scope.showPublic = function () {
		return userService.isAuth();
	};

    $scope.undreadSortDesc = false;
    $scope.undreadClick = function () {
        $scope.undreadSortDesc = !$scope.undreadSortDesc;
    };

    $scope.createdOnSortDesc = false;
    $scope.createdOnClick = function () {
        $scope.createdOnSortDesc = !$scope.createdOnSortDesc;
    };

    $scope.closedOnSortDesc = false;
    $scope.closedOnClick = function () {
        $scope.closedOnSortDesc = !$scope.closedOnSortDesc;
    };

    $scope.inboxCollapsed = false;
    $scope.inboxCollapseClick = function () {
        $scope.inboxCollapsed = !$scope.inboxCollapsed;
    };

    $scope.draftsCollapsed = true;
    $scope.draftsCollapseClick = function () {
        $scope.draftsCollapsed = !$scope.draftsCollapsed;
    };

    $scope.closedCollapsed = true;
    $scope.closedCollapseClick = function () {
        $scope.closedCollapsed = !$scope.closedCollapsed;
    };

    $scope.closedList = [
        {type:1, title:'Playing soccer together', time:'1 days'},
        {type:2, title:'Looking for a flatscreen TV', time:'3 days'}
    ];

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
});