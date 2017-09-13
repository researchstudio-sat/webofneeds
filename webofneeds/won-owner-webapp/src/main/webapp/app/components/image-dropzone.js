/**
 * Created by ksinger on 16.09.2015.
 */
;

import angular from 'angular';
import 'angular-sanitize';
import enterModule from '../directives/enter.js';
import { dispatchEvent, attach, readAsDataURL } from '../utils.js';

function genComponentConf() {
    let template = `
        <div ng-class="self.imageDataUrl? 'wid__dropzone--filled' : 'wid__dropzone--empty'"
             style="background-image: url({{self.imageDataUrl}})">
                <input type="file" accept="image/*" />
                <img src="generated/icon-sprite.svg#illu_drag_here"
                     class="wid__dropzone__default-bg">
                     <!--
                <img ng-src="{{self.imageDataUrl}}"

                     class="wid__dropzone__preview-img">
                     -->
        </div>
    `;

    const serviceDependencies = ['$scope', '$element'/*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);

            window.idc4dbg = this;

            this.imageDataUrl = undefined;

            this.$element[0].querySelector('input[type="file"]').addEventListener('change', (e) =>
                this.fileDropped(e.target.files[0])
            );
        }

        fileDropped(f) {
            if(/^image\//.test(f.type)) {
                readAsDataURL(f).then(dataUrl => {
                    this.imageDataUrl = dataUrl; //TODO where do previews go in in multi-img mode?
                    this.$scope.$digest(); // so the preview is displayed

                    var b64data = dataUrl.split('base64,')[1];
                    var imageData  = {
                        name: f.name,
                        type: f.type,
                        data: b64data
                    }
                    return imageData;
                }).then(imageData => {
                    //TODO implement multi-image mode
                    this.onImagePicked({image: imageData});
                    dispatchEvent(this.$element[0], 'image-picked', imageData);
                });
            }
        }
    }
    Controller.$inject = serviceDependencies;


    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            /*
             * Usage:
             *  on-image-picked="myCallback(image)"
             */
            onImagePicked: '&',
        },
        template: template
    }
}
export default angular.module('won.owner.components.imageDropzone', [
        enterModule,
    ])
    .directive('wonImageDropzone', genComponentConf)
    .name;
