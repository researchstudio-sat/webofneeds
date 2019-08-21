import React from "react";

import "~/style/_descriptionpicker.scss";
import PropTypes from "prop-types";
import ReactMarkdown from "react-markdown";
import TextareaAutosize from "react-autosize-textarea";

export default class WonDescriptionPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: props.initialValue || "",
    };
  }

  render() {
    return (
      <won-description-picker>
        <div className="dp__input">
          {this.state.value && (
            <svg
              className="dp__input__icon clickable"
              onClick={this.reset.bind(this)}
            >
              <use xlinkHref="#ico36_close" href="#ico36_close" />
            </svg>
          )}
          <TextareaAutosize
            className="dp__input__inner"
            maxRows={6}
            placeholder={this.props.detail.placeholder}
            value={this.state.value}
            onChange={this.update.bind(this)}
          />
        </div>
        <div className="dp__preview__header">Preview</div>
        {this.state.value !== undefined && this.state.value !== "" ? (
          <ReactMarkdown
            className="dp__preview__content markdown"
            source={this.state.value}
          />
        ) : (
          <div className="dp__preview__content--empty">
            Add Content to see instant preview
          </div>
        )}
      </won-description-picker>
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
WonDescriptionPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
