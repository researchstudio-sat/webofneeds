import angular from 'angular';
import overviewTitleBarModule from '../overview-title-bar.js';
import feedItemModule from '../feed-item.js'
import { actionCreators }  from '../../actions/actions.js';
import { attach } from '../../utils.js';

import {
    resetParams,
} from '../../configRouting.js';

import {
    selectAllOwnNeeds,
} from '../../selectors.js';

import * as srefUtils from '../../sref-utils.js';

const serviceDependencies = ['$ngRedux', '$scope', '$state'/*'$routeParams' /*injections as strings here*/];
class FeedController {
    constructor() {
        attach(this, serviceDependencies, arguments);
        Object.assign(this, srefUtils); // bind srefUtils to scope

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

