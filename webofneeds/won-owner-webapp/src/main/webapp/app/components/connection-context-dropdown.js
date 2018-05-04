/**
 * Created by ksinger on 30.03.2017.
 */



import angular from 'angular';
import ngAnimate from 'angular-animate';
import { actionCreators }  from '../actions/actions.js';
import won from '../won-es6.js';
import {
    attach,
} from '../utils.js';
import {
    selectOpenConnectionUri,
    selectNeedByConnectionUri,
} from '../selectors.js';
import {
    connect2Redux,
} from '../won-utils.js';

const serviceDependencies = ['$scope', '$ngRedux', '$element'];
function genComponentConf() {
    let template = `
            <svg class="cdd__icon__small clickable"
                style="--local-primary:#var(--won-secondary-color);"
                ng-show="!self.contextMenuOpen"
                ng-click="self.contextMenuOpen = true">
                    <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
            </svg>
            <div class="cdd__contextmenu contextmenu" ng-show="self.contextMenuOpen">
                <div class="content" ng-click="self.contextMenuOpen = false">
                    <div class="topline">
                        <svg class="cdd__icon__small__contextmenu clickable"
                            style="--local-primary:black;">
                            <use  xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                        </svg>
                    </div>
                    <!-- Buttons when connection is available -->
                    <button
                        class="won-button--outlined thin red"
                        ng-if="!self.isSuggested"
                        ng-click="self.goToPost(self.connection.get('remoteNeedUri'))">
                        Show Post Details
                    </button>
                    <button
                        ng-if="self.isConnected || self.isSuggested"
                        class="won-button--filled red"
                        ng-click="self.closeConnection()">
                        Close Connection
                    </button>
                    <button
                        ng-if="self.isSentRequest"
                        class="won-button--filled red"
                        ng-click="self.closeConnection()">
                        Cancel Request
                    </button>
                </div>
            </div>
        `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            const self = this;
            const selectFromState = (state) => {
                const connectionUri = selectOpenConnectionUri(state);

                const post = connectionUri && selectNeedByConnectionUri(state, connectionUri);
                const connection = post && post.getIn(["connections", connectionUri]);
                const connectionState = connection && connection.get('state');

                return {
                    connection,
                    connectionUri,
                    isConnected: connectionState === won.WON.Connected,
                    isSentRequest: connectionState === won.WON.RequestSent,
                    isReceivedRequest: connectionState === won.WON.RequestReceived,
                    isSuggested: connectionState === won.WON.Suggested
                }
            };
            connect2Redux(selectFromState, actionCreators, [], this);

            const callback = (event) => {
                const clickedElement = event.target;
                //hide MainMenu if click was outside of the component and menu was open
                if(this.contextMenuOpen && !this.$element[0].contains(clickedElement)){
                    this.contextMenuOpen = false;
                    this.$scope.$apply();
                }
            };

            this.$scope.$on('$destroy', () => {
                window.document.removeEventListener('click', callback);
            });
            
            window.document.addEventListener('click', callback);
        }

        closeConnection(){
            this.connections__close(this.connectionUri);
            this.router__stateGoCurrent({connectionUri: null});
        }

        goToPost(postUri) {
            this.router__stateGoCurrent({postUri: postUri});
        }
    }
    Controller.$inject = serviceDependencies;

    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: {
        },
        template: template
    }
}

export default angular.module('won.owner.components.connectionContextDropdown', [
    ngAnimate,
])
    .directive('wonConnectionContextDropdown', genComponentConf)
    .name;

