import angular from "angular";
import ngAnimate from "angular-animate";
import { actionCreators } from "../actions/actions.js";
import { attach, toAbsoluteURL } from "../utils.js";

import { connect2Redux } from "../won-utils.js";
import * as wonUtils from "../won-utils.js";
import { ownerBaseUrl } from "~/config/default.js";

import * as generalSelectors from "../selectors/general-selectors.js";

import "~/style/_post-share-link.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
        <div class="psl__content">
            <p class="psl__text" ng-if="(self.post.get('connections').size != 0 && self.isOwned) || !self.isOwned">
                Know someone who might also be interested in this posting? Consider sharing the link below in social media.
            </p>
            <div class="psl__tabs">
              <div class="psl__tabs__tab clickable" ng-class="{'psl__tabs__tab--selected': self.showLink}" ng-click="self.showLink = true">Link</div>
              <div class="psl__tabs__tab clickable" ng-class="{'psl__tabs__tab--selected': !self.showLink}" ng-click="self.showLink = false">QR-Code</div>
            </div>
            <div class="psl__link" ng-if="self.showLink">
              <div class="psl__link__copyfield" ng-if="self.showLink">
                <input class="psl__link__copyfield__input" value="{{self.linkToPost}}" readonly type="text" ng-focus="self.selectLink()" ng-blur="self.clearSelection()">
                <button class="red won-button--filled psl__link__copyfield__copy-button" ng-click="self.copyLink()">
                  <svg ng-if="self.copied" class="psl__link__copyfield__copy-button__icon" style="--local-primary:white;">
                    <use xlink:href="#ico16_checkmark" href="#ico16_checkmark"></use>
                  </svg>
                  <svg ng-if="!self.copied" class="psl__link__copyfield__copy-button__icon" style="--local-primary:white;">
                    <use xlink:href="#ico16_copy_to_clipboard" href="#ico16_copy_to_clipboard"></use>
                  </svg>
                </button>
              </div>
            </div>
            <div class="psl__qrcode" ng-if="!self.showLink">
              <img class="psl__qrcode__code" src="data:image/svg+xml;utf8,{{self.svgQrCodeToPost}}"/>
            </div>
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.postShareLink4dbg = this;

      const selectFromState = state => {
        const post = this.postUri && state.getIn(["atoms", this.postUri]);
        this.showLink = true;

        let linkToPost;
        if (ownerBaseUrl && post) {
          const path = "#!post/" + `?postUri=${encodeURI(this.postUri)}`;

          linkToPost = toAbsoluteURL(ownerBaseUrl).toString() + path;
        }
        let svgQrCodeToPost = wonUtils.generateSvgQrCode(linkToPost);

        return {
          post,
          isOwned: generalSelectors.isAtomOwned(state, this.postUri),
          linkToPost,
          svgQrCodeToPost,
        };
      };
      connect2Redux(selectFromState, actionCreators, ["self.postUri"], this);
    }

    getLinkField() {
      if (!this._linkField) {
        this._linkField = this.$element[0].querySelector(
          ".psl__link__copyfield__input"
        );
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
          linkEl.blur();

          //Temprorarily show checkmark
          this.copied = true;
          const self = this;
          setTimeout(() => {
            self.copied = false;
            self.$scope.$digest();
          }, 1000);
        }
      }
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
