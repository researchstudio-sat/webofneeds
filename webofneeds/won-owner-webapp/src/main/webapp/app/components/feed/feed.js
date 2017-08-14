import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar';
import feedItemModule from '../feed-item'
import { actionCreators }  from '../../actions/actions';
import { attach } from '../../utils';

import {
    resetParams,
} from '../../configRouting';

import {
    selectAllOwnNeeds,
} from '../../selectors';

const serviceDependencies = ['$ngRedux', '$scope', /*'$routeParams' /*injections as strings here*/];
class FeedController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        this.selection = 0;

        this.resetParams = resetParams;

        const selectFromState = (state) => {
            const ownActiveNeeds = selectAllOwnNeeds(state).filter(need => need.get("state") === won.WON.ActiveCompacted);

            return {
                ownNeedUris: ownActiveNeeds && ownActiveNeeds.map(need => need.get('uri')).toArray(),
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

