;

import angular from 'angular';
import labelledHrModule from '../labelled-hr';
import overviewTitleBarModule from '../visitor-title-bar';

class LandingpageController {
    constructor() {}
}

LandingpageController.$inject = [];

export default angular.module('won.owner.components.landingpage', [
    overviewTitleBarModule,
    labelledHrModule
])
    .controller('LandingpageController', LandingpageController)
    .name;

