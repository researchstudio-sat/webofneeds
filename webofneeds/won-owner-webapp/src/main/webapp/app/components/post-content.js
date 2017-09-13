/**
 * Component to display the optional fields (description, images, ...)
 * of a need.
 * Created by ksinger on 10.04.2017.
 */

import angular from 'angular';
import 'ng-redux';
import extendedGalleryModule from '../components/extended-gallery.js';
import { actionCreators }  from '../actions/actions.js';
import {
    attach,
} from '../utils.js'
import {
    connect2Redux,
} from '../won-utils.js'

const serviceDependencies = ['$ngRedux', '$scope'];
function genComponentConf() {
    let template = `
        <div
          class="pc__text"
          ng-show="self.need.getIn(['location', 's:name'])">
            <img class="pc__icon"
              src="generated/icon-sprite.svg#ico16_indicator_location"/>
            <span>{{ self.need.getIn(['location', 's:name']) }}</span>
        </div>
      <!--
      <div class="pc__images" ng-show="self.need.get('images')">
        <won-extended-gallery
          max-thumbnails="self.maxThumbnails"
          items="self.need.get('images')"
          class="vertical">
        </won-extended-gallery>
      </div>
      -->
        <!--
        <div class="pc__datetime">
          <img class="pc__icon"
            src="generated/icon-sprite.svg#ico16_indicator_time"/>
          <span>Available until 5th May</span>
        </div>
        -->
        <div class="pc__text"
          ng-show="!!self.need.get('description')">
          <img
            class="pc__icon"
            src="generated/icon-sprite.svg#ico16_indicator_description"/>
          <span>
            {{ self.need.get('description') }}
          </span>
        </div>
        <div class="pc__text"
          ng-show="!!self.need.get('location')">
          <img
            class="pc__icon"
            src="generated/icon-sprite.svg#ico16_indicator_location"/>
          <span>
            {{ self.need.getIn(['location','address']) }}
          </span>
        </div>
        <div class="pc__text"
          ng-show="!!self.textMessage">
          <img
            class="pc__icon"
            src="generated/icon-sprite.svg#ico16_indicator_message"/>
          <span>
            {{ self.textMessage }}
          </span>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            window.pc4dbg = this;
            const selectFromState = (state) => {
                return {
                    need: state.getIn(['needs', this.needUri]),
                }
            };
            /*
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
            */
            connect2Redux(selectFromState, actionCreators, ['self.needUri'], this);
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
             * one of:
             * - "fullpage" (NOT_YET_IMPLEMENTED) (used in post-info page)
             * - "medium" (NOT_YET_IMPLEMENTED) (used in incoming/outgoing requests and matches-tiles)
             * - "small" (NOT_YET_IMPLEMENTED) (in matches-list)
             */
            //size: '=',

            /**
             * Additional text-message that is shown. Use this e.g. when displaying
             * an incoming request.
             */
            textMessage: '=',
        },
        template: template
    }
}

export default angular.module('won.owner.components.postContent', [
    extendedGalleryModule,
])
    .directive('wonPostContent', genComponentConf)
    .name;
