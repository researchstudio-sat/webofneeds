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
    .directive('imagePicker', function factory($log, utilService) {
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
                   onchange="angular.element(this).scope().setFile(this.files[0])"> \
            <img ng-show="imageDataURL" src="{{imageDataURL}}" alt="The image you just picked."/>\
            <!--<img ng-show="imageDataURL" src="{{imageUrl}}" alt="The image you just picked."/>-->\
            ',
            link: function (scope, element, attr){ //, MyStore) {

                /*
                TODO move gallery to seperate directive (that pulls it's infos from the store)
                 */
                scope.setFile = function(file){
                    //TODO only accept images & limit their size !!!!!!!!!!!!!!! (limit on server-side)

                    //https://developer.mozilla.org/en-US/docs/Web/API/FileReader/readAsDataURL
                    //http://stackoverflow.com/questions/25811693/angularjs-promise-not-resolving-file-with-filereader

                    if (!/^image\//.test(file.type)) {
                        //TODO trigger error notification to please pick images only!
                        return;
                    }
                    console.log('image-uploader.js - image-handle: ', file);

                    utilService.readAsDataURL(file).then(function (dataURL) {
                        console.log("image-uploader.js - readAsDataURL - " +
                            + dataURL.length + ": " + dataURL.substring(0, 50) + "(...)");
                        scope.imageDataURL = dataURL;

                        var b64data = dataURL.split('base64,')[1];
                        //window.dataURL=fileData;
                        console.log("image-uploader.js - readAsDataURL - q4235 ", file.type);
                        console.log("image-uploader.js - readAsDataURL - wrtyw ", file.name);
                        //console.log("image-uploader.js - readAsDataURL - qvqwe ", b64);
                        scope.onImagesPicked([
                            {
                                name: file.name,
                                type: file.type,
                                data: b64data
                            }
                        ]);
                    });
                }
            }
    };
});

