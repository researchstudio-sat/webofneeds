import React from "react";

import "~/style/_pricepicker.scss";
import PropTypes from "prop-types";
import { isValidNumber } from "../../../utils";

export default class WonPricePicker extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      amount: (props.initialValue && props.initialValue.amount) || "",
      currency:
        (props.initialValue && props.initialValue.currency) ||
        this.getDefaultCurrency() ||
        "EUR",
      unitCode:
        (props.initialValue && props.initialValue.unitCode) ||
        this.getDefaultUnitCode() ||
        "",
    };
  }

  render() {
    return (
      <won-price-picker>
        <div className="pricep__input">
          <div className="pricep__input__reset clickable">
            {this.state.amount !== "" &&
              this.state.amount !== undefined && (
                <svg
                  className="pricep__input__reset__icon clickable"
                  onClick={() => this.reset(true)}
                >
                  <use xlinkHref="#ico36_close" href="#ico36_close" />
                </svg>
              )}
          </div>
          <input
            type="number"
            className="pricep__input__inner"
            placeholder={this.props.detail.placeholder}
            value={this.state.amount}
            onChange={event => this.updateAmount(event, false)}
            onBlur={event => this.updateAmount(event, true)}
          />
          <select
            className="pricep__input__currency"
            disabled={this.props.detail.currency.length <= 1}
            onChange={event => this.updateCurrency(event)}
            value={this.state.currency}
          >
            {this.props.detail.currency.map((currency, index) => (
              <option key={currency + "-" + index} value={currency.value}>
                {currency.label}
              </option>
            ))}
          </select>
          {!this.totalUnitCodeOnly() && (
            <select
              className="pricep__input__unitCode"
              disabled={this.props.detail.unitCode.length <= 1}
              onChange={event => this.updateUnitCode(event)}
              value={this.state.unitCode}
            >
              {this.props.detail.unitCode.map((unitCode, index) => (
                <option key={unitCode + "-" + index} value={unitCode.value}>
                  {unitCode.label}
                </option>
              ))}
            </select>
          )}
        </div>
      </won-price-picker>
    );
  }

  updateUnitCode(event) {
    const unitCode = event.target.value;
    this.setState(
      {
        unitCode: unitCode,
      },
      this.update.bind(this)
    );
  }

  update() {
    if (isValidNumber(this.state.amount)) {
      this.props.onUpdate({ value: this.state });
    } else {
      this.props.onUpdate({ value: undefined });
    }
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

  updateAmount(event, resetInput) {
    if (resetInput) console.debug("CALLED FROM onBlur");
    const amount = Number.parseFloat(event.target.value);

    if (isValidNumber(amount)) {
      this.setState(
        {
          amount: amount,
        },
        this.update.bind(this)
      );
    } else {
      this.reset(resetInput);
    }
  }

  reset(resetInput) {
    if (resetInput) {
      this.setState(
        {
          amount: "",
          currency: this.getDefaultCurrency(),
          unitCode: this.getDefaultUnitCode(),
        },
        this.update.bind(this)
      );
    } else {
      this.props.onUpdate({
        value: undefined,
      });
    }
  }

  /**
   * If there is no unitCode present in the given detail other than the "" blank/total unit code then we do not show any dropdown picker
   * @returns {boolean}
   */
  totalUnitCodeOnly() {
    const unitCode = this.props.detail && this.props.detail.unitCode;
    return unitCode && unitCode.length == 1 && unitCode[0].value == "";
  }

  getDefaultCurrency() {
    let defaultCurrency;

    this.props.detail &&
      this.props.detail.currency.forEach(curr => {
        if (curr.default) defaultCurrency = curr.value;
      });

    return defaultCurrency;
  }
  getDefaultUnitCode() {
    let defaultUnitCode;

    this.props.detail &&
      this.props.detail.unitCode.forEach(uc => {
        if (uc.default) defaultUnitCode = uc.value;
      });

    return defaultUnitCode;
  }
}
WonPricePicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
