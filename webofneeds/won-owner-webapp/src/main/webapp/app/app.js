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

app = angular.module('won.owner',
        [ 'ui.bootstrap'
        , 'ngRoute'
        , 'blueimp.fileupload'
        , 'ngMockE2E'
        , 'smart-table'
        , 'ngScrollbar'
        , 'scrollable-table'
        , 'ui.utils'
        //, 'ui.event'
        //, 'ui.keypress'
        ]
    ).config(function ($routeProvider, $httpProvider, $provide) {
        $httpProvider.responseInterceptors.push('redirectInterceptor');
	    $routeProvider.
            when('/create-need/:step/:selectedType/:title', {controller : 'CreateNeedCtrlNew', templateUrl:'app/create-need/create-need.html',title:'Create Need'}).
            when('/create-need/:step/:selectedType', {controller : 'CreateNeedCtrlNew', templateUrl:'app/create-need/create-need.html',title:'Create Need'}).
            when('/create-need/:step', {controller : 'CreateNeedCtrlNew', templateUrl:'app/create-need/create-need.html',title:'Create Need'}).
			when('/', {controller : 'HomeCtrl', templateUrl:'app/home/welcome.html',title:'Welcome'}).
            when('/home', {controller : 'HomeCtrl', templateUrl: 'app/home/home.partial.html',title:'Home'}).
			when('/signin', {controller:'HomeCtrl', templateUrl:'app/home/home.partial.html',title:'Sign in'}).
			when('/register', {controller:'HomeCtrl', templateUrl:'app/home/home.partial.html',title:'Register'}).
            when('/need-list', {controller : 'NeedListCtrl', templateUrl:'app/need-list/need-list.partial.html',title:'Post List'}).
			//when('/need-detail/:needId', {controller:'NeedDetailCtrl', templateUrl:'app/need-detail/need-detail.partial.html'}).
            when('/why-use', {controller:'WhyUseCtrl', templateUrl:'app/why-use/why-use.html', title:'Why use Web of Needs?'}).
            when('/impressum', {controller:'ImpressumCtrl', templateUrl:'app/impressum/impressum.html',title:'Impressum'}).
            when('/search/:type/:term', {controller:'SearchCtrl', templateUrl:'app/search/search.html',title:'Search'}).
            when('/faq', {controller:'FaqCtrl', templateUrl:'app/faq/faq.html',title:'FAQ'}).
            when('/forgot-pwd', {controller:'ForgotPwdCtrl', templateUrl:'app/forgot-pwd/forgot-pwd.html',title:'Forgot Password'}).
            when('/new-pwd', {controller:'EnterNewPwdCtrl', templateUrl:'app/forgot-pwd/enter-new-pwd.html',title:'New Password'}).
            when('/postbox', {controller:'PostBoxCtrl', templateUrl:'app/postbox/postbox.html',title:'Postbox'}).
            when('/private-link', {controller:'PrivateLinkCtrl', templateUrl:'app/private-link/private-link.html',title:'Private Link'}).
            when('/post-detail', {controller:'PostDetailCtrl', templateUrl:'app/post-detail/post-detail.html', title:'Post Detail'}).
			otherwise({redirectTo : '/home'});
    //TODO add access control / error handling (e.g. trying to bring up /postbox while not logged in)
    app.directive('header', function(){
        return {
            restrict: 'A',
            replace: true,
            templateUrl:'templates/header.html'
        }
})

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
})

    .filter('orderObjectBy', function() {
        return function(items, field, reverse) {
            var filtered = [];
            var path = field.split(".");
            angular.forEach(items, function(item) {
                filtered.push(item);
            });
            //filtered.sort(function (a, b) {
            //    return (a[field] > b[field] ? 1 : -1);
            //});
            var getFieldValue = function(a, path) {
                var value = a;
                var i;
                for (i = 0; i < path.length; i++) {
                    if (value[path[i]] == undefined) {
                        return undefined;
                    } else {
                        value = value[path[i]];
                    }
                }
                return value;
            }
            filtered.sort(function (a, b) {
                var avalue = getFieldValue(a, path);
                var bvalue = getFieldValue(b, path);
                return (avalue > bvalue ? 1 : -1);
            });
            if(reverse) {
                filtered.reverse();
            }
            return filtered;
        };
    })


    .filter('messageTypeFilter', function(){
        var getTypeText = function(lastConEvent) {
            switch (lastConEvent.event.hasMessageType) {
                case won.WONMSG.connectionMessageSentMessage:
                    return 'Conversation';
                case won.WONMSG.connectionMessage:
                    return 'Conversation';
                case won.WONMSG.openMessage:
                    //if (lastConEvent.event.hasReceiverNeed == lastConEvent.remoteNeed.uri){
                        return 'Conversation';
                    //}
                    //return 'Incoming Request';
                case won.WONMSG.connectMessage:
                    if (lastConEvent.event.hasReceiverNeed == lastConEvent.remoteNeed.uri){
                        return 'Outgoing Request';
                    }
                    return 'Incoming Request';
                case won.WONMSG.connectSentMessage:
                    if (lastConEvent.event.hasReceiverNeed == lastConEvent.remoteNeed.uri){
                        return 'Outgoing Request';
                    }
                    return 'Incoming Request';
                case won.WONMSG.hintMessage:
                    return 'Matches';
                case won.WONMSG.closeMessage:
                    if (lastConEvent.event.hasReceiverNeed == lastConEvent.remoteNeed.uri){
                        return 'Outgoing Closed';
                    }
                    return 'Incoming Closed';
                default:
                    return 'Unknown';
            }
        }


        return function(inputArray, eventTypes){
            if (inputArray == null) return null;
            if (! angular.isArray(inputArray)) return null;
            if (inputArray.length == 0) return [];
            var outputArray = [];
            //var filter= [won.Won.OwnerOpenCompacted, won.WON.PartnerOpenCompacted, won.WON.PartnerMessageCompacted, won.WON.OwnerMessageCompacted, won.WON.HintCompacted];

            for(var i = 0; i < inputArray.length; i++) {
                var item = inputArray[i];
                if (item != null) {
                    if (eventTypes.indexOf(item.event.hasMessageType) != -1) {
                        item.id = i;
                        item.typeText = getTypeText(item);
                        outputArray.push(item);
                    }
                }
            }

            return outputArray;
        }
    }
);

app.run(function($httpBackend,$rootScope ){

        //$httpBackend.whenGET('/owner/rest/need/\d+').respond('test');
        $httpBackend.whenPOST('/owner/rest/needs/').passThrough();
        $httpBackend.whenGET('/owner/rest/needs/').passThrough();
        $httpBackend.whenGET('/owner/rest/needs/drafts/').passThrough();
        $httpBackend.whenPOST('/owner/rest/needs/drafts').passThrough();
        $httpBackend.whenDELETE('/owner/rest/needs/drafts').passThrough();
        $httpBackend.whenGET(/\/owner\/rest\/needs\/drafts\/.+/).passThrough();
        $httpBackend.whenDELETE(/\/owner\/rest\/needs\/drafts\/.+/).passThrough();
        $httpBackend.whenPOST('/owner/rest/need/create/saveDraft').passThrough();
        $httpBackend.whenGET(/.*/).passThrough();
        $httpBackend.whenPOST(/.*/).passThrough();
       $httpBackend.whenPOST(/\/owner\/rest\/user\/.*/).passThrough();
        $httpBackend.whenPOST('/owner').passThrough();

        $httpBackend.whenGET('/').passThrough();
        $httpBackend.whenGET('/home').passThrough();
        $httpBackend.whenGET('/signin').passThrough();
        $httpBackend.whenGET('/register').passThrough();
        $httpBackend.whenGET('/create-need').passThrough();
        $httpBackend.whenGET('/search').passThrough();
        $httpBackend.whenGET('/need-list').passThrough();
        $httpBackend.whenGET('/need-detail/:needId').passThrough();
        $httpBackend.whenGET('app/home/home.partial.html').passThrough();
        $httpBackend.whenGET('app/home/home.partial.html').passThrough();
       // $httpBackend.whenGET(/app\/.*/).passThrough();
       // $httpBackend.whenGET('/app.*/').passThrough();
    }
);
app.factory('redirectInterceptor', ['$location', '$q', function($location, $q) { return function(promise) {
    promise.then(
        function(response) {


            return response;
        },
        function(response) {
            if (response.status == 401 && $location.path()=="/postbox") {

                $location.url("/signin");
            }
            return $q.reject(response);
        }
    );
    return promise;
};
}]);
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
app.config(['$logProvider', function($logProvider){
    $logProvider.debugEnabled(true);
}])
