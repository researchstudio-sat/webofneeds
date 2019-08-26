import React from "react";
import { get } from "../utils.js";
import { connect } from "react-redux";
import * as usecaseUtils from "../usecase-utils.js";

import "~/style/_atom-content-details.scss";
import PropTypes from "prop-types";

const mapStateToProps = (state, ownProps) => {
  const atom = ownProps.atomUri && state.getIn(["atoms", ownProps.atomUri]);
  const details = ownProps.branch && get(atom, ownProps.branch);

  return {
    branch: ownProps.branch,
    atomUri: ownProps.atomUri,
    details,
  };
};

class WonAtomContentDetails extends React.Component {
  render() {
    const allDetailsImm = usecaseUtils.getAllDetailsImm();
    const contentDetailsMap =
      this.props.details &&
      this.props.details.map((contentDetail, contentDetailKey) => {
        const detailDefinitionImm = get(allDetailsImm, contentDetailKey);
        if (
          detailDefinitionImm &&
          !this.shouldOmitDetail(contentDetail, contentDetailKey)
        ) {
          const detailDefinition = detailDefinitionImm.toJS();
          const ReactViewerComponent =
            detailDefinition && detailDefinition.viewerComponent;

          if (ReactViewerComponent) {
            return (
              <div key={contentDetailKey} className="pis__component">
                <ReactViewerComponent
                  detail={detailDefinition}
                  content={contentDetail}
                />
              </div>
            );
          }
        }

        return undefined;
      });

    const contentDetailsArray = contentDetailsMap
      ? contentDetailsMap.toArray()
      : [];

    return (
      <won-atom-content-details>{contentDetailsArray}</won-atom-content-details>
    );
  }

  /**
   * Checks if the given contentDetailKey is "title" and the corresponding branch to view is "content"
   * @param contentDetailKey
   * @returns {boolean}
   */
  shouldOmitDetail(contentDetail, contentDetailKey) {
    return (
      !contentDetail ||
      (contentDetailKey === "title" && this.props.branch === "content")
    );
  }
}
WonAtomContentDetails.propTypes = {
  atomUri: PropTypes.string.isRequired,
  branch: PropTypes.string.isRequired,
  details: PropTypes.arrayOf(PropTypes.object),
};

export default connect(mapStateToProps)(WonAtomContentDetails);
