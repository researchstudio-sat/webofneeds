import React from "react";

import "~/style/_petrinettransitionpicker.scss";
import PropTypes from "prop-types";

export default class WonPetrinetTransitionPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      selectedPetriNetUri:
        (props.initialValue && props.initialValue.petriNetUri) || "",
      selectedTransitionUri:
        (props.initialValue && props.initialValue.transitionUri) || "",
    };
  }

  render() {
    const showPetriNetUriResetButton =
      this.state.selectedPetriNetUri !== "" &&
      this.state.selectedPetriNetUri !== undefined;
    const showTransitionUriResetButton =
      this.state.selectedTransitionUri !== "" &&
      this.state.selectedTransitionUri !== undefined;
    return (
      <won-petrinettransition-picker>
        <div className="petrinettransitionp__petrineturi">
          <div className="petrinettransitionp__petrineturi__label">
            PetriNetUri:
          </div>
          <div className="petrinettransitionp__petrineturi__input">
            {showPetriNetUriResetButton ? (
              <svg
                className="petrinettransitionp__petrineturi__input__icon clickable"
                onClick={this.resetPetriNetUri.bind(this)}
              >
                <use xlinkHref="#ico36_close" href="#ico36_close" />
              </svg>
            ) : (
              undefined
            )}
            <input
              type="text"
              className="petrinettransitionp__petrineturi__input__inner"
              onChange={this.updatePetriNetUri.bind(this)}
              value={this.state.selectedPetriNetUri}
            />
          </div>
        </div>
        <div className="petrinettransitionp__transitionuri">
          <div className="petrinettransitionp__transitionuri__label">
            TransitionUri:
          </div>
          <div className="petrinettransitionp__transitionuri__input">
            {showTransitionUriResetButton ? (
              <svg
                className="petrinettransitionp__transitionuri__input__icon clickable"
                onClick={this.resetTransitionUri.bind(this)}
              >
                <use xlinkHref="#ico36_close" href="#ico36_close" />
              </svg>
            ) : (
              undefined
            )}
            <input
              type="text"
              className="petrinettransitionp__transitionuri__input__inner"
              onChange={this.updateTransitionUri.bind(this)}
              value={this.state.selectedTransitionUri}
            />
          </div>
        </div>
      </won-petrinettransition-picker>
    );
  }

  /**
   * Checks validity and uses callback method
   */
  update() {
    if (
      this.state.selectedPetriNetUri &&
      this.state.selectedPetriNetUri.trim().length > 0 &&
      this.state.selectedTransitionUri &&
      this.state.selectedTransitionUri.trim().length > 0
    ) {
      this.props.onUpdate({
        value: {
          petriNetUri: this.state.selectedPetriNetUri,
          transitionUri: this.state.selectedTransitionUri,
        },
      });
    } else {
      this.props.onUpdate({ value: undefined });
    }
  }

  updatePetriNetUri(event) {
    const text = event.target.value;

    if (text && text.trim().length > 0) {
      this.setState(
        {
          selectedPetriNetUri: text,
        },
        this.update.bind(this)
      );
    } else {
      this.resetPetriNetUri();
    }
  }

  updateTransitionUri(event) {
    const text = event.target.value;

    if (text && text.trim().length > 0) {
      this.setState(
        {
          selectedTransitionUri: text,
        },
        this.update.bind(this)
      );
    } else {
      this.resetTransitionUri();
    }
  }

  resetPetriNetUri() {
    this.setState(
      {
        selectedPetriNetUri: "",
      },
      this.update.bind(this)
    );
  }

  resetTransitionUri() {
    this.setState(
      {
        selectedTransitionUri: "",
      },
      this.update.bind(this)
    );
  }
}
WonPetrinetTransitionPicker.propTypes = {
  initialValue: PropTypes.string,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
