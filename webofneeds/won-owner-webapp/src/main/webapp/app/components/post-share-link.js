import angular from 'angular';
import ngAnimate from 'angular-animate';
import { actionCreators }  from '../actions/actions.js';
import {
    attach,
} from '../utils.js';

import {
    connect2Redux,
} from '../won-utils.js';

const serviceDependencies = ['$scope', '$ngRedux', '$element'];
function genComponentConf() {
    let template = `
        <div class="psl__separator clickable" ng-class="{'psl__separator--open' : self.showShare}" ng-click="self.showShare = !self.showShare">
            <span class="psl__separator__text">Share</span>
            <svg style="--local-primary:var(--won-secondary-color);"
                class="psl__separator__arrow"
                ng-if="self.showShare">
                <use xlink:href="#ico16_arrow_down" href="#ico16_arrow_down"></use>
            </svg>
            <svg style="--local-primary:var(--won-secondary-color);"
                class="psl__separator__arrow"
                ng-if="!self.showShare">
                <use xlink:href="#ico16_arrow_up" href="#ico16_arrow_up"></use>
            </svg>
        </div>
        <div class="psl__content" ng-if="self.showShare">
            <p class="psl__text" ng-if="self.post.get('connections').size == 0 && self.post.get('ownNeed')">
                Your posting has no connections yet. Consider sharing the link below in social media, or wait for matchers to connect you with others.
            </p>
            <p class="psl__text" ng-if="(self.post.get('connections').size != 0 && self.post.get('ownNeed')) || !self.post.get('ownNeed')">
                Know someone who might also be interested in this posting? Consider sharing the link below in social media.
            </p>
            <input class="psl__link" value="{{self.linkToPost}}" readonly type="text" ng-click="self.selectLink()">
            <p class="psl__info" ng-if="!self.isCopied">Click the Link above to copy it to the clipboard.</p>
            <p class="psl__info" ng-if="self.isCopied">Link copied to the clipboard.</p>
        </div>
    `;

    class Controller {
        constructor() {
            attach(this, serviceDependencies, arguments);

            const selectFromState = (state) => {
                const post = this.postUri && state.getIn(["needs", this.postUri]);

                return {
                    post,
                    linkToPost: post && new URL("/owner/#!post/?postUri="+encodeURI(post.get('uri')), window.location.href).href,
                }
            };
            connect2Redux(selectFromState, actionCreators, ['self.postUri'], this);
        }

        getLinkField() {
            if(!this._linkField) {
                this._linkField = this.$element[0].querySelector('.psl__link');
            }
            return this._linkField;
        }

        selectLink() {
            const linkEl = this.getLinkField();
            if(linkEl) {
                linkEl.setSelectionRange(0, linkEl.value.length); //refocus so people can keep writing

                try {
                    var successful = document.execCommand('copy');
                    if (!successful) throw successful;
                    this.isCopied = true;
                    linkEl.setSelectionRange(0,0);
                    //prompt that it has been copied to the clipboard otherwise and remove the selection
                } catch (err) {
                    window.prompt("Copy to clipboard: Ctrl+C, Enter", linkEl.value);
                }
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
            postUri: "=",
            connectionUri: '=',
        },
        template: template
    }
}

export default angular.module('won.owner.components.postShareLink', [
    ngAnimate,
])
    .directive('wonPostShareLink', genComponentConf)
    .name;

