
app = angular.module('owner', ['owner.home', 'owner.header', 'owner.createneed', 'owner.needdetail', 'owner.needlist', 'owner.service.user']).config(function ($routeProvider, $httpProvider) {
	$routeProvider.
			when('/', {controller : 'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
			when('/signin', {controller:'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
			when('/register', {controller:'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
			when('/create-need', {controller : 'CreateNeedCtrl', templateUrl:'app/create-need/create-need.partial.html'}).
			when('/need-list', {controller : 'NeedListCtrl', templateUrl:'app/need-list/need-list.partial.html'}).
			when('/need-detail/:needId', {controller:'NeedDetailCtrl', templateUrl:'app/need-detail/need-detail.partial.html'}).
			otherwise({redirectTo : '/'});

	var interceptor = function ($rootScope, $q, $location) {

		function success(response) {
			return response;
		}

		function error(response) {

			var status = response.status;
			var config = response.config;
			var method = config.method;
			var url = config.url;

			if (status == 401) {
				$location.path("/");
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
