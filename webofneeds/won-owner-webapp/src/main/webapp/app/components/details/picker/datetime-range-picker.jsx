import React from "react";

import "~/style/_datetimepicker.scss";
import PropTypes from "prop-types";
import WonDatetimePicker from "./datetime-picker";

export default class WonDatetimeRangePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = this.initialValue || {};
  }

  render() {
    return (
      <won-datetime-range-picker>
        From
        <WonDatetimePicker
          initialValue={
            this.props.initialValue && this.props.initialValue.fromDatetime
          }
          onUpdate={this.updateFromDatetime.bind(this)}
          detail={this.props.detail && this.props.detail.fromDatetime}
        />
        To
        <WonDatetimePicker
          initialValue={
            this.props.initialValue && this.props.initialValue.toDatetime
          }
          onUpdate={this.updateToDatetime.bind(this)}
          detail={this.props.detail && this.props.detail.toDatetime}
        />
      </won-datetime-range-picker>
    );
  }

  updateFromDatetime({ value }) {
    this.setState({ fromDatetime: value }, this.update.bind(this));
  }

  updateToDatetime({ value }) {
    this.setState({ toDatetime: value }, this.update.bind(this));
  }

  update() {
    // TODO: think about adding sanity checking (from < to)
    if (this.state.fromDatetime || this.state.toDatetime) {
      this.props.onUpdate({ value: this.state });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }
}
WonDatetimeRangePicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
