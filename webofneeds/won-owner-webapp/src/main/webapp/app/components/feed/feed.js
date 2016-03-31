import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import feedItemModule from '../feed-item'
import { actionCreators }  from '../../actions/actions';
import { attach } from '../../utils';

import {
    selectConnectionsByNeed,
    selectUnreadCountsByNeedAndType,
    selectUnreadEvents
} from '../../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];
class FeedController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;


        const selectFromState = (state) => {


            /*
            const unreadEventUris = state
                .getIn(['events', 'unreadEventUris'])
                .map(event => event.get('uri'))
                .toSet();
                */
            //const events = state.getIn(['events', 'events']);
            //unreadEventUris.map(eventUri => events.get(eventUri));

            const unreadEvents = selectUnreadEvents(state);

            window.selectUnread4dbg = selectUnreadEvents;

            const eventsByConnection = state.getIn(['events', 'events'])

            const connectionsByNeed = state.getIn(['needs', 'ownNeeds'])
                .map(ownNeed => ownNeed.get('hasConnections'));
            //TODO attach events

            // sort by newest event (excluding matches)

            // wenn sich die sortierung aufgrund neuer events verändern wuerde, wird ein button/link angezeigt ("new messages/requests. click to update")
            // always show latest message in a line

            return {
                unreadEvents4dbg: unreadEvents,
                state4dbg: state,

                posts: state.getIn(["needs", "ownNeeds"]),
                connectionsByNeed: selectConnectionsByNeed(state),
                unreadCountsByNeedAndType: selectUnreadCountsByNeedAndType(state),
            }
        }
        const disconnect = this.$ngRedux.connect(selectFromState,actionCreators)(this)
        this.$scope.$on('$destroy', disconnect);

        window.fc4dbg = this;
    }

}

export default angular.module('won.owner.components.feed', [
    overviewTitleBarModule,
    feedItemModule
])
    .controller('FeedController', [...serviceDependencies,FeedController])
    .name;

