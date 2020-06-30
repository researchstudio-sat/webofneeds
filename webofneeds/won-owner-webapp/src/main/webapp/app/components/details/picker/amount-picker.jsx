import React from "react";

import "~/style/_pricepicker.scss";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import PropTypes from "prop-types";
import { isValidNumber } from "../../../utils.js";

export default class WonAmountPicker extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      amount: (props.initialValue && props.initialValue.amount) || "",
      unit:
        (props.initialValue && props.initialValue.unit) ||
        this.getDefaultUnit() ||
        "",
    };
  }

  render() {
    const showResetButton =
      this.state.amount !== "" && this.state.amount !== undefined;
    return (
      <won-price-picker>
        <div className="pricep__input">
          <div className="pricep__input__reset clickable">
            {showResetButton && (
              <svg
                className="pricep__input__reset__icon clickable"
                onClick={() => this.reset(true)}
              >
                <use xlinkHref={ico36_close} href={ico36_close} />
              </svg>
            )}
          </div>
          <input
            type="number"
            className={
              "pricep__input__inner " +
              (showResetButton ? "pricep__input__inner--withreset" : "")
            }
            placeholder={this.props.detail.placeholder}
            value={this.state.amount}
            onChange={event => this.updateAmount(event, false)}
            onBlur={event => this.updateAmount(event, true)}
          />
          <select
            className="pricep__input__unit"
            disabled={this.props.detail.unit.length <= 1}
            onChange={this.updateUnit.bind(this)}
            value={this.state.unit}
          >
            {this.props.detail.unit.map((unit, index) => (
              <option key={unit.value + "-" + index} value={unit.value}>
                {unit.label}
              </option>
            ))}
          </select>
        </div>
      </won-price-picker>
    );
  }

  update() {
    if (isValidNumber(this.state.amount) && this.state.unit) {
      this.props.onUpdate({ value: this.state });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }

  updateUnit(event) {
    const unit = event.target.value;
    this.setState(
      {
        unit: unit,
      },
      this.update.bind(this)
    );
  }

  updateAmount(event, resetInput) {
    const amount = Number.parseFloat(event.target.value);

    if (isValidNumber(amount)) {
      this.setState(
        {
          amount: amount,
        },
        this.update.bind(this)
      );
    } else {
      this.reset(resetInput || event.target.value === "");
    }
  }

  reset(resetInput) {
    if (resetInput) {
      this.setState(
        {
          amount: "",
          unit: this.getDefaultUnit(),
        },
        this.update.bind(this)
      );
    } else {
      this.props.onUpdate({
        value: undefined,
      });
    }
  }

  getDefaultUnit() {
    let defaultUnit;

    this.props.detail &&
      this.props.detail.unit.forEach(unit => {
        if (unit.default) defaultUnit = unit.value;
      });

    return defaultUnit;
  }
}
WonAmountPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
