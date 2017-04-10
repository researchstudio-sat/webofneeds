/**
 * Component to render the details for one post
 *
 * Created by ksinger on 10.04.2017.
 */
import angular from 'angular';
import 'ng-redux';
import extendedGalleryModule from '../components/extended-gallery';
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
        <div class="piu__header__title">
          <div class="piu__header__title__topline">
            <div class="piu__header__title__topline__title">
              {{ self.needContent.get('dc:title') }}
            </div>
            <div class="piu__header__title__topline__date">
              {{ self.friendlyTimestamp }}
            </div>
          </div>
          <div class="piu__header__title__subtitle">
            <!--
            <span class="piu__header__title__subtitle__group" ng-show="{{self.need.get('group')}}">
              <img
                src="generated/icon-sprite.svg#ico36_group"
                class="piu__header__title__subtitle__group__icon">
              {{self.need.get('group')}}
              <span class="piu__header__title__subtitle__group__dash"> &ndash; </span>
            </span>
            -->
            <span class="piu__header__title__subtitle__type">
              {{ self.labels.type[self.needType] }}
            </span>
          </div>
        </div>
      </div>
      <div class="piu__content">
        <!--
        <div class="piu__content__images" ng-show="self.need.get('images')">
          <won-extended-gallery max-thumbnails="self.maxThumbnails" items="self.need.get('images')" class="vertical"></won-extended-gallery>
        </div>
        -->
        <div class="piu__content__description">
          <div
            class="piu__content__description__location"
            ng-show="won.needContent.getIn(['won:hasLocation', 's:name'])">
              <img class="piu__content__description__indicator"
                src="generated/icon-sprite.svg#ico16_indicator_location"/>
              <span>{{ won.needContent.getIn(['won:hasLocation', 's:name']) }}</span>
          </div>
          <!--
          <div class="piu__content__description__datetime">
            <img class="piu__content__description__indicator" src="generated/icon-sprite.svg#ico16_indicator_time"/>
            <span>Available until 5th May</span>
          </div>
          -->
          <div class="piu__content__description__text"
            ng-show="!!self.needContent.get('won:hasTextDescription')">
            <img
              class="piu__content__description__indicator"
              src="generated/icon-sprite.svg#ico16_indicator_description"/>
            <span>
              <p>{{ self.needContent.get('won:hasTextDescription') }}</p>
            </span>
          </div>
          <div class="piu__content__description__text"
            ng-show="!!self.textMsg">
            <img
              class="piu__content__description__indicator"
              src="generated/icon-sprite.svg#ico16_indicator_message"/>
            <span>
              <p>{{ self.textMsg }}</p>
            </span>
          </div>

        </div>
      </div>
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
    extendedGalleryModule
])
    .directive('wonPostInfoUnified', genComponentConf)
    .name;
