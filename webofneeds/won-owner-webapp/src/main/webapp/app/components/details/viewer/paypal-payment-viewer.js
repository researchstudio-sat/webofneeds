import angular from "angular";
import "ng-redux";
import { actionCreators } from "../../../actions/actions.js";
import postHeaderModule from "../../post-header.js";
import { attach } from "../../../cstm-ng-utils.js";
import { connect2Redux } from "../../../configRedux.js";

import "~/style/_paypalpayment-viewer.scss";

const serviceDependencies = ["$scope", "$ngRedux", "$element"];
function genComponentConf() {
  let template = `
        <div class="paypalpaymentv__header">
          <svg class="paypalpaymentv__header__icon" ng-if="self.detail.icon">
              <use xlink:href={{self.detail.icon}} href={{self.detail.icon}}></use>
          </svg>
          <span class="paypalpaymentv__header__label" ng-if="self.detail.label">{{self.detail.label}}</span>
        </div>
        <div class="paypalpaymentv__content">
          <div class="paypalpaymentv__content__label">
            {{ self.detail.amountLabel }}
          </div>
          <div class="paypalpaymentv__content__price">
            {{ self.getPriceWithCurrency() }}
          </div>
          <div class="paypalpaymentv__content__label">
            {{ self.detail.receiverLabel }}
          </div>
          <div class="paypalpaymentv__content__receiver">
            {{ self.content.get("receiver") }}
          </div>
          <!-- Not in use: secret
          <div class="paypalpaymentv__content__label">
            {{ self.detail.secretLabel }}
          </div>
          <div class="paypalpaymentv__content__secret">
            {{ self.content.get("secret") }}
          </div>
          -->
          <!-- Not in use: Customer information
          <div class="paypalpaymentv__content__label span-column">
            {{ self.detail.customerLabel }}
          </div>
          <div class="paypalpaymentv__content__customer" ng-if="self.customerPost">
            <won-post-header
                atom-uri="self.customerPost.get('uri')"
                timestamp="self.customerPost.get('creationDate')"
                hide-image="::false">
            </won-post-header>
          </div>
          <button class="paypalpaymentv__content__action span-column won-button--outlined thin red"
              ng-if="!self.customerPost"
              ng-click="self.loadPost()">
              Click to Load Customer Post
          </button>
          -->
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.paypalpaymentv4dbg = this;

      const selectFromState = state => {
        const customerUri = this.content && this.content.get("customerUri");

        const customerPost = customerUri && state.getIn(["atoms", customerUri]);

        return {
          customerUri,
          customerPost,
        };
      };

      connect2Redux(
        selectFromState,
        actionCreators,
        ["self.content", "self.detail"],
        this
      );
    }

    getPriceWithCurrency() {
      if (this.content && this.detail) {
        let currencyLabel = undefined;

        this.detail.currency &&
          this.detail.currency.forEach(curr => {
            if (curr.value === this.content.get("currency")) {
              currencyLabel = curr.label;
            }
          });
        currencyLabel = currencyLabel || this.content.get("currency");

        return this.content.get("amount") + currencyLabel;
      }
      return undefined;
    }

    // loadPost() {
    //   if (this.customerUri) {
    //     this.atoms__fetchSuggested(this.customerUri);
    //   }
    // }
  }
  Controller.$inject = serviceDependencies;

  return {
    restrict: "E",
    controller: Controller,
    controllerAs: "self",
    bindToController: true, //scope-bindings -> ctrl
    scope: {
      content: "=",
      detail: "=",
    },
    template: template,
  };
}

export default angular
  .module("won.owner.components.paypalPaymentViewer", [postHeaderModule])
  .directive("wonPaypalPaymentViewer", genComponentConf).name;
