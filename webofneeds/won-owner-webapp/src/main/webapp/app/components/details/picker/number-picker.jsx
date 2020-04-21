import React from "react";

import "~/style/_numberpicker.scss";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import PropTypes from "prop-types";
import { isValidNumber } from "../../../utils.js";

export default class WonNumberPicker extends React.Component {
  constructor(props) {
    super(props);
    const parsedNum = Number.parseFloat(props.initialValue);
    this.state = {
      value: isValidNumber(parsedNum) ? parsedNum : "",
    };
  }

  render() {
    return (
      <won-number-picker>
        <div className="numberp__input">
          <div className="numberp__input__reset clickable">
            {this.state.value !== "" &&
              this.state.value !== undefined && (
                <svg
                  className="numberp__input__reset__icon clickable"
                  onClick={() => this.reset(true)}
                >
                  <use xlinkHref={ico36_close} href={ico36_close} />
                </svg>
              )}
          </div>
          <input
            type="number"
            className="numberp__input__inner"
            placeholder={this.props.detail.placeholder}
            value={this.state.value}
            onChange={event => this.update(event, false)}
            onBlur={event => this.update(event, true)}
          />
        </div>
      </won-number-picker>
    );
  }

  update(event, resetInput) {
    const number = Number.parseFloat(event.target.value);

    if (isValidNumber(number)) {
      this.props.onUpdate({ value: number });
      this.setState({ value: number });
    } else {
      this.reset(resetInput || event.target.value === "");
    }
  }

  reset(resetInput) {
    if (resetInput) {
      this.setState(
        {
          value: "",
        },
        () => this.props.onUpdate({ value: undefined })
      );
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }
}
WonNumberPicker.propTypes = {
  initialValue: PropTypes.number,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
