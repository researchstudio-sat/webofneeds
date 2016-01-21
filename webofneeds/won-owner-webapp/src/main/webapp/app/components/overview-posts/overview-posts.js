;
import Immutable from 'immutable';
import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import postItemLineModule from '../post-item-line';
import { actionCreators }  from '../../actions/actions';
import { attach } from '../../utils';
import won from '../../won-es6';

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
                    unseenMatchesCounts =unseenMatchesCounts.set(receiverNeed, count + 1);
                }

            });
            return {
                posts: state.getIn(["needs", "needs"]).toJS(),
                unreadEvents,
                unreadMatchEventsOfNeed: unseenMatchesCounts,
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


