;

import angular from 'angular';

function genComponentConf() {
    let template = `
                <img class="image" ng-show="self.src" ng-src="{{self.src}}"/>
                <div class="image" style="background-color: {{self.generateHexColor()}}" ng-show="!self.src"><!-- figure out some better way to color -->
                    <span class="image__noimage">{{self.generateTitleCharacter()}}</span>
                </div>`;

    class Controller {
        constructor() {}

        generateHexColor(title) {
            var hash = 0;
            if(this.title){
                for (var i = 0; i < this.title.length; i++) {
                    hash = this.title.charCodeAt(i) + ((hash << 5) - hash);
                }
            }

            var c = (hash & 0x00FFFFFF)
                .toString(16);

            return "#"+("00000".substring(0, 6 - c.length) + c);
        }

        generateTitleCharacter() {
            if(this.title)
                return this.title.charAt(0).toUpperCase();
            else
                return undefined;
        }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {src: "=",
                title: "="},
        template: template
    }
}

export default angular.module('won.owner.components.squareImage', [])
    .directive('wonSquareImage', genComponentConf)
    .name;

