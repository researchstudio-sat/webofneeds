;

import angular from 'angular';

import {
    generateHexColor,
    generateTitleCharacter,
} from '../utils.js'

function genComponentConf() {
    let template = `
                <img class="image" ng-show="self.src" ng-src="{{self.src}}"/>
                <div title="{{self.title}}" class="image" style="background-color: {{self.generateHexColor(self.uri)}}" ng-show="!self.src"><!-- figure out some better way to color -->
                    <span class="image__noimage">{{self.generateTitleCharacter(self.title)}}</span>
                </div>`;

    class Controller {
        constructor() {
            this.generateHexColor = generateHexColor;
            this.generateTitleCharacter = generateTitleCharacter;
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

