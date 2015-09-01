/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import visitorTitleBarModule from '../visitor-title-bar';
import galleryModule from '../gallery';

class Controller {
    constructor() {}
}

Controller.$inject = [];

export default angular.module('won.owner.components.postVisitor', [
        visitorTitleBarModule,
        galleryModule
    ])
    .controller('PostVisitorController', Controller)
    .name;
