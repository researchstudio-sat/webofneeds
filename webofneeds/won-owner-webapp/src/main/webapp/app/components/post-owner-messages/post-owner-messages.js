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
import needConnectionMessageLineModule from '../connection-message-item-line';
import openConversationModule from '../open-conversation';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];
class Controller {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;
        this.openConversation = undefined;
        window.msgs4dbg = this;

        const selectFromState = (state)=>{
            const postId = decodeURIComponent(state.getIn(['router', 'currentParams', 'myUri']));
            return {
                post: state.getIn(['needs','ownNeeds', postId]).toJS(),
                conversations: Object.keys(state.getIn(['connections','connections']).toJS())
                    .map(key=>state.getIn(['connections','connections']).toJS()[key])
                    .filter(conn=>{
                        if(conn.connection.hasConnectionState===won.WON.Connected && conn.ownNeed.uri === postId){
                            return true
                        }
                    }),
            };
        }

        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }
}

Controller.$inject = serviceDependencies;

export default angular.module('won.owner.components.postOwner.messages', [
        visitorTitleBarModule,
        galleryModule,
        postMessagesModule,
        needConnectionMessageLineModule
    ])
    .controller('PostOwnerMessagesController', Controller)
    .name;
