/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular'
import 'ng-redux';
import createPostModule from '../create-post.js';

import {
    attach,
    clone,
} from '../../utils.js';
import { actionCreators }  from '../../actions/actions.js';
import won from '../../won-es6.js';

//TODO can't inject $scope with the angular2-router, preventing redux-cleanup
const serviceDependencies = ['$ngRedux', '$scope'/*'$routeParams' /*injections as strings here*/];

class CreateNeedController {
    constructor(/* arguments <- serviceDependencies */) {
        attach(this, serviceDependencies, arguments);

        const selectFromState = (state) => {
            return {}
        };

        // Using actionCreators like this means that every action defined there is available in the template.
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }
}

export default angular.module('won.owner.components.createNeed', [
        createPostModule,
    ])
    .controller('CreateNeedController', [...serviceDependencies, CreateNeedController])
    .name;
