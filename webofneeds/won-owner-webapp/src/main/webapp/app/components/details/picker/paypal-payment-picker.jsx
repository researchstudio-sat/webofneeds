import React from "react";

import "~/style/_paypalpaymentpicker.scss";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import PropTypes from "prop-types";
import { isValidNumber } from "../../../utils";

export default class WonPaypalPaymentPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      amount: (this.initialValue && this.initialValue.amount) || "",
      currency: (this.initialValue && this.initialValue.currency) || "EUR",
      receiver: (this.initialValue && this.initialValue.receiver) || "",
    };
  }

  render() {
    const showAmountResetButton =
      this.state.amount !== "" && this.state.amount !== undefined;
    const showReceiverResetButton =
      this.state.receiver !== "" && this.state.receiver !== undefined;
    return (
      <won-paypal-payment-picker>
        <div className="paypalpaymentp__label">
          {this.props.detail.amountLabel}
        </div>
        <div className="paypalpaymentp__input">
          <div className="paypalpaymentp__input__reset clickable">
            {showAmountResetButton && (
              <svg
                className="paypalpaymentp__input__reset__icon"
                onClick={() => this.resetNumber(true)}
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            )}
          </div>
          <input
            type="number"
            className={
              "paypalpaymentp__input__amount " +
              (showAmountResetButton
                ? "paypalpaymentp__input__amount--withreset"
                : "")
            }
            placeholder={this.props.detail.amountPlaceholder}
            value={this.state.amount}
            onChange={event => this.updateNumber(event, false)}
            onBlur={event => this.updateNumber(event, true)}
          />
          <select
            className="paypalpaymentp__input__currency"
            disabled={this.props.detail.currency.length <= 1}
            onChange={this.updateCurrency.bind(this)}
            value={this.state.currency}
          >
            {this.props.detail.currency.map((currency, index) => (
              <option key={currency.value + "-" + index} value={currency.value}>
                {currency.label}
              </option>
            ))}
          </select>
        </div>
        <div className="paypalpaymentp__label">
          {this.props.detail.receiverLabel}
        </div>
        <div className="paypalpaymentp__textinput">
          <input
            type="email"
            className={
              "paypalpaymentp__textinput__receiver " +
              (showReceiverResetButton
                ? "paypalpaymentp__textinput__receiver--withreset"
                : "")
            }
            placeholder={this.props.detail.receiverPlaceholder}
            onBlur={event => this.updateReceiver(event, true)}
            onChange={event => this.updateReceiver(event, false)}
            value={this.state.receiver}
          />
          <div className="paypalpaymentp__textinput__reset clickable">
            {showReceiverResetButton && (
              <svg
                className="paypalpaymentp__textinput__reset__icon"
                onClick={this.resetReceiver.bind(this)}
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            )}
          </div>
        </div>
      </won-paypal-payment-picker>
    );
  }

  resetNumber(resetInput) {
    if (resetInput) {
      this.setState(
        {
          amount: "",
          currency: this.getDefaultCurrency(),
        },
        this.update.bind(this)
      );
    } else {
      this.props.onUpdate({
        value: undefined,
      });
    }
  }

  getDefaultCurrency() {
    let defaultCurrency;

    this.props.detail &&
      this.props.detail.currency.forEach(curr => {
        if (curr.default) defaultCurrency = curr.value;
      });

    return defaultCurrency;
  }

  resetReceiver() {
    this.setState(
      {
        receiver: "",
      },
      this.update.bind(this)
    );
  }

  updateNumber(event, resetInput) {
    const amount = Number.parseFloat(event.target.value);

    if (isValidNumber(amount)) {
      this.setState(
        {
          amount: amount,
        },
        this.update.bind(this)
      );
    } else {
      this.resetNumber(resetInput || event.target.value === "");
    }
  }

  updateReceiver(event) {
    //TODO: check if receiver is valid email
    this.setState(
      {
        receiver: event.target.value,
      },
      this.update.bind(this)
    );
  }

  updateCurrency(event) {
    const currency = event.target.value;
    this.setState(
      {
        currency: currency,
      },
      this.update.bind(this)
    );
  }

  /**
   * Checks validity and uses callback method
   */
  update() {
    if (
      isValidNumber(this.state.amount) &&
      this.state.currency &&
      this.state.receiver
    ) {
      this.props.onUpdate({
        value: this.state,
      });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }
}
WonPaypalPaymentPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
