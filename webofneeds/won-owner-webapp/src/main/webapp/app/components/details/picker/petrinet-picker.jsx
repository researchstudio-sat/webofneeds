import React from "react";

import WonFileDropzone from "../../file-dropzone.jsx";
import PropTypes from "prop-types";

import "~/style/_petrinetpicker.scss";

export default class WonPetrinetPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      addedWorkflow: props.initialValue,
    };
  }

  render() {
    return (
      <won-petrinet-picker>
        {this.state.addedWorkflow ? (
          <div className="petrinetp__preview">
            <div className="petrinetp__preview__label clickable">
              {this.state.addedWorkflow.name}
            </div>
            <svg
              className="petrinetp__preview__remove"
              onClick={this.removeWorkflow.bind(this)}
            >
              <use xlinkHref="#ico36_close" href="#ico36_close" />
            </svg>
            <svg className="petrinetp__preview__typeicon">
              <use
                xlinkHref="#ico36_uc_transport_demand"
                href="#ico36_uc_transport_demand"
              />
            </svg>
          </div>
        ) : (
          <WonFileDropzone
            onFilePicked={this.updateWorkflow.bind(this)}
            accepts={this.props.detail.accepts}
            multiSelect={false}
          />
        )}
      </won-petrinet-picker>
    );
  }

  updateWorkflow({ file }) {
    this.setState({ addedWorkflow: file }, () => {
      this.update();
    });
  }

  removeWorkflow() {
    this.setState({ addedWorkflow: undefined }, this.update.bind(this));
  }
  update() {
    this.props.onUpdate({ value: this.state.addedWorkflow });
  }
}
WonPetrinetPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
