app = angular.module('won.owner', ['ui.bootstrap', 'ui.map', 'blueimp.fileupload']).config(function ($routeProvider, $httpProvider, $provide) {
	$routeProvider.
			when('/', {controller : 'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
			when('/signin', {controller:'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
			when('/register', {controller:'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
			when('/create-need', {controller : 'CreateNeedCtrl', templateUrl:'app/create-need/create-need.partial.html'}).
			when('/need-list', {controller : 'NeedListCtrl', templateUrl:'app/need-list/need-list.partial.html'}).
			when('/need-detail/:needId', {controller:'NeedDetailCtrl', templateUrl:'app/need-detail/need-detail.partial.html'}).
			otherwise({redirectTo : '/'});

app.directive('header', function(){
    return {
        restrict: 'A',
        replace: true,
        tempalteUrl:'templates/header.html'
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
