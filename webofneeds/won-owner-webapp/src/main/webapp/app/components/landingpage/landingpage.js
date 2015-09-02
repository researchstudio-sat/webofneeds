;

import angular from 'angular';
import overviewTitleBarModule from '../owner-title-bar';

class LandingPageController {
    constructor() {}

}

LandingPageController.$inject = [];

export default angular.module('won.owner.components.landingpage', [
    overviewTitleBarModule
])
    .controller('LandingPageController', LandingPageController)
    .name;

