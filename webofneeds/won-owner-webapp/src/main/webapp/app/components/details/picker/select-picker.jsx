import React from "react";

import PropTypes from "prop-types";

export default class WonSelectPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {};
  }

  render() {
    let optionElements;
    if (
      this.props.detail &&
      this.props.detail.options &&
      this.props.detail.options.length > 0
    ) {
      this.props.detail.options.map((option, index) => (
        <label
          key={option.label + "-" + index}
          onClick={this.updateSelects.bind(this)}
          className="selectp__input__inner"
        >
          <input
            className="selectp__input__inner__select"
            type={this.getSelectType()}
            value={option.value}
            checked={() => this.isChecked(option)}
          />
          {option.label}
        </label>
      ));
    }

    return (
      <won-select-picker>
        <div>
          TODO: THIS IS NOT IMPLEMENTED YET (no detail uses it as of now)
        </div>
        {optionElements}
      </won-select-picker>
    );
  }

  getSelectType() {
    return this.props.detail && this.props.detail.multiSelect
      ? "checkbox"
      : "radio";
  }

  isChecked(option) {
    if (this.props.initialValue) {
      for (const key in this.props.initialValue) {
        if (this.props.initialValue[key] === option.value) {
          return true;
        }
      }
    }
    return false;
  }

  getInputName() {
    /*TODO: Implement a sort of hashcode to prepend to the returned name to add the
    possibility of using the same identifier in is and seeks for these pickers*/
    return this.props.detail.identifier;
  }

  /**
   * Checks validity and uses callback method
   */
  update(selectedValues) {
    // check if there are tags
    if (selectedValues && selectedValues.length > 0) {
      this.props.onUpdate({ value: selectedValues });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }

  selectedFields() {
    //TODO: FIND SELECTED FIELDS
    return [{ value: "test" }];
  }

  updateSelects() {
    let selectedValues = [];
    this.selectedFields().forEach(selected =>
      selectedValues.push(selected.value)
    );
    this.update(selectedValues);
  }
}
WonSelectPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
