/**
 * Component for rendering need-title, type and timestamp
 * Created by ksinger on 10.04.2017.
 */
import angular from 'angular';
import 'ng-redux';
import squareImageModule from './square-image.js';
import { actionCreators }  from '../actions/actions.js';
import {
    labels,
    relativeTime
} from '../won-label-utils.js';
import {
    attach,
} from '../utils.js'
import {
    connect2Redux,
} from '../won-utils.js'
import {
    selectLastUpdateTime,
    selectNeedByConnectionUri,
    selectAllTheirNeeds,
} from '../selectors.js';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <won-square-image
        ng-class="{'bigger' : self.biggerImage, 'inactive' : self.need.get('state') === self.WON.InactiveCompacted}"
        src="self.need.get('TODO')"
        title="self.need.get('title')"
        uri="self.needUri"
        ng-show="!self.hideImage">
      </won-square-image>
      <div class="ch__right">
        <div class="ch__right__topline">
          <div class="ch__right__topline__title">
            {{ self.need.get('title') }}
          </div>
          <div class="ch__right__topline__date">
            {{ self.friendlyTimestamp }}
          </div>
        </div>
        <div class="ch__right__subtitle">
          <!--
          <span class="piu__header__title__subtitle__group" ng-show="{{self.need.get('group')}}">
            
          <svg style="--local-primary:var(--won-primary-color);"
            class="piu__header__title__subtitle__group__icon">
              <use href="#ico36_group"></use>
          </svg>


            {{self.need.get('group')}}
            <span class="piu__header__title__subtitle__group__dash"> &ndash; </span>
          </span>
          -->
          <span class="ch__right__subtitle__type">
            {{self.labels.type[self.need.get('type')]}}{{self.need.get('matchingContexts')? ' in '+ self.need.get('matchingContexts').join(', ') : ' (no matching context specified)' }}
          </span>
        </div>
      </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.ph4dbg = this;
            this.labels = labels;
            this.WON = won.WON;
            const selectFromState = (state) => {
                const ownNeed = selectNeedByConnectionUri(state, this.connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", this.connectionUri]);
                const need = connection && selectAllTheirNeeds(state).get(connection.get("remoteNeedUri"));

                return {
                    need,
                    friendlyTimestamp: need && relativeTime(
                        selectLastUpdateTime(state),
                        this.timestamp || need.get('lastUpdateDate')
                    ),
                }
            };

            connect2Redux(
                selectFromState, actionCreators,
                ['self.needUri', 'self.timestamp'],
                this
            );
        }
    }
    Controller.$inject = serviceDependencies;
    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
            connectionUri: '=',

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
            //size: '=',

            /**
             * if set, the avatar will be hidden
             */
            hideImage: '=',

            /**
             * If true, the title image will be a bit bigger. This
             * can be used to create visual contrast.
             */
            biggerImage: '=',
        },
        template: template
    }
}

export default angular.module('won.owner.components.connectionHeader', [
    squareImageModule,
])
    .directive('wonConnectionHeader', genComponentConf)
    .name;
