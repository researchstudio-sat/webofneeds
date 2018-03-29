/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import ownerTitleBarModule from '../owner-title-bar.js';
//import galleryModule from '../gallery.js';
import postMessagesModule from '../post-messages.js';
import {
    attach,
    mapToMatches,
    decodeUriComponentProperly,
    getIn,
} from '../../utils.js';
import won from '../../won-es6.js';
import { actionCreators }  from '../../actions/actions.js';
import openRequestModule from '../open-request.js';
import connectionSelectionModule from '../connection-selection.js';
import postInfoModule from '../post-info.js';
import matchesModule from '../matches.js';
import {
    selectOpenPostUri,
    selectOpenConnectionUri,
} from '../../selectors.js';

import {
   makeParams,
} from '../../configRouting.js';

const serviceDependencies = ['$ngRedux', '$scope'];
class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;
        window.p4dbg = this;
        this.wonConnected = won.WON.Connected;
        this.WON = won.WON;

        const selectFromState = (state)=>{
            const postUri = selectOpenPostUri(state);
            const post = state.getIn(["needs", postUri]);

            const hasConnections = post && post.get('connections')
                .size > 0;

            const connectionUri = selectOpenConnectionUri(state);
            const actualConnectionType = post && connectionUri && post.getIn(['connections', connectionUri, 'state']);

            const connectionTypeInParams = decodeUriComponentProperly(
                getIn(state, ['router', 'currentParams', 'connectionType'])
            );

            const sendAdHocRequest = post && !post.get("ownNeed") && getIn(state, ['router', 'currentParams', 'sendAdHocRequest']);

            const connectionIsOpen = !!connectionUri &&
                //make sure we don't get a mismatch between supposed type and actual type:
                actualConnectionType == connectionTypeInParams;

            return {
                connectionOpen: !!connectionUri,
                postUri,
                post,
                isOwnPost: post && post.get("ownNeed"),
                connectionUri,
                hasConnections,
                sendAdHocRequest,
                connectionType: connectionTypeInParams,
                actualConnectionType,
                // TODO: check if this can be shortened
                showConnectionDetails: connectionIsOpen,
                won: won.WON,
            };
        };

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }

    openConnection(connectionUri) {
        this.router__stateGoAbs('post', {
            postUri: decodeURIComponent(this.postUri),
            connectionUri: connectionUri,
            connectionType: this.connectionType,
        });
    }
}

Controller.$inject = serviceDependencies;



export default angular.module('won.owner.components.post', [
    ownerTitleBarModule,
    //galleryModule,
    postMessagesModule,
    connectionSelectionModule,
    openRequestModule,
    matchesModule,
    postInfoModule,
])
    .controller('PostController', Controller)
    .name;
