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

app = angular.module('won.owner', ['ui.bootstrap', 'ui.map', 'blueimp.fileupload', 'ngMockE2E']).config(function ($routeProvider, $httpProvider, $provide) {
	$routeProvider.
			when('/', {controller : 'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
			when('/signin', {controller:'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
			when('/register', {controller:'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
            when('/create-need/:step', {controller : 'CreateNeedCtrlNew', templateUrl:'app/create-need/create-need.html'}).
            when('/need-list', {controller : 'NeedListCtrl', templateUrl:'app/need-list/need-list.partial.html'}).
			when('/need-detail/:needId', {controller:'NeedDetailCtrl', templateUrl:'app/need-detail/need-detail.partial.html'}).
            when('/why-use', {controller:'WhyUseCtrl', templateUrl:'app/why-use/why-use.html'}).
            when('/impressum', {controller:'ImpressumCtrl', templateUrl:'app/impressum/impressum.html'}).
            when('/search', {controller:'SearchCtrl', templateUrl:'app/search/search.html'}).
            when('/faq', {controller:'FaqCtrl', templateUrl:'app/faq/faq.html'}).
            when('/forgot-pwd', {controller:'ForgotPwdCtrl', templateUrl:'app/forgot-pwd/forgot-pwd.html'}).
            when('/new-pwd', {controller:'EnterNewPwdCtrl', templateUrl:'app/forgot-pwd/enter-new-pwd.html'}).
            when('/postbox', {controller:'PostBoxCtrl', templateUrl:'app/postbox/postbox.html'}).
            when('/private-link', {controller:'PrivateLinkCtrl', templateUrl:'app/private-link/private-link.html'}).
            when('/post-detail', {controller:'PostDetailCtrl', templateUrl:'app/post-detail/post-detail.html'}).
			otherwise({redirectTo : '/'});

app.directive('header', function(){
    return {
        restrict: 'A',
        replace: true,
        templateUrl:'templates/header.html'
    }
})
	var interceptor = function ($rootScope, $q, $location, $window) {

		function success(response) {
			return response;
		}

		function error(response) {

			var status = response.status;
			var config = response.config;
			var method = config.method;
			var url = config.url;

			if (status == 401) {
				$window.user = {
					isAuth:false
				}

				$location.path("/signin");
			} else {
				$rootScope.error = method + " on " + url + " failed with status " + status;
			}

			return $q.reject(response);
		}

		return function (promise) {
			return promise.then(success, error);
		};
	};
	$httpProvider.responseInterceptors.push(interceptor);
   // $provide.decorator('$httpBackend', angular.mock.e2e.$httpBackendDecorator);
	/* http://stackoverflow.com/questions/18888104/angularjs-q-wait-for-all-even-when-1-rejected */
	$provide.decorator('$q', ['$delegate', function ($delegate) {
		var $q = $delegate;

		// Extention for q
		$q.allSettled = $q.allSettled || function (promises) {
			var deferred = $q.defer();
			if (angular.isArray(promises)) {
				var states = [];
				var results = [];
				var didAPromiseFail = false;

				// First create an array for all promises setting their state to false (not completed)
				angular.forEach(promises, function (promise, key) {
					states[key] = false;
				});

				// Helper to check if all states are finished
				var checkStates = function (states, results, deferred, failed) {
					var allFinished = true;
					angular.forEach(states, function (state, key) {
						if (!state) {
							allFinished = false;
							return;
						}
					});
					if (allFinished) {
						if (failed) {
							deferred.reject(results);
						} else {
							deferred.resolve(results);
						}
					}
				}

				// Loop through the promises
				// a second loop to be sure that checkStates is called when all states are set to false first
				angular.forEach(promises, function (promise, key) {
					$q.when(promise).then(function (result) {
						states[key] = true;
						results[key] = result;
						checkStates(states, results, deferred, didAPromiseFail);
					}, function (reason) {
						states[key] = true;
						results[key] = reason;
						didAPromiseFail = true;
						checkStates(states, results, deferred, didAPromiseFail);
					});
				});
			} else {
				throw 'allSettled can only handle an array of promises (for now)';
			}

			return deferred.promise;
		};

		return $q;
	}]);
});
app.run(function($httpBackend){

        $httpBackend.whenGET('/owner/rest/need/\d+').respond('test');

        $httpBackend.whenPOST('/owner/rest/need/create').respond(function(method, url, data){

            return [200, 'text',{}];
        });
       /* $httpBackend.whenPOST('/owner/rest/need/create/saveDraft').respond(function(method, url, data){

            return [200, 'text',{}];
        });*/

        $httpBackend.whenPOST('/owner/rest/need/create/saveDraft').passThrough();
        $httpBackend.whenGET(/.*/).passThrough();
        $httpBackend.whenPOST(/.*/).passThrough();
        $httpBackend.whenPOST(/\/owner\/rest\/user\/.*/).passThrough();
        $httpBackend.whenPOST('/owner').passThrough();
        $httpBackend.whenGET('/').passThrough();
        $httpBackend.whenGET('/signin').passThrough();
        $httpBackend.whenGET('/register').passThrough();
        $httpBackend.whenGET('/create-need').passThrough();
        $httpBackend.whenGET('/search').passThrough();
        $httpBackend.whenGET('/need-list').passThrough();
        $httpBackend.whenGET('/need-detail/:needId').passThrough();
        $httpBackend.whenGET('app/home/home.partial.html').passThrough();
        $httpBackend.whenGET('app/home/home.partial.html').passThrough();
        $httpBackend.whenGET(/app\/.*/).passThrough();
        $httpBackend.whenGET('/app.*/').passThrough();
    }
);
angular.resetForm = function (scope, formName, defaults) {
	$('form[name=' + formName + '], form[name=' + formName + '] .ng-dirty').removeClass('ng-dirty').addClass('ng-pristine');
	var form = scope[formName];
	form.$dirty = false;
	form.$pristine = true;
	for (var field in form) {
		if (form[field].$pristine === false) {
			form[field].$pristine = true;
		}
		if (form[field].$dirty === true) {
			form[field].$dirty = false;
		}
	}
	for (var d in defaults) {
		scope[d] = defaults[d];
	}
};
