;

import angular from 'angular';

import overviewTitleBarModule from '../overview-title-bar';
import matchesModule from  '../matches';
import topnavModule from '../topnav';

import { attach } from '../../utils';
import { actionCreators }  from '../../actions/actions';

const serviceDependencies = ['$ngRedux', '$scope'];
class OverviewMatchesController {
    constructor() {
        attach(this, serviceDependencies, arguments);

        window.omc=this;

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

