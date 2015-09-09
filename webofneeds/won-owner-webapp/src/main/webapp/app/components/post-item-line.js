;

import angular from 'angular';

function genComponentConf() {
    let template = `
            <div>{{self.item.name}}</div>
    `;

    class Controller {
        constructor() { }
    }

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {item: "="},
        template: template
    }
}

export default angular.module('won.owner.components.postItemLine', [])
    .directive('wonPostItemLine', genComponentConf)
    .name;

