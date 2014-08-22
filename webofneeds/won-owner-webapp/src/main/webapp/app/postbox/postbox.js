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

	$scope.search = '';
	// TODO call backend method here
	$scope.posts = [
		{type:1, title:'Playing soccer together', time:'2 days'},
		{type:2, title:'Looking for a flatscreen TV', time:'6 days'},
		{type:3, title:'Offering IKEA couch', time:'5 min'},
		{type:4, title:'Collect items for Caritas asylum', time:'1 day'}
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

    $scope.inboxCollapsed = false;
    $scope.inboxCollapseClick = function () {
        $scope.inboxCollapsed = !$scope.inboxCollapsed;
    };
});