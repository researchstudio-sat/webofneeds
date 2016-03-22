/**
 * Created by ksinger on 24.08.2015.
 */
;

import angular from 'angular';
import visitorTitleBarModule from '../owner-title-bar';
import galleryModule from '../gallery';
import postMessagesModule from '../post-messages';
import { attach,mapToMatches } from '../../utils';
import won from '../../won-es6';
import { actionCreators }  from '../../actions/actions';
import openConversationModule from '../open-conversation';
import connectionSelectionModule from '../connection-selection';
import { selectAllByConnections } from '../../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);
        window.msgs4dbg = this;

        const selectFromState = (state)=>{
            const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
            const connectionsDeprecated = selectAllByConnections(state).toJS(); //TODO plz don't do `.toJS()`. every time an ng-binding somewhere cries.
            const conversations = Object.keys(connectionsDeprecated)
                    .map(key => connectionsDeprecated[key])
                    .filter(conn =>
                        conn.connection.hasConnectionState === won.WON.Connected &&
                        conn.ownNeed.uri === postId
                    );
            const conversationUris = conversations.map(conn => conn.connection.uri)

            return {
                post: state.getIn(['needs','ownNeeds', postId]).toJS(),
                allByConnections: connectionsDeprecated,
                conversations: conversations,
                conversationUris: conversationUris,
                routerParams: state.getIn(['router', 'currentParams']),
            };
        }

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }
    openConversation(connectionUri) {
        console.log('openConversation ', connectionUri);
        this.router__stateGo('postConversations', {
            myUri: this.routerParams.get('myUri'),
            openConversation: connectionUri,
        })
    }
}

Controller.$inject = serviceDependencies;

export default angular.module('won.owner.components.postOwner.messages', [
        visitorTitleBarModule,
        galleryModule,
        postMessagesModule,
        connectionSelectionModule,
    ])
    .controller('PostOwnerMessagesController', Controller)
    .name;
