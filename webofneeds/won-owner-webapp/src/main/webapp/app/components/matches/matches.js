/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import needTitleBarModule from '../need-title-bar';

class MatchesController {
    constructor() {}

}

MatchesController.$inject = [];

export default angular.module('won.owner.components.matches', [
        needTitleBarModule
    ])
    .controller('MatchesController', MatchesController)
    .name;
