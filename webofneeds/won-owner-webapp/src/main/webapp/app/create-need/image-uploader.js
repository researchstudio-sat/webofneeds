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
    .directive('imagePicker', ['$log', 'utilService', '$q', function factory($log, utilService, $q) {
        return {
            restrict: 'E',
            //controllerAs: 'myComponent',
            scope: {
                onImagesPicked: '='
                //preselectedImages: '=?' // use api-methods in controller instead?
            },
            template: '\
            <h1>Image upload isn\'t fully implemented yet.</h1>\
            <input name="fooUpload" \
                   id="fooUpload"\
                   scope="{{contentOptions.currentValue}}" \
                   key="value" \
                   type="file" \
                   accept="image/*"\
                   multiple\
                   onchange="angular.element(this).scope().setFile(this.files)"> \
            <img ng-show="imageDataURL" src="{{imageDataURL}}" alt="The image you just picked."/>\
            <!--<img ng-show="imageDataURL" src="{{imageUrl}}" alt="The image you just picked."/>-->\
            ',
            link: function (scope, element, attr) { //, MyStore) {

                /*
                 TODO move gallery to seperate directive (that pulls it's infos from the store)
                 */
                scope.setFile = function (files) {
                    console.log('image-uploader.js:setFile asdfas;fljas;f', files);

                    //TODO only accept images & limit their size !!!!!!!!!!!!!!! (limit on server-side)

                    //https://developer.mozilla.org/en-US/docs/Web/API/FileReader/readAsDataURL
                    //http://stackoverflow.com/questions/25811693/angularjs-promise-not-resolving-file-with-filereader

                    var imageHandles = Array.prototype.filter(files, function(f) {
                        //TODO trigger error notification to please pick images only!
                        return true; //TODO: disabled check if file is image for testing purposes
                        //return /^image\//.test(f.type);
                    });

                    var imageDataPromises = imageHandles.map(function(f){
                        return utilService.readAsDataURL(f).then(function(dataUrl) {
                            scope.imageDataURL = dataUrl; //TODO display more than one image in the gui; last one wins currently
                            var b64data = dataUrl.split('base64,')[1];
                            var imageData  = {
                                name: f.name,
                                type: f.type,
                                data: b64data
                            }
                            console.log('image-uploader.js:setFile:imageData: ', imageData);
                            return imageData;
                        });
                    });

                    $q.all(imageDataPromises).then(function(imageDataArray){
                        scope.onImagesPicked(imageDataArray);
                    });
                }
            }
    };
}]);

