import React from "react";

import PropTypes from "prop-types";
import WonTitlePicker from "./title-picker.jsx";

import "~/style/_reviewpicker.scss";

export default class WonReviewPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      text: props.initialValue && props.initialValue.text,
      rating:
        (props.initialValue && props.initialValue.rating) ||
        this.getDefaultRating(),
    };
  }

  render() {
    return (
      <won-review-picker>
        <div className="reviewp__input">
          <select
            className="reviewp__input__rating"
            onChange={this.updateRating.bind(this)}
            value={this.state.rating}
            disabled={
              !this.props.detail.rating || this.props.detail.rating.length <= 1
            }
          >
            {this.props.detail.rating &&
              this.props.detail.rating.map((r, index) => (
                <option key={r.value + "-" + index} value={r.value}>
                  {r.label}
                </option>
              ))}
          </select>
          <WonTitlePicker
            onUpdate={this.updateText.bind(this)}
            initialValue={
              this.props.initialValue && this.props.initialValue.text
            }
            detail={{ placeholder: this.props.detail.placeholder }}
          />
        </div>
      </won-review-picker>
    );
  }

  getDefaultRating() {
    let defaultRating;

    this.props.detail &&
      this.props.detail.rating.forEach(rating => {
        if (rating.default) defaultRating = rating.value;
      });

    return defaultRating;
  }

  updateText({ value }) {
    this.setState({ text: value }, this.update.bind(this));
  }

  updateRating(event) {
    const rating = event.target.value;
    console.debug("Rating: ", rating);

    this.setState({ rating: rating }, this.update.bind(this));
  }

  update() {
    if (this.state.rating) {
      this.props.onUpdate({
        value: this.state,
      });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }
}
WonReviewPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
