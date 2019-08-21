import React from "react";

import "~/style/_datetimepicker.scss";
import PropTypes from "prop-types";
import DateTimePicker from "react-datetime-picker";
import { isValidDate } from "../../../utils.js";

export default class WonDatetimePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      date: props.initialValue,
    };
  }

  render() {
    return (
      <won-datetime-picker>
        <button
          className="datetimep__button won-button--filled red"
          onClick={this.currentDatetime.bind(this)}
        >
          Now
        </button>
        <DateTimePicker
          onChange={this.onChange.bind(this)}
          value={this.state.date}
        />
      </won-datetime-picker>
    );
  }

  onChange(date) {
    this.setState({ date: date }, this.update.bind(this));
    console.debug("Date: ", date);
  }

  currentDatetime() {
    this.setState({ date: new Date() }, this.update.bind(this));
  }

  update() {
    if (isValidDate(this.state.date)) {
      this.props.onUpdate({ value: this.state.date });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }
}
WonDatetimePicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
