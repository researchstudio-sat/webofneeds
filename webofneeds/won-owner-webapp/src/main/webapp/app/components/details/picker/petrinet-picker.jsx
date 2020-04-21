import React from "react";

import WonFileDropzone from "../../file-dropzone.jsx";
import PropTypes from "prop-types";

import "~/style/_petrinetpicker.scss";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_uc_transport_demand from "~/images/won-icons/ico36_uc_transport_demand.svg";

export default class WonPetrinetPicker extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      addedWorkflow: props.initialValue,
    };
    this.update = this.update.bind(this);
    this.updateWorkflow = this.updateWorkflow.bind(this);
    this.removeWorkflow = this.removeWorkflow.bind(this);
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
              onClick={this.removeWorkflow}
            >
              <use xlinkHref={ico36_close} href={ico36_close} />
            </svg>
            <svg className="petrinetp__preview__typeicon">
              <use
                xlinkHref={ico36_uc_transport_demand}
                href={ico36_uc_transport_demand}
              />
            </svg>
          </div>
        ) : (
          <WonFileDropzone
            onFilePicked={this.updateWorkflow}
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
    this.setState({ addedWorkflow: undefined }, this.update);
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
