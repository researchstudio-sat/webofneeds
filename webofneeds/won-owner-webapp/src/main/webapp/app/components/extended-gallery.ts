/**
 * Created by ksinger on 28.08.2015.
 */

;
import angular from 'angular';

function genComponentConf() {
    let template = `
        <div class="eg__selected" ng-show="self.items">
            <img ng-src="{{self.selectedImgUrl? self.selectedImgUrl : self.items[0]}}" alt="a table"/>
        </div>
        <div class="eg__thumbs" ng-show="self.items && self.items.length > 1">
            <div class="eg__thumbs__frame clickable" ng-repeat="item in self.items track by $index" ng-click="self.showImage(item)" ng-show="$index < self.maxThumbnails">
                <img ng-src="{{item}}" alt="a combination of shelfs"/>
            </div>
            <div class="eg__thumbs__more clickable" ng-click="self.maxThumbnails = self.items.length" ng-show="self.items.length > self.maxThumbnails">
                <span>+{{self.items.length-self.maxThumbnails}}</span>
            </div>
        </div>`;

    class Controller {
        constructor() {
            /*TODO maxThumbnails is not reset to default value once it was changed*/

        }

        showImage(src){
            this.selectedImgUrl = src;
        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {maxThumbnails: "=",
                items: "="},
        template: template
    }
}

export default angular.module('won.owner.components.extendedGallery', [])
    .directive('wonExtendedGallery', genComponentConf)
    .name;
