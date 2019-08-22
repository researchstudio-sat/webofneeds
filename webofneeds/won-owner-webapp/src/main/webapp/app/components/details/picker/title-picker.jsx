import React from "react";

import "~/style/_titlepicker.scss";
import PropTypes from "prop-types";

export default class WonTitlePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: props.initialValue || "",
    };
  }

  render() {
    return (
      <won-title-picker
        class={this.props.className ? this.props.className : undefined}
      >
        <div className="titlep__input">
          {this.state.value && (
            <svg
              className="titlep__input__icon clickable"
              onClick={this.reset.bind(this)}
            >
              <use xlinkHref="#ico36_close" href="#ico36_close" />
            </svg>
          )}
          <input
            type="text"
            className="titlep__input__inner"
            placeholder={this.props.detail.placeholder}
            value={this.state.value}
            onChange={this.update.bind(this)}
          />
        </div>
      </won-title-picker>
    );
  }

  update(event) {
    const text = event.target.value;

    if (text.trim().length > 0) {
      this.setState({ value: text }, () =>
        this.props.onUpdate({ value: this.state.value.trim() })
      );
    } else {
      this.reset();
    }
  }

  reset() {
    this.setState(
      {
        value: "",
      },
      () => this.props.onUpdate({ value: undefined })
    );
  }
}
WonTitlePicker.propTypes = {
  className: PropTypes.string,
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
