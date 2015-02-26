/**
 * Created by ksinger on 18.02.2015.
 */

;
angular.module('won.owner')
    .directive('signIn', function ($modal, $controller) { //$controller for testing purposes only
            console.log("In SignInDirective init");
            return {
                restrict: 'AE',
                template: 'hello sign-in',
                //templateUrl : 'app/sign-in/sign-in.html',
                //scope : {
                    /*chosenMessage: '=',
                     clickOnPostLink: '&'*/
                    /*addressCallback: '&addressSelected'*/
                //},

                //link: function(scope, element, attrs){
                link: function(scope, element, attrs, $event){
                    var c = $controller;//deleteme
                    console.log("In SignInDirective link");


                    /*$(function () {
                        $('[data-toggle="popover"]').popover()
                    });*/



                    scope.items = ['item1', 'item2', 'item3'];

                    scope.open = function (size) {
                        console.log("opening modal now");

                        var modalInstance = $modal.open({
                            //templateUrl: 'myModalContent.html',
                            //templateUrl: 'app/sign-in/sign-in-modal-content.html',
                            template: 'H-E-Y-H-O-,- -M-O-D-A-L',
                            //controller: 'ModalInstanceCtrl',
                            size: size/*,
                            resolve: {
                                items: function () {
                                    return scope.items;
                                }
                            }*/
                        });

                        /*modalInstance.result.then(function (selectedItem) {
                            $scope.selected = selectedItem;
                        }, function () {
                            $log.info('Modal dismissed at: ' + new Date());
                        });*/
                    };
                }
            };
    });

// Please note that $modalInstance represents a modal window (instance) dependency.
// It is not the same as the $modal service used above.
/*angular.module('won.owner').controller('ModalInstanceCtrl', function ($scope, $modalInstance, items) {

  $scope.items = items;
  $scope.selected = {
    item: $scope.items[0]
  };

  $scope.ok = function () {
    $modalInstance.close($scope.selected.item);
  };

  $scope.cancel = function () {
    $modalInstance.dismiss('cancel');
  };
});*/



/*
 angular.module('won.owner').controller('ModalDemoCtrl', function ($scope, $modal, $log) {

 $scope.items = ['item1', 'item2', 'item3'];

 $scope.open = function (size) {

 var modalInstance = $modal.open({
 templateUrl: 'myModalContent.html',
 controller: 'ModalInstanceCtrl',
 size: size,
 resolve: {
 items: function () {
 return $scope.items;
 }
 }
 });

 modalInstance.result.then(function (selectedItem) {
 $scope.selected = selectedItem;
 }, function () {
 $log.info('Modal dismissed at: ' + new Date());
 });
 };
 });
 */
