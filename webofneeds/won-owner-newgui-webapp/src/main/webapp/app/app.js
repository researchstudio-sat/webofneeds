
app = angular.module('owner', ['owner.home', 'owner.header', 'owner.createneed', 'owner.needdetail', 'owner.needlist']).config(function ($routeProvider, $httpProvider) {
	$routeProvider.
			when('/', {controller : 'HomeCtrl', templateUrl:'app/home/home.partial.html'}).
			when('/create-need', {controller : 'CreateNeedCtrl', templateUrl:'app/create-need/create-need.partial.html'}).
			when('/need-list', {controller : 'NeedListCtrl', templateUrl:'app/need-list/need-list.partial.html'}).
			when('/need-detail/:needId', {controller:'NeedDetailCtrl', templateUrl:'app/need-detail/need-detail.partial.html'}).
			otherwise({redirectTo : '/'});
});
