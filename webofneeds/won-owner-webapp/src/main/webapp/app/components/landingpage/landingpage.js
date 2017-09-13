;

import angular from 'angular';
import topNavModule from '../topnav.js';
import createPostModule from '../create-post.js';
import {
    attach,
} from '../../utils.js';
import { actionCreators }  from '../../actions/actions.js';

import * as srefUtils from '../../sref-utils.js';

const serviceDependencies = ['$ngRedux', '$scope', '$state' /*'$routeParams' /*injections as strings here*/];

class LandingpageController {
    constructor(/* arguments <- serviceDependencies */){
        attach(this, serviceDependencies, arguments);
        Object.assign(this, srefUtils); // bind srefUtils to scope

        window.ldgp4dbg = this;

        const self = this;

        const select = (state) => ({ });

        const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
        this.$scope.$on('$destroy',disconnect);
    }

    openAboutInNewTab() {
        const url = this.$state? this.$state.href('about') : '#/about';
        window.open(url);
    }
}

export default angular.module('won.owner.components.landingpage', [
    topNavModule,
    createPostModule,
])
    .controller('LandingpageController', [...serviceDependencies, LandingpageController])
    .name;

