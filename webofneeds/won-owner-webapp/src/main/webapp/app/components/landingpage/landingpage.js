;

import angular from 'angular';
import topNavModule from '../topnav';
import createPostModule from '../create-post';
import {
    attach,
} from '../../utils';
import { actionCreators }  from '../../actions/actions';

import * as srefUtils from '../../sref-utils';

const serviceDependencies = ['$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];

class LandingpageController {
    constructor(/* arguments <- serviceDependencies */){
        attach(this, serviceDependencies, arguments);
        Object.assign(this, srefUtils); // bind srefUtils to scope

        window.lp4dbg = this;

        const self = this;

        const select = (state) => ({ });

        const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
        this.$scope.$on('$destroy',disconnect);
    }
}

export default angular.module('won.owner.components.landingpage', [
    topNavModule,
    createPostModule,
])
    .controller('LandingpageController', [...serviceDependencies, LandingpageController])
    .name;

