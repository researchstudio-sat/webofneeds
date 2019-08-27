import React from "react";

import "~/style/_rangepicker.scss";
import PropTypes from "prop-types";
import { isValidNumber } from "../../../utils.js";

export default class WonRangePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      min: (props.initialValue && props.initialValue.min) || "",
      max: (props.initialValue && props.initialValue.max) || "",
    };
  }

  render() {
    const showMinResetButton =
      this.state.min !== "" && this.state.min !== undefined;
    const showMaxResetButton =
      this.state.max !== "" && this.state.max !== undefined;
    return (
      <won-range-picker>
        <div className="rangep__input">
          <label className="rangep__input__label">
            {this.props.detail.minLabel}
          </label>
          <div className="rangep__input__reset clickable">
            {showMinResetButton && (
              <svg
                className="rangep__input__reset__icon clickable"
                onClick={() => this.resetMin(true)}
              >
                <use xlinkHref="#ico36_close" href="#ico36_close" />
              </svg>
            )}
          </div>
          <input
            type="number"
            className={
              "rangep__input__min " +
              (showMinResetButton ? " rangep__input__min--withreset " : "")
            }
            placeholder={this.props.detail.minPlaceholder}
            value={this.state.min}
            onChange={event => this.updateMin(event, false)}
            onBlur={event => this.updateMin(event, true)}
          />
        </div>
        <div className="rangep__input">
          <label className="rangep__input__label">
            {this.props.detail.maxLabel}
          </label>
          <div className="rangep__input__reset clickable">
            {showMaxResetButton && (
              <svg
                className="rangep__input__reset__icon clickable"
                onClick={() => this.resetMax(true)}
              >
                <use xlinkHref="#ico36_close" href="#ico36_close" />
              </svg>
            )}
          </div>
          <input
            type="number"
            className={
              "rangep__input__max " +
              (showMaxResetButton ? " rangep__input__max--withreset " : "")
            }
            placeholder={this.props.detail.maxPlaceholder}
            value={this.state.max}
            onChange={event => this.updateMax(event, false)}
            onBlur={event => this.updateMax(event, true)}
          />
        </div>
      </won-range-picker>
    );
  }

  update() {
    if (isValidNumber(this.state.min) || isValidNumber(this.state.max)) {
      this.props.onUpdate({ value: this.state });
    } else {
      this.props.onUpdate({ value: undefined });
    }
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
}
WonRangePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
