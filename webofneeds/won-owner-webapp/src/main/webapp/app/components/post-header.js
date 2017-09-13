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
} from '../selectors.js';

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
      <won-square-image
        ng-class="{'bigger' : self.biggerImage}"
        src="self.need.get('TODO')"
        title="self.need.get('title')"
        uri="self.needUri"
        ng-show="!self.hideImage">
      </won-square-image>
      <div class="ph__right">
        <div class="ph__right__topline">
          <div class="ph__right__topline__title">
            {{ self.need.get('title') }}
          </div>
          <div class="ph__right__topline__date">
            {{ self.friendlyTimestamp }}
          </div>
        </div>
        <div class="ph__right__subtitle">
          <!--
          <span class="piu__header__title__subtitle__group" ng-show="{{self.need.get('group')}}">
            <img
              src="generated/icon-sprite.svg#ico36_group"
              class="piu__header__title__subtitle__group__icon">
            {{self.need.get('group')}}
            <span class="piu__header__title__subtitle__group__dash"> &ndash; </span>
          </span>
          -->
          <span class="ph__right__subtitle__type">
            {{ self.labels.type[self.need.get('type')] }}
          </span>
        </div>
      </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.ph4dbg = this;
            this.labels = labels;
            const selectFromState = (state) => {
                const need = state.getIn(['needs', this.needUri]);

                return {
                    need,
                    friendlyTimestamp: need && relativeTime(
                        selectLastUpdateTime(state),
                        this.timestamp || need.get('creationDate')
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

export default angular.module('won.owner.components.postHeader', [
    squareImageModule,
])
    .directive('wonPostHeader', genComponentConf)
    .name;
