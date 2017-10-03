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

        const selectFromState = (state)=>{
            const postUri = selectOpenPostUri(state);
            const post = state.getIn(["needs", postUri]);

            const hasReceivedRequests = post && post.get('connections')
                .filter(conn => conn.get('state') === won.WON.RequestReceived)
                .size > 0;
            const hasSentRequests= post && post.get('connections')
                .filter(conn => conn.get('state') === won.WON.RequestSent)
                .size > 0;
            const hasMatches= post && post.get('connections')
                .filter(conn => conn.get('state') === won.WON.Suggested)
                .size > 0;
            const hasConversations = post && post.get('connections')
                .filter(conn => conn.get('state') === won.WON.Connected)
                .size > 0;

            const connectionUri = selectOpenConnectionUri(state);
            const actualConnectionType = post && post.getIn(['connections', connectionUri, 'state']);

            const connectionTypeInParams = decodeUriComponentProperly(
                getIn(state, ['router', 'currentParams', 'connectionType'])
            );

            const connectionIsOpen = !!connectionUri &&
                //make sure we don't get a mismatch between supposed type and actual type:
                actualConnectionType == connectionTypeInParams;

            return {
                connectionOpen: !!connectionUri,
                postUri,
                post,
                isOwnPost: post && post.get("ownNeed"),
                connectionUri,
                hasMatches,
                hasReceivedRequests,
                hasSentRequests,
                hasConversations,
                connectionType: connectionTypeInParams,
                showConnectionSelection: !!connectionTypeInParams && connectionTypeInParams !== won.WON.Suggested,
                showMatches: connectionTypeInParams === won.WON.Suggested && hasMatches,
                showConversationDetails: connectionIsOpen && connectionTypeInParams === won.WON.Connected,
                showIncomingRequestDetails: connectionIsOpen && connectionTypeInParams === won.WON.RequestReceived,
                showSentRequestDetails: connectionIsOpen && connectionTypeInParams === won.WON.RequestSent,
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
