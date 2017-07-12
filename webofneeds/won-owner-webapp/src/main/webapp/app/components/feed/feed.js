import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import feedItemModule from '../feed-item'
import { actionCreators }  from '../../actions/actions';
import { attach } from '../../utils';

import {
    selectAllByConnectionsByNeed,
    selectUnreadCountsByNeedAndType,
    selectUnreadEvents,
    selectOwnNeeds,
} from '../../selectors';

const serviceDependencies = ['$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];
class FeedController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;

        const selectFromState = (state) => {

            const unreadEvents = selectUnreadEvents(state);

            window.selectUnread4dbg = selectUnreadEvents;

            //TODO attach events

            // sort by newest event (excluding matches)

            // wenn sich die sortierung aufgrund neuer events verändern wuerde, wird ein button/link angezeigt ("new messages/requests. click to update")
            // always show latest message in a line
            const ownNeeds = selectOwnNeeds(state);

            return {
                unreadEvents4dbg: unreadEvents,
                state4dbg: state,

                ownNeedUris: ownNeeds && ownNeeds.filter(n => n.getIn([won.WON.isInStateCompacted, "@id"]) === won.WON.ActiveCompacted).map(n => n.get('@id')).toArray(),

                connectionsByNeed: selectAllByConnectionsByNeed(state),
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

