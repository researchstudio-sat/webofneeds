import React from "react";
import { get } from "../../../utils.js";

import "~/style/_paypalpayment-viewer.scss";
import PropTypes from "prop-types";

export default class WonPaypalPaymentViewer extends React.Component {
  render() {
    const icon = this.props.detail.icon && (
      <svg className="paypalpaymentv__header__icon">
        <use xlinkHref={this.props.detail.icon} href={this.props.detail.icon} />
      </svg>
    );

    const label = this.props.detail.icon && (
      <span className="paypalpaymentv__header__label">
        {this.props.detail.label}
      </span>
    );

    return (
      <won-paypal-payment-viewer class={this.props.className}>
        <div className="paypalpaymentv__header">
          {icon}
          {label}
        </div>
        <div className="paypalpaymentv__content">
          <div className="paypalpaymentv__content__label">
            {this.props.detail.amountLabel}
          </div>
          <div className="paypalpaymentv__content__price">
            {this.getPriceWithCurrency()}
          </div>
          <div className="paypalpaymentv__content__label">
            {this.props.detail.receiverLabel}
          </div>
          <div className="paypalpaymentv__content__receiver">
            {get(this.props.content, "receiver")}
          </div>
        </div>
      </won-paypal-payment-viewer>
    );
  }

  getPriceWithCurrency() {
    if (this.props.content && this.props.detail) {
      let currencyLabel = undefined;

      this.props.detail.currency &&
        this.props.detail.currency.forEach(curr => {
          if (curr.value === get(this.props.content, "currency")) {
            currencyLabel = curr.label;
          }
        });
      currencyLabel = currencyLabel || this.content.get("currency");

      return get(this.props.content, "amount") + currencyLabel;
    }
    return undefined;
  }
}
WonPaypalPaymentViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
