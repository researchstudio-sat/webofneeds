;

import angular from 'angular';
import topNavModule from '../topnav';
import labelledHrModule from '../labelled-hr';
import overviewTitleBarModule from '../visitor-title-bar';
import accordionModule from '../accordion';

class LandingpageController {
    constructor() {
        this.questions = [{title: "Blablabla1", detail: "1blablabla"},
            {title: "Blablabla2", detail: "2blablabla"},
            {title: "Blablabla3", detail: "3blablabla"},
            {title: "Blablabla4", detail: "4blablabla"},
            {title: "Blablabla5", detail: "5blablabla"}];
    }
}

LandingpageController.$inject = [];

export default angular.module('won.owner.components.landingpage', [
    overviewTitleBarModule,
    labelledHrModule,
    accordionModule,
    topNavModule
])
    .controller('LandingpageController', LandingpageController)
    .name;

