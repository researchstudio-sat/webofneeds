import angular from "angular";
import ngAnimate from "angular-animate";
import { actionCreators } from "../actions/actions.js";
import { attach, toAbsoluteURL } from "../utils.js";

import { connect2Redux } from "../won-utils.js";

import { ownerBaseUrl } from "config";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
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
            <div class="psl__inputline">
              <input class="psl__link" value="{{self.linkToPost}}" readonly type="text" ng-focus="self.selectLink()" ng-blur="self.clearSelection()">
              <button class="red won-button--filled psl__copy-button" ng-click="self.copyLink()">
                <svg class="psl__button-icon" style="--local-primary:white;">
                  <use xlink:href="{{ self.copied === true ? '#ico16_checkmark' : '#ico16_copy_to_clipboard'}}" href="{{ self.copied ? '#ico16_checkmark' : '#ico16_copy_to_clipboard'}}"></use>
                </svg>
              </button>
            </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);

      const selectFromState = state => {
        const post = this.postUri && state.getIn(["needs", this.postUri]);

        let linkToPost;
        if (ownerBaseUrl && post) {
          const path = "#!post/" + `?postUri=${encodeURI(post.get("uri"))}`;

          linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;
        }

        return {
          post,
          linkToPost,
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.postUri"], this);
    }

    getLinkField() {
      if (!this._linkField) {
        this._linkField = this.$element[0].querySelector(".psl__link");
      }
      return this._linkField;
    }

    selectLink() {
      const linkEl = this.getLinkField();
      if (linkEl) {
        linkEl.setSelectionRange(0, linkEl.value.length);
      }
    }

    clearSelection() {
      const linkEl = this.getLinkField();
      if (linkEl) {
        linkEl.setSelectionRange(0, 0);
      }
    }

    copyLink() {
      const linkEl = this.getLinkField();
      if (linkEl) {
        linkEl.focus();
        linkEl.setSelectionRange(0, linkEl.value.length);
        if (!document.execCommand("copy")) {
          window.prompt("Copy to clipboard: Ctrl+C", linkEl.value);
        } else {
          linkEl.setSelectionRange(0, 0);
        }
        linkEl.blur();
      }

      this.copied = true;

      const self = this;
      setTimeout(() => {
        self.copied = false;
        self.$scope.$digest();
      }, 1000);
    }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      postUri: "=",
      connectionUri: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.postShareLink", [ngAnimate])
  .directive("wonPostShareLink", genComponentConf).name;
