/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import ngAnimate from 'angular-animate';
import {
    attach,
    getIn,
} from '../../utils.js';
import won from '../../won-es6.js';
import { actionCreators }  from '../../actions/actions.js';
import sendRequestModule from '../send-request.js';
import visitorTitleBarModule from '../visitor-title-bar.js';
import {
    selectOpenPostUri,
} from '../../selectors.js';

const serviceDependencies = ['$ngRedux', '$scope'];
class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;
        window.p4dbg = this;
        this.WON = won.WON;

        const selectFromState = (state)=>{
            const postUri = selectOpenPostUri(state);
            const post = state.getIn(["needs", postUri]);

            const isOwnPost = post && post.get("ownNeed");
            return {
                postUri,
                isOwnPost: isOwnPost,
                post,
                won: won.WON,
            };
        };

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }

    goToOwnPost() {
        this.router__stateGoAbs('connections', {connectionUri: undefined, postUri: this.postUri});
        //TODO: REDIRECT TO THE OVERVIEW INSTEAD OF POST-VIEW (this does not work yet as the postUri is not set yet during the time of the function execution
    }
}

Controller.$inject = serviceDependencies;

export default angular.module('won.owner.components.post', [
    sendRequestModule,
    ngAnimate,
    visitorTitleBarModule,
])
    .controller('PostController', Controller)
    .name;
