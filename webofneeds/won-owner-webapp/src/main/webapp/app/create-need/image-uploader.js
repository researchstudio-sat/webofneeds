/**
 * Created by ksinger on 02.06.2015.
 */


/*

ways to do this:

* use fileReader: https://developer.mozilla.org/en/docs/Web/API/FileReader
* angulars $http
* there are a few angular-directives for this, that i have to evaluate

* source of http://download.tizen.org/misc/examples/w3c_html5/communication/xmlhttprequest_level_2/xhr1.html using vanilla XHR

 */

//angular.module('app', ['flux'])
angular.module('won.owner')
    .directive('imageUploader', function factory($log) {
        return {
            restrict: 'E',
            //controllerAs: 'myComponent',
            scope: {},
            template: '\
            <h1>Image upload isn\'t fully implemented yet.</h1>\
            <!--<input type="file"/>-->\
            <input name="fooUpload" scope="{{contentOptions.currentValue}}" key="value" type="file" onchange="angular.element(this).scope().setFile(this)">\
            ',
            controller: function ($scope){ //, MyStore) {

                $scope.setFile = function(fileInput){
                    //TODO only accept images & limit their size !!!!!!!!!!!!!!!
                    console.log(fileInput.files[0]);

                    var xhr = new XMLHttpRequest();
                    var formData = new FormData();
                    // add the file intended to be upload to the created FormData instance
                    formData.append("upload", fileInput.files[0]);

                    xhr.open("post", "/upload", true);
                    xhr.setRequestHeader("Content-Type", "multipart/form-data");
                    xhr.onreadystatechange = function () {
                        if (xhr.readyState == 4 && xhr.status == 200) {
                            alert(xhr.statusText);
                        }
                    }
                    xhr.send(formData);  // send formData to the server using XHRar formData = new FormData();
                }


                /*
                $http.put(

                 'http://localhost:5984/demo/d06917e8d1fae1ae162ea7773c003f0b/' + file.name + '?rev=4-c10029f35a5c5ed9bd8cc31bf8589d3c',
                 file,
                 { headers: { 'Content-Type' : file.type } });
                */
            }
    };
});

/*
from https://github.com/angular/angular.js/issues/1375 :
 return {
 restrict: 'E',
 template: '<input type="file" />',
 replace: true,
 require: 'ngModel',
 link: function(scope, element, attr, ctrl) {
 var listener = function() {
 scope.$apply(function() {
 attr.multiple ? ctrl.$setViewValue(element[0].files) : ctrl.$setViewValue(element[0].files[0]);
 });
 }
 element.bind('change', listener);
 }
 }

 <file name="image" ng-model="inputFile" accept="image/png,image/jpg,image/jpeg" />


 var file = $scope.inputFile;
 */