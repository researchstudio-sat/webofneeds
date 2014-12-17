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

angular.module('won.owner').controller('HomeCtrl',
    function ($scope,$routeParams, $location, userService, $log) {

    var firstDisplay = true;
    var time = 400;


    $log.debug("Initializing HeaderCtrl.");



	$scope.goToNewNeed = function() {
		if(userService.isAuth()) {
			$location.url("/create-need");
		} else {
            $location.url("/signin");
		}
	}

	$scope.goToAllNeeds = function () {

		if (userService.isAuth()) {
            $location.url("/need-list");
		} else {
            $location.url("/signin");
		}
	}

	$scope.forms = new function() {
		this.signin = ($location.path().indexOf("signin") > -1);
		this.register = ($location.path().indexOf("register") > -1);
		this.reset = function() {
			this.signin = false;
			this.register = false;
		}
	}

    $scope.clickOnMoreButton = function () {
        $('#landing_page').slideToggle();
    }

    $scope.closeComicsPanel = function () {
        $('#comics_panel').fadeOut(time);
        $('.hotspot1').css('background-color', 'transparent');
        $('.hotspot2').css('background-color', 'transparent');
        $('.hotspot3').css('background-color', 'transparent');
        $('.hotspot4').css('background-color', 'transparent');
    }

    $scope.clickOnHotspot = function (hotspotID) {
        $('#comics1').hide();
        $('#comics2').hide();
        $('#comics3').hide();
        $('#comics4').hide();

        var color = '';
        switch (hotspotID) {
            case 1:
                $('#comics1').show();
                color = 'red';
                break;
            case 2:
                $('#comics2').show();
                color = 'yellow'
                break;
            case 3:
                $('#comics3').show();
                color = 'blue';
                break;
            case 4:
                $('#comics4').show();
                color = 'green'
                break;
            default:
                return;
        }

        $('#comics').css('border-right-color', color);
        $('.hotspot' + hotspotID).css('background-color', color);

        if ($scope.prevHotspotID != hotspotID) {
            $('#comics_panel').show();
            $('.hotspot' + $scope.prevHotspotID).css('background-color', 'transparent');
        } else {
            var hotspotElement = $('.hotspot' + hotspotID);
            $('#comics_panel').toggle();
            hotspotElement.css('background-color', $('#comics_panel').css('display') == 'none' ? 'transparent' : color);
        }

        if (firstDisplay) {
            $('#comics').scroller({
                horizontal:true
            });
            firstDisplay = false;
        }
        $scope.prevHotspotID = hotspotID;
    }

    $scope.clickTemp = function () {
        $('.popover-markup>.trigger').popover({
            html: true,
            title: function () {
                return $(this).parent().find('.head').html();
            },
            content: function () {
                return $(this).parent().find('.content').html();
            }
        });
    }

    $scope.showPublic = function() {
        return !userService.isAuth();
    };

    $scope.clickOnIButton = function() {
        $('#i_panel').show();
        $('#others_panel').hide();
        $('#i_button').addClass('btn-info');
        $('#others_button').removeClass('btn-warning');
        $scope.othersPost.reset();
    }

    $scope.clickOnOthersButton = function() {
        $('#others_panel').show();
        $('#i_panel').hide();
        $('#others_button').addClass('btn-warning');
        $('#i_button').removeClass('btn-info');
        $scope.iPost.reset();
    }

/*I panel*/
    $scope.iMenu = {item0 : 'want to have something', item1 : 'offer something', item2 : 'want to do something together', item3 : 'want to change something'};
    $scope.iMenuValidationMessage = 'Please choose type of post';
    $scope.iPostValidationMessage = 'Please enter a title for your post';
    $scope.chooseText = 'Choose if you want to find what you need or what others need';
    $scope.iChooseTypeText = 'Choose type of post';
    $scope.iGivePostTitlText = 'Give your post a title';
    $scope.iPostBtnText = 'New post';
    $scope.iPost = new function(){
        this.reset = function() {
            if(this.selectedType > -1){
                $('#IMenuItem' + this.selectedType).removeClass('active');
            }
            this.title = '';
            this.selectedType = -1;
            this.firstattempt = true;
        }

        this.reset();
    };

    $scope.iPost.selectedType = -1;
    $scope.$watch('selectedType', function(newVal,oldVal){
        if(newVal != oldVal ){
            $scope.onClickIMenuItem(newVal, oldVal);
        } else if(oldVal != $scope.iPost.selectedType){
            $scope.onClickIMenuItem(newVal, $scope.iPost.selectedType);
        }
    });
    //$('#IMenuItem' + $scope.selectedType).addClass('active');
    $scope.onClickIMenuItem = function(item, oldVal) {

        if(item > -1){

            if( oldVal == item){
                $('#IMenuItem' +  oldVal).removeClass('active');
                $scope.$parent.selectedType = -1;
            }else{
                if(oldVal > -1){
                    $('#IMenuItem' +  oldVal).removeClass('active');
                }
                $scope.$parent.selectedType = item;
                $scope.iPost.selectedType = $scope.selectedType;

                $('#IMenuItem' +  item).addClass('active');
            }
        }
    }

    $scope.onClickOnNewPost = function() {
        //$location.path('/create-need/2');
        var validPanel = true;
        $scope.iPost.firstattempt = false;

        //Title mustn't be empty
        if($scope.iPost.title.length < 1){
            $scope.iNewPost.iPostTitle.$invalid = true;
            validPanel = false;
        }

        if($scope.iPost.selectedType < 0){
            validPanel = false;
        }

        if ($scope.iNewPost.$valid && validPanel) {
            //userService.registerUser($scope.registerUser).then(onRegisterResponse);

            $location.url('/create-need/1/'+$scope.iPost.selectedType+'/'+$scope.iPost.title);
        }
    }

    /*Others panel*/
    $scope.othersMenu = {item0 : 'want to have something', item1 : 'offer something', item2 : 'want to do something together', item3 : 'want to change something'};
    $scope.othersMenuValidationMessage = 'Please choose type of post';
    $scope.othersPostValidationMessage = 'Please enter a title for your post';
    $scope.othersChooseTypeText = 'Choose type of post';
    $scope.othersSearchText = 'Type what you want to find';
    $scope.othersSearchBtnText = 'Search';
    $scope.othersPost = new function(){
        this.reset = function() {
            if(this.selectedType > -1){
                $('#othersMenuItem' + this.selectedType).removeClass('active');
            }
            this.searchText = '';
            this.selectedType = -1;
            this.firstattempt = true;
        }

        this.reset();
    };
    $scope.onClickOthersMenuItem = function(item) {
        if(item > -1){
            if($scope.othersPost.selectedType == item){
                $('#othersMenuItem' + $scope.othersPost.selectedType).removeClass('active');
                $scope.othersPost.selectedType = -1;
            }else{
                if($scope.othersPost.selectedType > -1){
                    $('#othersMenuItem' + $scope.othersPost.selectedType).removeClass('active');
                }
                $scope.othersPost.selectedType = item;
                $('#othersMenuItem' + $scope.othersPost.selectedType).addClass('active');
            }
        }
    }
    $scope.onClickOnNewSearch = function() {
        var validPanel = true;
        $scope.othersPost.firstattempt = false;

        //Title mustn't be empty
        if($scope.othersPost.searchText.length < 1){
            $scope.otherNewSearch.otherSearchText.$invalid = true;
            validPanel = false;
        }

        if($scope.othersPost.selectedType < 0){
            validPanel = false;
        }

        if ($scope.otherNewSearch.$valid && validPanel) {
            //userService.registerUser($scope.registerUser).then(onRegisterResponse);
            $location.url('/search');
        }
    }

});

angular.module('won.owner').controller('SignInCtrl', function ($scope,$route,$window,$location,$http, applicationStateService, userService) {

	$scope.user = { //TODO yet another user object. We need to refactor this so there's only one of these (e.g. in user-service) and all calls go directly to that
		username:'',
		password:''
	};

	$scope.error = '';


    //TODO move to userService.login
	onLoginResponse = function(response) {
		if (response.status == "OK") {
            $location.url('/postbox');
		} else if (response.status == "ERROR") {
			$scope.error = response.message;
		} else {
            $log.debug(response.messsage);
        }
	}

	$scope.onClickSignIn = function () {
		$scope.error = '';
		if($scope.signInForm.$valid) {
			userService.logIn($scope.user).then(onLoginResponse);
		}
	}


});

angular.module('won.owner').controller('RegisterCtrl', function ($scope, $route, $window, $location, userService) {

	$scope.registerUser = new function(){
		this.reset = function() {
			this.username = '';
			this.password = '';
			this.passwordAgain = '';
		}

		this.isSamePassword = function() {
			return (this.password == this.passwordAgain);
		}

		this.reset();
	};

    $scope.error = '';
	$scope.success = '';
    $scope.registered = false;
    $scope.mail_s = false;

    onLoginSuccessful = function() {
        $location.url("/postbox");
    }

    onLoginError = function(response) {
        $scope.error = response.message;
    }

    onRegisterResponse = function(response) {
        if (response.status == "OK") {
            $scope.error = '';
            $scope.success = '';
            angular.resetForm($scope, "registerForm");
            $scope.registered = true;
            userService.logIn($scope.registerUser).then(onLoginSuccessful, onLoginError);
        } else if (response.status == "ERROR") {
            $scope.error = response.message;
        } else {
            $log.debug(response.messsage);
        }
    }

	$scope.onClickRegister = function () {
		var validPass = true;
		if(!$scope.registerUser.isSamePassword()) {
			//$scope.error = 'Filled in passwords should be same';
			validPass = false;
		}

        //Password must be grate or equal 6
        if($scope.registerUser.password.length < 6){
            $scope.registerForm.registerPassword.$invalid = true;
            validPass = false;
        }

		if ($scope.registerForm.$valid && validPass) {
//			userService.registerUser($scope.registerUser)
//                .success(onRegisterSuccessful)
//                .error(onRegisterError);
            userService.registerUser($scope.registerUser).then(onRegisterResponse);
		}
	}

    $scope.cancelRegister = function() {
        $scope.forms.register = false;
        // TODO breaks the view (the url doesn't change, the sign-up button is still pressed)
        // // since this fn executes async in a future turn of the event loop, we need to wrap
        // // our code into an $apply call so that the model changes are properly observed.
        //scope.$apply(function() {
    }

});

