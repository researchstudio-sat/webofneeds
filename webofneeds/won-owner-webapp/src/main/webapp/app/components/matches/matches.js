/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import overviewTitleBarModule from '../owner-title-bar';

class MatchesController {
    constructor() {}

}

MatchesController.$inject = [];

export default angular.module('won.owner.components.matches', [
        overviewTitleBarModule
    ])
    .controller('MatchesController', MatchesController)
    .name;
