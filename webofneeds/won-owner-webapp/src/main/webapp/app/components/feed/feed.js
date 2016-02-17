import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import feedItemModule from '../feed-item'
import { actionCreators }  from '../../actions/actions';
import { attach } from '../../utils';

import {
    selectConnectionsByNeed,
    selectUnreadCountsByNeedAndType,
} from '../../selectors';

const serviceDependencies = ['$q', '$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];
class FeedController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;

        const selectFromState = (state) =>({
            posts: state.getIn(["ownNeeds", "needs"]).toJS(),
            connectionsByNeed: selectConnectionsByNeed(state),
            unreadCountsByNeedAndType: selectUnreadCountsByNeedAndType(state),

            state4dgb: state,
        })
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

