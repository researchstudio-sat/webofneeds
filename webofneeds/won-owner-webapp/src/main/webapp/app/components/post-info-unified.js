/**
 * Component to render the details for one post
 *
 * Created by ksinger on 10.04.2017.
 */
import angular from 'angular';
import 'ng-redux';
import postContentModule from './post-content';
import postHeaderModule from './post-header';
import { actionCreators }  from '../actions/actions';
import {
    labels,
    relativeTime
} from '../won-label-utils';
import {
    attach,
    msStringToDate,
} from '../utils.js'
import {
    selectTimestamp,
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils'
import {
    selectLastUpdateTime,
} from '../selectors';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <div class="piu__header">
        <a> <!-- TODO allow to specify a custom route / handler for when the user presses the "x" -->
          <img class="piu__header__icon clickable" src="generated/icon-sprite.svg#ico36_close"/>
        </a>
        <won-post-header
          need-uri="self.needUri"
          timestamp="self.timestamp"
          size="self.size">
        </won-post-header>
      </div>
      <won-post-content
        need-uri="self.needUri"
        size="self.size"
        text-message="self.textMessage"
      >
      </won-post-content>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.piu4dbg = this;
            this.labels = labels;
            const selectFromState = (state) => {
                const need = state.getIn(['needs', 'ownNeeds', this.needUri]) ||
                             state.getIn(['needs', 'theirNeeds', this.needUri]);

                return {
                    need,
                    needType: need && inferLegacyNeedType(need),
                    needContent: need && seeksOrIs(need),
                    friendlyTimestamp: need && relativeTime(
                        selectLastUpdateTime(state),
                        this.timestamp || need.get('dct:created')
                    ),
                }
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            needUri: '=',
            /**
             * Will be used instead of the posts creation date if specified.
             * Use if you e.g. instead want to show the date when a request was made.
             */
            timestamp: '=',
            /**
             * one of:
             * - "fullpage" (NOT_YET_IMPLEMENTED) (used in post-info page)
             * - "medium" (NOT_YET_IMPLEMENTED) (used in incoming/outgoing requests)
             * - "small" (NOT_YET_IMPLEMENTED) (in matches-list)
             */
            size: '=',

            /**
             * Additional text-message that is shown. Use this e.g. when displaying
             * an incoming request.
             */
            textMessage: '=',

            hideCloseButton: '=',
            /*
             * Triggered when the "x"-button is pressed.
             * Alternatively `onhide` can also be registered
             * as DOM-node-listener.
             * Usage: `on-hide="::myCallback()"`
             */
            onHide: '&',
        },
        template: template
    }
}

export default angular.module('won.owner.components.postInfoUnified', [
    postContentModule,
    postHeaderModule,
])
    .directive('wonPostInfoUnified', genComponentConf)
    .name;
