/**
 * Created with IntelliJ IDEA.
 * User: alexey.sidelnikov
 * Date: 8/14/14
 * Time: 11:38 AM
 * To change this template use File | Settings | File Templates.
 */
angular.module('won.owner').controller('EnterNewPwdCtrl', function ($scope, $location, userService) {
    $scope.user = {
        email:'',
        password:'',
        passwordAgain:''
    };
    $scope.sendStatus = false;

    $scope.send = function() {
        //TODO Put here logic
        if(!$scope.sendStatus)$scope.sendStatus = true;
    };

    $scope.isSamePassword = function() {
        return ($scope.user.password == $scope.user.passwordAgain);
    }
});