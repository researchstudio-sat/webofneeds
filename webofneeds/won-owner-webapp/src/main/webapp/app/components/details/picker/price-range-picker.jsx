import React from "react";

import "~/style/_pricerangepicker.scss";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import PropTypes from "prop-types";
import { isValidNumber } from "../../../utils.js";

export default class WonPriceRangePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      min: (props.initialValue && props.initialValue.min) || "",
      max: (props.initialValue && props.initialValue.max) || "",
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
    const showMinResetButton =
      this.state.min !== "" && this.state.min !== undefined;
    const showMaxResetButton =
      this.state.max !== "" && this.state.max !== undefined;
    return (
      <won-price-range-picker>
        <div className="pricerangep__input">
          <label className="pricerangep__input__label">
            {this.props.detail.minLabel}
          </label>
          <div className="pricerangep__input__reset clickable">
            {showMinResetButton && (
              <svg
                className="pricerangep__input__reset__icon clickable"
                onClick={() => this.resetMin(true)}
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            )}
          </div>
          <input
            type="number"
            className={
              "pricerangep__input__min " +
              (showMinResetButton ? " pricerangep__input__min--withreset " : "")
            }
            placeholder={this.props.detail.minPlaceholder}
            value={this.state.min}
            onChange={event => this.updateMin(event, false)}
            onBlur={event => this.updateMin(event, true)}
          />
        </div>
        <div className="pricerangep__input">
          <label className="pricerangep__input__label">
            {this.props.detail.maxLabel}
          </label>
          <div className="pricerangep__input__reset clickable">
            {showMaxResetButton && (
              <svg
                className="pricerangep__input__reset__icon clickable"
                onClick={() => this.resetMax(true)}
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            )}
          </div>
          <input
            type="number"
            className={
              "pricerangep__input__max " +
              (showMaxResetButton ? " pricerangep__input__max--withreset " : "")
            }
            placeholder={this.props.detail.maxPlaceholder}
            value={this.state.max}
            onChange={event => this.updateMax(event, false)}
            onBlur={event => this.updateMax(event, true)}
          />
        </div>
        <div className="pricerangep__input">
          <label className="pricerangep__input__label">Currency</label>
          <select
            className="pricerangep__input__currency"
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
        {!this.totalUnitCodeOnly() && (
          <select
            className="pricerangep__input__unitCode"
            disabled={this.props.detail.unitCode.length <= 1}
            onChange={this.updateUnitCode.bind(this)}
            value={this.state.unitCode}
          >
            {this.props.detail.unitCode.map((unitCode, index) => (
              <option key={unitCode.value + "-" + index} value={unitCode.value}>
                {unitCode.label}
              </option>
            ))}
          </select>
        )}
      </won-price-range-picker>
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
    if (
      (isValidNumber(this.state.min) || isValidNumber(this.state.max)) &&
      this.state.currency
    ) {
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

  updateMin(event, resetInput) {
    const min = Number.parseFloat(event.target.value);

    if (isValidNumber(min)) {
      this.setState(
        {
          min: min,
        },
        this.update.bind(this)
      );
    } else {
      this.resetMin(resetInput || event.target.value === "");
    }
  }

  updateMax(event, resetInput) {
    const max = Number.parseFloat(event.target.value);

    if (isValidNumber(max)) {
      this.setState(
        {
          max: max,
        },
        this.update.bind(this)
      );
    } else {
      this.resetMax(resetInput || event.target.value === "");
    }
  }

  resetMin(resetInput) {
    if (resetInput) {
      this.setState(
        {
          min: "",
        },
        this.update.bind(this)
      );
    } else {
      this.props.onUpdate({
        value: undefined,
      });
    }
  }

  resetMax(resetInput) {
    if (resetInput) {
      this.setState(
        {
          max: "",
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
WonPriceRangePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
