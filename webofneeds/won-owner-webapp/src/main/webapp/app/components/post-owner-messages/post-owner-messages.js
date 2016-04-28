/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import ownerTitleBarModule from '../owner-title-bar';
//import galleryModule from '../gallery';
import postMessagesModule from '../post-messages';
import { attach,mapToMatches } from '../../utils';
import won from '../../won-es6';
import { actionCreators }  from '../../actions/actions';
import openConversationModule from '../open-conversation';
import openRequestModule from '../open-request';
import connectionSelectionModule from '../connection-selection';
import { selectAllByConnections } from '../../selectors';

const serviceDependencies = ['$ngRedux', '$scope'];
class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;
        window.msgs4dbg = this;
        this.wonConnected = won.WON.Connected;

        const selectFromState = (state)=>{
            const encodedPostUri = state.getIn(['router', 'currentParams', 'postUri']) ||
                                state.getIn(['router', 'currentParams', 'myUri']) ; // TODO old parameter
            const postUri = decodeURIComponent(encodedPostUri);

            const encodedConnectionUri = state.getIn(['router', 'currentParams', 'connectionUri']) ||
                state.getIn(['router', 'currentParams', 'openConversation']); // TODO old parameter
            const connectionUri = encodedConnectionUri? decodeURIComponent(encodedConnectionUri) : undefined;
            const actualConnectionType = state.getIn([
                'connections', connectionUri, 'hasConnectionState'
            ]);

            const encodedConnectionType = state.getIn(['router', 'currentParams', 'connectionType']);
            const connectionTypeInParams = (encodedConnectionType ? decodeURIComponent(encodedConnectionType) : undefined) ||
                won.WON.Connected; // TODO old parameter

            const connectionIsOpen = !!encodedConnectionUri &&
                //make sure we don't get a mismatch between supposed type and actual type:
                actualConnectionType == connectionTypeInParams;

            return {
                postUri: postUri,

                connectionType: connectionTypeInParams,
                connectionUri: decodeURIComponent(encodedConnectionUri),

                showConversationDetails: connectionIsOpen && connectionTypeInParams === won.WON.Connected,
                showMatchDetails: connectionIsOpen && connectionTypeInParams === won.WON.Suggested,
                showIncomingRequestDetails: connectionIsOpen && connectionTypeInParams === won.WON.RequestReceived,
                showSentRequestDetails: connectionIsOpen && connectionTypeInParams === won.WON.RequestSent,
            };
        }

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }
    openConnection(connectionUri) {
        this.router__stateGo('post', {
            postUri: decodeURIComponent(this.postUri),
            connectionUri: connectionUri,
            connectionType: this.connectionType,
        })
    }
}

Controller.$inject = serviceDependencies;



export default angular.module('won.owner.components.postOwner.messages', [
    ownerTitleBarModule,
    //galleryModule,
    postMessagesModule,
    connectionSelectionModule,
    openRequestModule,
])
    .controller('PostOwnerMessagesController', Controller)
    .name;
