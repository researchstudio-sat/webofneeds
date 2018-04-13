/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import ngAnimate from 'angular-animate';
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
import connectionSelectionModule from '../connection-selection.js';
import postInfoModule from '../post-info.js';
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
        this.WON = won.WON;

        const selectFromState = (state)=>{
            const postUri = selectOpenPostUri(state);
            const post = state.getIn(["needs", postUri]);


            const hasConnections = post && post.get('connections').filter(conn => conn.get("state") !== won.WON.Closed).size > 0;

            const connectionUri = selectOpenConnectionUri(state);
            const actualConnectionType = post && connectionUri && post.getIn(['connections', connectionUri, 'state']);

            const connectionTypeInParams = decodeUriComponentProperly(
                getIn(state, ['router', 'currentParams', 'connectionType'])
            );

            const sendAdHocRequest = post && !post.get("ownNeed") && getIn(state, ['router', 'currentParams', 'sendAdHocRequest']);

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
                won: won.WON,
            };
        };

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }

    openConnection(connectionUri) {
        this.markAsRead(connectionUri);
        this.router__stateGoAbs('post', {
            postUri: decodeURIComponent(this.postUri),
            connectionUri: connectionUri,
            connectionType: this.connectionType,
        });
    }

    markAsRead(connectionUri){
        const connections = this.post && this.post.get("connections");
        const connection = connections && connections.get(connectionUri);

        if(connection && connection.get("unread") && connection.get("state") !== won.WON.Connected) {
            const payload = {
                connectionUri: connectionUri,
                needUri: this.postUri,
            };

            this.connections__markAsRead(payload);
        }
    }
}

Controller.$inject = serviceDependencies;



export default angular.module('won.owner.components.post', [
    ownerTitleBarModule,
    //galleryModule,
    postMessagesModule,
    connectionSelectionModule,
    postInfoModule,
    ngAnimate,
])
    .controller('PostController', Controller)
    .name;
