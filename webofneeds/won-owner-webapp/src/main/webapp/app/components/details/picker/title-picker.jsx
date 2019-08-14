import React from "react";

import "~/style/_titlepicker.scss";
import PropTypes from "prop-types";

export default class WonTitlePicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      addedTitle: props.initialValue,
    };
  }

  render() {
    return (
      <won-title-picker>
        <div className="titlep__input">
          {this.state.addedTitle && (
            <svg
              className="titlep__input__icon clickable"
              onClick={() => this.resetTitle()}
            >
              <use xlinkHref="#ico36_close" href="#ico36_close" />
            </svg>
          )}
          <input
            type="text"
            className="titlep__input__inner"
            placeholder={this.props.detail.placeholder}
            value={this.state.addedTitle}
            onChange={event => this.updateTitle(event)}
          />
        </div>
      </won-title-picker>
    );
  }

  updateTitle(event) {
    const text = event.target.value;

    if (text.trim().length > 0) {
      this.setState({ addedTitle: text });
      this.props.onUpdate({ value: text.trim() });
    }
  }

  resetTitle() {
    this.props.onUpdate({ value: undefined });

    this.setState({
      addedTitle: undefined,
    });
  }
}
WonTitlePicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
