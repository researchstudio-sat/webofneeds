/**
 * Created by ksinger on 02.06.2015.
 */


/*

ways to implement the part on the...

...client:

* use fileReader: https://developer.mozilla.org/en/docs/Web/API/FileReader
* angulars $http
* there are a few angular-directives for this, that i have to evaluate
* source of http://download.tizen.org/misc/examples/w3c_html5/communication/xmlhttprequest_level_2/xhr1.html using vanilla XHR

...server:

* There's the class RestNeedPhotoController that already allows uploading images but probably needs some work.
* A server-side snippet for spring can be found at https://spring.io/guides/gs/uploading-files/

 */

//angular.module('app', ['flux'])
angular.module('won.owner')
    .directive('imageUploader', function factory($log, utilService) {
        return {
            restrict: 'E',
            //controllerAs: 'myComponent',
            scope: {},
            template: '\
            <h1>Image upload isn\'t fully implemented yet.</h1>\
            <!--<input type="file"/>-->\
            <input name="fooUpload" scope="{{contentOptions.currentValue}}" key="value" type="file" \
                   accept="image/*"\
                   onchange="angular.element(this).scope().setFile(this.files[0])"> \
            <img ng-show="imageData" src="{{imageData}}" alt="The image you just picked."/>\
            ',
            controller: function ($scope){ //, MyStore) {
                $scope.setFile = function(file){
                    //TODO only accept images & limit their size !!!!!!!!!!!!!!! (limit on server-side)

                    //https://developer.mozilla.org/en-US/docs/Web/API/FileReader/readAsDataURL
                    //http://stackoverflow.com/questions/25811693/angularjs-promise-not-resolving-file-with-filereader

                    if(!/^image/.test(file.type)) {
                        //TODO trigger error notification to please pick an image
                        return;
                    }
                    utilService.readAsDataURL(file).then(function(fileData){
                        $log.debug("image-uploader.js - readAsDataURL of " + file.name + ": "  + fileData.substring(0, 50) + "(...)");
                        $scope.imageData = fileData;
                    });


                    /*var xhr = new XMLHttpRequest();
                    var formData = new FormData();
                    // add the file intended to be upload to the created FormData instance
                    formData.append("upload", file);

                    xhr.open("post", "/upload", true);
                    xhr.setRequestHeader("Content-Type", "multipart/form-data");
                    xhr.onreadystatechange = function () {
                        if (xhr.readyState == 4 && xhr.status == 200) {
                            alert(xhr.statusText);
                        }
                    }
                    xhr.send(formData);  // send formData to the server using XHRar formData = new FormData();
                    */
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