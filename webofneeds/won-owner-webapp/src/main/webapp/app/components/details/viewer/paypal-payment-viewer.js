import angular from "angular";
import "ng-redux";
import { actionCreators } from "../../../actions/actions.js";
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
        </div>
    `;

  class Controller {
    constructor() {
      attach(this, serviceDependencies, arguments);
      window.paypalpaymentv4dbg = this;

      const selectFromState = () => ({});

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
  .module("won.owner.components.paypalPaymentViewer", [])
  .directive("wonPaypalPaymentViewer", genComponentConf).name;
