import React from "react";
import { get } from "../../../utils.js";

import "~/style/_paypalpayment-viewer.scss";
import PropTypes from "prop-types";

export default function WonPaypalPaymentViewer({ detail, content, className }) {
  const icon = detail.icon && (
    <svg className="paypalpaymentv__header__icon">
      <use xlinkHref={detail.icon} href={detail.icon} />
    </svg>
  );

  const label = detail.icon && (
    <span className="paypalpaymentv__header__label">{detail.label}</span>
  );

  function getPriceWithCurrency() {
    if (content && detail) {
      let currencyLabel = undefined;

      detail.currency &&
        detail.currency.forEach(curr => {
          if (curr.value === get(content, "currency")) {
            currencyLabel = curr.label;
          }
        });
      currencyLabel = currencyLabel || content.get("currency");

      return get(content, "amount") + currencyLabel;
    }
    return undefined;
  }

  return (
    <won-paypal-payment-viewer class={className}>
      <div className="paypalpaymentv__header">
        {icon}
        {label}
      </div>
      <div className="paypalpaymentv__content">
        <div className="paypalpaymentv__content__label">
          {detail.amountLabel}
        </div>
        <div className="paypalpaymentv__content__price">
          {getPriceWithCurrency()}
        </div>
        <div className="paypalpaymentv__content__label">
          {detail.receiverLabel}
        </div>
        <div className="paypalpaymentv__content__receiver">
          {get(content, "receiver")}
        </div>
      </div>
    </won-paypal-payment-viewer>
  );
}
WonPaypalPaymentViewer.propTypes = {
  detail: PropTypes.object,
  content: PropTypes.object,
  className: PropTypes.string,
};
