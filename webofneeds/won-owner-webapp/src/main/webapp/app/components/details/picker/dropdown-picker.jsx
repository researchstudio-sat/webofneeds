import React from "react";

import "~/style/_dropdownpicker.scss";
import PropTypes from "prop-types";

export default class WonDropdownPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = { value: props.initialValue };
  }

  render() {
    return (
      <won-dropdown-picker>
        <div className="dropdownp__input">
          <select
            type="text"
            className="dropdownp__input__inner"
            value={this.state.value}
            onChange={this.update.bind(this)}
          >
            <option value="">{self.detail.placeholder}</option>
            {this.props.detail.options.map((option, index) => (
              <option
                key={option.value + "-" + index}
                ng-repeat="option in this.props.detail.options"
                value={option.value}
              >
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </won-dropdown-picker>
    );
  }

  update(event) {
    const value = event.target.value;
    this.setState(
      {
        value: value,
      },
      () => {
        if (this.state.value && this.state.value !== "") {
          this.props.onUpdate({ value: value });
        } else {
          this.props.onUpdate({ value: undefined });
        }
      }
    );
  }
}
WonDropdownPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
