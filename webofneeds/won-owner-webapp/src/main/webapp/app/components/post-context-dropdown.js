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
    connect2Redux,
} from '../won-utils.js';
import {
    selectOpenPostUri,
} from '../selectors.js';

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
                            <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
                        </svg>
                    </div>
                    <!-- Buttons for post -->
                    <button class="won-button--filled red"
                        ng-if="self.isOwnPost && self.isInactive"
                        ng-click="self.reOpenPost()">
                        Reopen Post
                    </button>
                    <button class="won-button--filled red"
                        ng-if="self.isOwnPost && self.isActive"
                        ng-click="self.closePost()">
                        Close Post
                    </button>
                </div>
            </div>
        `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            const self = this;
            const selectFromState = (state) => {
                const postUri = selectOpenPostUri(state);
                const post = postUri && state.getIn(["needs", postUri]);
                const postState = post && post.get("state");

                return {
                    isOwnPost: post && post.get("ownNeed"),
                    isActive: postState === won.WON.ActiveCompacted,
                    isInactive: postState === won.WON.InactiveCompacted,
                    post,
                    postUri,
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

        closePost() {
            if(this.isOwnPost){
                console.log("CLOSING THE POST: "+this.post.get('uri'));
                this.needs__close(this.post.get('uri'));
                this.router__stateGoCurrent({postUri : null})
            }
        }

        reOpenPost() {
            if(this.isOwnPost){
                console.log("RE-OPENING THE POST: "+this.post.get('uri'));
                this.needs__reopen(this.post.get('uri'));
            }
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

export default angular.module('won.owner.components.postContextDropdown', [
    ngAnimate,
])
    .directive('wonPostContextDropdown', genComponentConf)
    .name;

