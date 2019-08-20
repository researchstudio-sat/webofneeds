import React from "react";

import "~/style/_numberpicker.scss";
import PropTypes from "prop-types";
import { isValidNumber } from "../../../utils.js";

export default class WonNumberPicker extends React.Component {
  constructor(props) {
    super(props);
    const parsedNum = Number.parseFloat(this.initialValue);
    this.state = {
      value: isValidNumber(parsedNum) ? parsedNum : undefined,
    };
  }

  render() {
    return (
      <won-number-picker>
        <div className="numberp__input">
          {this.state.value && (
            <svg
              className="numberp__input__icon clickable"
              onClick={() => this.reset(true)}
            >
              <use xlinkHref="#ico36_close" href="#ico36_close" />
            </svg>
          )}
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
      this.addedNumber = number;
      this.update(this.addedNumber);
      this.showResetButton = true;
    } else {
      this.reset(resetInput);
    }
  }

  reset(resetInput) {
    this.props.onUpdate({ value: undefined });

    if (resetInput) {
      this.setState({
        value: undefined,
      });
    }
  }
}
WonNumberPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
