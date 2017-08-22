/**
 * Created by ksinger on 21.08.2017.
 */
;

import angular from 'angular';
import {
    attach,
} from '../../utils';
import {
    actionCreators
}  from '../../actions/actions';

import createNeedTitleBarModule from '../create-need-title-bar';

import topNavModule from '../topnav';

import * as srefUtils from '../../sref-utils';

const serviceDependencies = ['$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];

class SignupController {

    constructor(/* arguments <- serviceDependencies */) {
        attach(this, serviceDependencies, arguments);
        Object.assign(this, srefUtils); // bind srefUtils to scope
        const self = this;

        const select = (state) => ({
            //focusSignup: state.getIn(['router', 'currentParams', 'focusSignup']) === "true",
            loggedIn: state.getIn(['user','loggedIn']),
            registerError: state.getIn(['user','registerError'])
        });
        const disconnect = this.$ngRedux.connect(select, actionCreators)(this);
    }

    formKeyup(event) {
        this.registerReset();
        if(
            event.keyCode == 13 &&
            this.passwordAgain === this.password
        ) {
            this.register({email: this.email, password: this.password})
        }
    }
}

export default angular.module('won.owner.components.signup', [
    //overviewTitleBarModule,
    //accordionModule,
    createNeedTitleBarModule,
    topNavModule,
    //flexGridModule,
    //compareToModule,
])
    .controller('SignupController', [...serviceDependencies, SignupController])
    .name;
