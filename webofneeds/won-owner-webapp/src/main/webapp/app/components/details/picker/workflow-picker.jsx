import React from "react";

import "~/style/_workflowpicker.scss";
import ico36_close from "~/images/won-icons/ico36_close.svg";
import ico36_uc_transport_demand from "~/images/won-icons/ico36_uc_transport_demand.svg";
import PropTypes from "prop-types";
import WonFileDropzone from "../../file-dropzone";

export default class WonWorkflowPicker extends React.Component {
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
      <won-workflow-picker>
        {this.state.addedWorkflow ? (
          <div className="workflowp__preview">
            <div className="workflowp__preview__label clickable">
              {this.state.addedWorkflow.name}
            </div>
            <svg
              className="workflowp__preview__remove"
              onClick={this.removeWorkflow}
            >
              <use xlinkHref={ico36_close} href={ico36_close} />
            </svg>
            <svg className="workflowp__preview__typeicon">
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
      </won-workflow-picker>
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
WonWorkflowPicker.propTypes = {
  initialValue: PropTypes.object,
  detail: PropTypes.object,
  onUpdate: PropTypes.func.isRequired,
};
