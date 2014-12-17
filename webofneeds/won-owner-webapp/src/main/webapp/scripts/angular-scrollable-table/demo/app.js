/* global angular:false */
'use strict';

var myApp = angular.module('myApp',['scrollable-table'])
.service('Data', function() {
    this.get = function() {
//        var res = [],
//            size = 1000;
//        for(var i = 0;i < size; i++){
//            res.push({
//               facility: 'facility_' + i,
//               code: 'C-RD34_' + Math.ceil(Math.random() * 10),
//               cost: Math.ceil(Math.random() * 100),
//               conditionRating: Math.ceil(Math.random() * 1000),
//               extent: Math.ceil(Math.random() * 50),
//               planYear: Math.ceil(Math.random() * 1000)
//            });
//        }
//        return res;
        return [{
          facility: "Atlanta",
          code: "C-RD34",
          cost: 540000,
          conditionRating: 52,
          extent: 100,
          planYear: 2014
        }, {
          facility: "Seattle",
          code: "CRDm-4",
          cost: 23000,
          conditionRating: 40,
          extent: 88,
          planYear: 2014
        }, {
          facility: "Austin",
          code: "GR-5",
          cost: 1200000,
          conditionRating: 92,
          extent: 90,
          planYear: 2014
        }, {
          facility: "Dayton",
          code: "LY-7",
          cost: 123000,
          conditionRating: 71,
          extent: 98,
          planYear: 2014
        }, {
          facility: "Portland",
          code: "Dm-4",
          cost: 149000,
          conditionRating: 89,
          extent: 77,
          planYear: 2014
        }, {
          facility: "Dallas",
          code: "AW-3",
          cost: 14000,
          conditionRating: 89,
          extent: 79,
          planYear: 2014
        }, {
          facility: "Houston",
          code: "Dm-4",
          cost: 1100000,
          conditionRating: 93,
          extent: 79,
          planYear: 2014
        }, {
          facility: "Boston",
          code: "DD3",
          cost: 1940000,
          conditionRating: 86,
          extent: 80,
          planYear: 2015
        }, {
          facility: "New York",
          code: "ER1",
          cost: 910000,
          conditionRating: 87,
          extent: 82,
          planYear: 2015
        }];
    };
})
// when sorting by year, sort by year and then replace %
.service("Comparators", function() { 
  this.year = function(r1, r2) {
    if(r1.planYear === r2.planYear) {
      if (r1.extent === r2.extent) return 0;
      return r1.extent > r2.extent ? 1 : -1;
    } else if(!r1.planYear || !r2.planYear) {
      return !r1.planYear && !r2.planYear ? 0 : (!r1.planYear ? 1 : -1);
    }
    return r1.planYear > r2.planYear ? 1 : -1;
  };
})
.controller('MyCtrl', function($scope, $timeout, $window, Data, Comparators) {
    $scope.visibleProjects = Data.get();
    $scope.comparator = Comparators.year;
    $scope.facilities = [];
    for(var i = 0; i < $scope.visibleProjects.length; i++) {
        $scope.facilities.push($scope.visibleProjects[i].facility);
    }
    
    $scope.$watch('selected', function(fac) {
       $scope.$broadcast("rowSelected", fac);
    });

    $scope.changeRecord = function(){
        $scope.visibleProjects[3].code = 'aaabbbccc';
    };

    $scope.replaceRecords = function(){
        $scope.visibleProjects = Data.get();
    };

    $scope.toggleCol = function() {
      $scope.toggleColumn = !$scope.toggleColumn;
      $scope.$broadcast("renderScrollableTable");
    };
})
;