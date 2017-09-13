;

import angular from 'angular';
import 'ng-redux';
import postContentModule from './post-content.js';
import postHeaderModule from './post-header.js';
import { selectNeedByConnectionUri } from '../selectors.js';
import {
    connect2Redux,
} from '../won-utils.js';
import {
    attach,
    getIn,
} from '../utils.js';
import { actionCreators }  from '../actions/actions.js';

const serviceDependencies = ['$ngRedux', '$scope'];

function genComponentConf() {
    let template = `
      <div class="sr__caption">
        <div class="sr__caption__title">Send Conversation Request</div>
        <a ng-click="self.router__stateGoCurrent({connectionUri: null})"
            class="clickable">
          <img
            class="sr__caption__icon clickable"
            src="generated/icon-sprite.svg#ico36_close"/>
        </a>
      </div>

      <won-post-header
        need-uri="self.connection.get('remoteNeedUri')">
      </won-post-header>

      <won-post-content
        need-uri="self.connection.get('remoteNeedUri')">
      </won-post-content>

      <div class="sr__footer">
        <input
          type="text"
          ng-model="self.message"
          placeholder="Reply Message (optional)"/>
        <div class="flexbuttons">
          <button
            class="won-button--filled black"
            ng-click="self.router__stateGoCurrent({connectionUri: null})">
              Cancel
          </button>
          <button
            class="won-button--filled red"
            ng-click="self.sendRequest(self.message)">
              Request Contact
          </button>
        </div>
        <a ng-show="self.debugmode"
          class="debuglink"
          target="_blank"
          href="{{self.connection.get('uri')}}">
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
                const connectionUri = decodeURIComponent(getIn(state, ['router', 'currentParams', 'connectionUri']));
                const ownNeed = connectionUri && selectNeedByConnectionUri(state, connectionUri);
                const connection = ownNeed && ownNeed.getIn(["connections", connectionUri]);

                return {
                    connection,
                    debugmode: won.debugmode,
                }
            };
            connect2Redux(selectFromState, actionCreators, [], this);
        }

        sendRequest(message) {
            this.connections__connect(this.connection.get('uri'), message);
            this.router__stateGoCurrent({connectionUri: null})
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

