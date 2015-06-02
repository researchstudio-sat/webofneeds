/**
 * Created by ksinger on 02.06.2015.
 */


//angular.module('app', ['flux'])
angular.module('won.owner')
    .directive('imageUploader', function factory($log) {
        return {
            restrict: 'E',
            //controllerAs: 'myComponent',
            scope: {},
            template: '<h1>Hello Image Uploader!</h1>',
            controller: function ($scope){ //, MyStore) {
                /*
                $scope.comments = MyStore.comments;
                $scope.latestComment = MyStore.getLatestComment();
                $scope.$listenTo(MyStore, function () {
                    $scope.comments = MyStore.comments;
                    $scope.latestComment = MyStore.getLatestComment();
                });
                */
            }
    };
});
