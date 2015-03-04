/**
 * Created by ksinger on 18.02.2015.
 */

;
angular.module('won.owner').controller('SignInModalInstanceCtrl', function ($scope, $modalInstance) {
    $scope.testvar = "42";

    $scope.ok = function () {
      $modalInstance.close();
    };

    $scope.cancel = function () {
        $modalInstance.dismiss('cancel');
    };
});
