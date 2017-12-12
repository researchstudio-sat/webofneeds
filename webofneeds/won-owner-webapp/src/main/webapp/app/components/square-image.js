;

import angular from 'angular';

function genComponentConf() {
    let template = `
                <img class="image" ng-show="self.src" ng-src="{{self.src}}"/>
                <div ng-attr-title="{{self.title}}" class="image" style="background-color: {{self.generateHexColor()}}" ng-show="!self.src"><!-- figure out some better way to color -->
                    <span class="image__noimage">{{self.generateTitleCharacter()}}</span>
                </div>`;

    class Controller {
        constructor() {}

        generateHexColor(title) {
            var hash = 0;

            if(this.uri){
                for (var i = 0; i < this.uri.length; i++) {
                    hash = this.uri.charCodeAt(i) + ((hash << 5) - hash);
                }
            }

            var c = (hash & 0x00FFFFFF)
                .toString(16);

            return "#"+("00000".substring(0, 6 - c.length) + c);
        }

        generateTitleCharacter() {
            if(this.title) {
                try {
                    return this.title.charAt(0).toUpperCase();
                } catch (err) {
                    //for resilience purposes, since we can't be sure whether a need is a string or anything else
                    console.warn("Title Character could not be retrieved from: ", this.title);
                    return "?";
                }
            } else {
                return "?";
            }
        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {src: "=",
                title: "=",
                uri: "="},
        template: template
    }
}

export default angular.module('won.owner.components.squareImage', [])
    .directive('wonSquareImage', genComponentConf)
    .name;

