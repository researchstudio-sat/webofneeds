;
import Immutable from 'immutable';
import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import postItemLineModule from '../post-item-line';
import { actionCreators }  from '../../actions/actions';
import { attach } from '../../utils';
import { unreadCountsSelector } from '../../selectors';
import won from '../../won-es6';

const ZERO_UNSEEN = Object.freeze({
    matches: 0,
    incomingRequests: 0,
    conversations: 0,
});

const serviceDependencies = ['$q', '$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];
class OverviewPostsController {

    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 1;

        const selectFromState = (state) => {
            const unreadEvents = state.getIn(["events", "unreadEventUris"]);
            const receivedHintEvents = unreadEvents.filter(e=>e.get('eventType')===won.EVENT.HINT_RECEIVED);
            let unseenMatchesCounts = Immutable.Map();
            receivedHintEvents.forEach(e => {
                const receiverNeed = e.get('hasReceiverNeed');
                let count = unseenMatchesCounts.get(receiverNeed);
                if(!count){
                    unseenMatchesCounts = unseenMatchesCounts.set(receiverNeed, 1);
                }
                else{
                    unseenMatchesCounts = unseenMatchesCounts.set(receiverNeed, count + 1);
                }

            });




            //won.EVENT.HINT_RECEIVED -> matches
            //won.EVENT.WON_MESSAGE_RECEIVED -> convoMessages (!= convos with new messages <- we want this)
            //won.EVENT.CONNECT_RECEIVED -> incomingRequests

            /*

            ///-------------
            function groupByNeed(unreadEvents) {
                let need2events = Immutable.Map();
                unreadEvents.forEach(e => {
                    const receiverNeed = e.get('hasReceiverNeed');
                    need2events
                    //....
                });
            }

            //-----------
            let unseenCounts = {};
            unreadEvents.forEach(e => {
                const receiverNeed = e.get('hasReceiverNeed');
                const prevCounts = unseenCounts[receiverNeed];
                {}

                unseenCounts[receiverNeed] = unseenCounts[receiverNeed]? unseenCounts[receiverNeed] : {};
t
                switch(e.get('eventType')) {
                    case won.EVENT.HINT_RECEIVED:
                        const count = unseenCounts[receiverNeed].matches? unseenCounts[receiverNeed].matchesObje
                        this._unseenCounts[receiverNeed].matches += 1;
                    break;
                    case won.EVENT.WON_MESSAGE_RECEIVED:
                        //TODO count unique convos
                        this._unseenCounts[receiverNeed].conversations += 1;
                    break;
                    case won.EVENT.CONNECTION_MESSAGE_RECEIVED:
                        //TODO v--^ which of these two?
                    case won.EVENT.CONNECT_RECEIVED:
                        this._unseenCounts[receiverNeed].incomingRequests += 1;
                    break;

                        if(!this._unseenCounts[receiverNeed]) this._unseenCounts[receiverNeed] = {};
                }

            });
            */

            //goal: unseenCounts = { <uri> : { matches: 11, conversations: 0, incomingRequests: 2 }, <uri>:...}
            //TODO use memoized selector to avoid running this calculation on every tick



            return {
                posts: state.getIn(["needs", "needs"]).toJS(),
                unreadEvents,
                unreadCounts: unreadCountsSelector(state),
                unreadMatchEventsOfNeed: unseenMatchesCounts,
                //nrOfPostsWithNotifications: unseenMatchesCounts.length
                drafts: null,
                activePostsOpen: state.getIn(["postOverview", "activePostsView"]),
                draftsOpen: false,
                closedPostsOpen: state.getIn(["postOverview", "closedPostsView"])
            }
        }

        window.opc = this;
/*        this.$scope.getMatches = function(uri){
            this.$filter('filterEventByType')(this.$scope.unreadEvents,uri,won.EVENT.HINT_RECEIVED)
        }*/
        const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
        this.$scope.$on('$destroy', disconnect);
    }


}

OverviewPostsController.$inject = [];

export default angular.module('won.owner.components.overviewPosts', [
        overviewTitleBarModule,
        postItemLineModule
    ])
    .controller('OverviewPostsController',[...serviceDependencies,OverviewPostsController] )

    .name;


