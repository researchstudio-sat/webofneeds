;

import angular from 'angular';
import 'ng-redux';
import postContentModule from './post-content';
import postHeaderModule from './post-header';
import { selectLastUpdateTime } from '../selectors';
import { attach } from '../utils';
import { actionCreators }  from '../actions/actions';
import {
    seeksOrIs,
    inferLegacyNeedType,
} from '../won-utils';

const serviceDependencies = ['$q', '$ngRedux', '$scope'];

function genComponentConf() {
    let template = `
      <div class="sr__caption">
        <div class="sr__caption__title">Send Conversation Request</div>
        <a ui-sref="{connectionUri: null}">
          <img
            class="sr__caption__icon clickable"
            src="generated/icon-sprite.svg#ico36_close"/>
        </a>
      </div>

      <won-post-header
        need-uri="self.theirNeedUri">
      </won-post-header>

      <won-post-content
        need-uri="self.theirNeedUri">
      </won-post-content>

      <div class="sr__footer">
        <input
          type="text"
          ng-model="self.message"
          placeholder="Reply Message (optional)"/>
        <div class="flexbuttons">
          <button
            class="won-button--filled black"
            ui-sref="{connectionUri: null}">
              Cancel
          </button>
          <button
            class="won-button--filled red"
            ng-click="self.sendRequest(self.message)"
            ui-sref="{connectionUri: null}">
              Request Contact
          </button>
        </div>
        <a ng-show="self.debugmode"
          class="debuglink"
          target="_blank"
          href="{{self.connectionUri}}">
            [CONNDATA]
        </a>
      </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);
            this.maxThumbnails = 9;
            this.message = '';
            window.openMatch4dbg = this;

            const selectFromState = (state) => {
                const connectionUri = decodeURIComponent(state.getIn(['router', 'currentParams', 'connectionUri']));

                const theirNeedUri = state.getIn(['connections', connectionUri, 'hasRemoteNeed']);

                return {
                    connectionUri: connectionUri,
                    connection: state.getIn(['connections', connectionUri]),

                    theirNeedUri,
                    debugmode: won.debugmode,
                }
            };
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);
        }

        sendRequest(message) {
            this.connections__connect(this.connectionUri, message);
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {},
        template: template
    }
}

export default angular.module('won.owner.components.sendRequest', [
    postContentModule,
    postHeaderModule,
])
    .directive('wonSendRequest', genComponentConf)
    .name;

