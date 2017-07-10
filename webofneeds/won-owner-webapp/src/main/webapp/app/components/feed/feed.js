import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import feedItemModule from '../feed-item'
import { actionCreators }  from '../../actions/actions';
import { attach } from '../../utils';

import {
    selectUnreadCountsByNeedAndType,
    selectUnreadEvents,
    selectAllOwnNeeds,
} from '../../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];
class FeedController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;

        const selectFromState = (state) => {

            const unreadEvents = selectUnreadEvents(state);

            //TODO attach events

            // sort by newest event (excluding matches)

            // wenn sich die sortierung aufgrund neuer events verändern wuerde, wird ein button/link angezeigt ("new messages/requests. click to update")
            // always show latest message in a line
            const ownNeeds = selectAllOwnNeeds(state);

            return {
                ownNeedUris: ownNeeds && ownNeeds.filter(need => need.get("state") === won.WON.ActiveCompacted).map(need => need.get('uri')).toArray(),
                unreadCountsByNeedAndType: selectUnreadCountsByNeedAndType(state),
            }
        };
        const disconnect = this.$ngRedux.connect(selectFromState,actionCreators)(this);
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

