import React from "react";
import { get } from "../utils.js";
import { actionCreators } from "../actions/actions.js";
import * as usecaseUtils from "../usecase-utils.js";

import "~/style/_atom-content-details.scss";
import PropTypes from "prop-types";

export default class WonAtomContentDetails extends React.Component {
  componentDidMount() {
    this.atomUri = this.props.atomUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.atomUri = nextProps.atomUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }
  selectFromState(state) {
    const atom = this.atomUri && state.getIn(["atoms", this.atomUri]);
    const details = this.props.branch && get(atom, this.props.branch);

    return {
      details,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    const allDetailsImm = usecaseUtils.getAllDetailsImm();
    const contentDetailsMap =
      this.state.details &&
      this.state.details.map((contentDetail, contentDetailKey) => {
        const detailDefinition = get(allDetailsImm, contentDetailKey);
        if (
          detailDefinition &&
          !this.shouldOmitDetail(contentDetail, contentDetailKey)
        ) {
          const ReactViewerComponent = get(
            detailDefinition,
            "reactViewerComponent"
          );

          if (ReactViewerComponent) {
            return (
              <div key={contentDetailKey} className="pis__component">
                <ReactViewerComponent
                  detail={detailDefinition.toJS()}
                  content={contentDetail}
                  ngRedux={this.props.ngRedux}
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
  ngRedux: PropTypes.object.isRequired,
};
