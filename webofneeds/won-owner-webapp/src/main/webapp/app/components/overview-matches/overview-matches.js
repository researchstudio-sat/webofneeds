;

import angular from 'angular';

import overviewTitleBarModule from '../overview-title-bar.js';
import matchesModule from  '../matches.js';
import topnavModule from '../topnav.js';

import { attach } from '../../utils.js';
import { actionCreators }  from '../../actions/actions.js';

const serviceDependencies = ['$ngRedux', '$scope'];
class OverviewMatchesController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        const selectFromState = (state) => ({ });
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }
}


export default angular.module('won.owner.components.overviewMatches', [
    topnavModule,
    overviewTitleBarModule,
    matchesModule
])
    .controller('OverviewMatchesController', [...serviceDependencies, OverviewMatchesController])
    .name;

