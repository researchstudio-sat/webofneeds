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
      <won-title-picker>
        <div className="titlep__input">
          {this.state.value && (
            <svg
              className="titlep__input__icon clickable"
              onClick={() => this.reset()}
            >
              <use xlinkHref="#ico36_close" href="#ico36_close" />
            </svg>
          )}
          <input
            type="text"
            className="titlep__input__inner"
            placeholder={this.props.detail.placeholder}
            value={this.state.value}
            onChange={event => this.update(event)}
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
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
