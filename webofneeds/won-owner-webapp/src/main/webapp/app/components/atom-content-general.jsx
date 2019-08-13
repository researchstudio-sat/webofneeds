/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import { actionCreators } from "../actions/actions.js";
import PropTypes from "prop-types";
import {
  getConnectionUriFromRoute,
  getOwnedAtomByConnectionUri,
  selectLastUpdateTime,
} from "../redux/selectors/general-selectors";
import { getOwnedConnectionByUri } from "../redux/selectors/connection-selectors.js";
import { get, getIn } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import won from "../won-es6";
import { relativeTime } from "../won-label-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";

import "~/style/_atom-content-general.scss";

export default class WonAtomContentGeneral extends React.Component {
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
    const connectionUri = getConnectionUriFromRoute(state);
    const connection = getOwnedConnectionByUri(state, connectionUri);

    const ownAtom = getOwnedAtomByConnectionUri(state, connectionUri);

    const ratingConnectionUri =
      get(connection, "targetAtomUri") == this.atomUri &&
      atomUtils.getHeldByUri(ownAtom)
        ? connectionUri
        : null;

    const atom = this.atomUri && getIn(state, ["atoms", this.atomUri]);
    const viewState = get(state, "view");

    const creationDate = get(atom, "creationDate");
    const modifiedDate = get(atom, "modifiedDate");

    return {
      WON: won.WON,
      typeLabel: atom && atomUtils.generateTypeLabel(atom),
      fullFlagLabels: atom && atomUtils.generateFullFlagLabels(atom),
      shortFlagLabels: atom && atomUtils.generateShortFlagLabels(atom),
      fullSocketLabels: atom && atomUtils.generateFullSocketLabels(atom),
      shortSocketLabels: atom && atomUtils.generateShortSocketLabels(atom),
      friendlyCreationDate:
        creationDate && relativeTime(selectLastUpdateTime(state), creationDate),
      friendlyModifiedDate:
        modifiedDate &&
        modifiedDate != creationDate &&
        relativeTime(selectLastUpdateTime(state), modifiedDate),
      ratingConnectionUri: ratingConnectionUri,
      shouldShowRdf: viewUtils.showRdf(viewState),
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div />;
    }

    let flags;
    let sockets;

    if (this.state.shouldShowRdf) {
      if (this.state.fullFlagLabels && this.state.fullFlagLabels.length > 0) {
        const flagArray = this.state.fullFlagLabels.map((flag, index) => {
          return (
            <span key={flag + "-" + index} className="acg__item__value__flag">
              {flag}
            </span>
          );
        });

        flags = (
          <div className="acg__item">
            <div className="acg__item__label">Flags</div>
            <div className="acg__item__value">{flagArray}</div>
          </div>
        );
      }

      if (
        this.state.fullSocketLabels &&
        this.state.fullSocketLabels.length > 0
      ) {
        const socketArray = this.state.fullSocketLabels.map((socket, index) => {
          return (
            <span
              key={socket + "-" + index}
              className="acg__item__value__socket"
            >
              {socket}
            </span>
          );
        });
        sockets = (
          <div className="acg__item">
            <div className="acg__item__label">Sockets</div>
            <div className="acg__item__value">{socketArray}</div>
          </div>
        );
      }
    } else {
      if (this.state.shortFlagLabels && this.state.shortFlagLabels.length > 0) {
        const flagArray = this.state.shortFlagLabels.map((flag, index) => {
          return (
            <span key={flag + "-" + index} className="acg__item__value__flag">
              {flag}
            </span>
          );
        });

        flags = (
          <div className="acg__item">
            <div className="acg__item__label">Flags</div>
            <div className="acg__item__value">{flagArray}</div>
          </div>
        );
      }
      if (
        this.state.shortSocketLabels &&
        this.state.shortSocketLabels.length > 0
      ) {
        const socketArray = this.state.shortSocketLabels.map(
          (socket, index) => {
            return (
              <span
                key={socket + "-" + index}
                className="acg__item__value__socket"
              >
                {socket}
              </span>
            );
          }
        );
        sockets = (
          <div className="acg__item">
            <div className="acg__item__label">Sockets</div>
            <div className="acg__item__value">{socketArray}</div>
          </div>
        );
      }
    }

    return (
      <won-atom-content-general>
        {this.state.friendlyCreationDate && (
          <div className="acg__item">
            <div className="acg__item__label">Created</div>
            <div className="acg__item__value">
              {this.state.friendlyCreationDate}
            </div>
          </div>
        )}
        <div className="acg__item">
          <div className="acg__item__label">Type</div>
          <div className="acg__item__value">{this.state.typeLabel}</div>
        </div>
        {this.state.friendlyModifiedDate && (
          <div className="acg__item">
            <div className="acg__item__label">Modified</div>
            <div className="acg__item__value">
              {this.state.friendlyModifiedDate}
            </div>
          </div>
        )}
        {flags}
        {sockets}
      </won-atom-content-general>
    );
  }
}
WonAtomContentGeneral.propTypes = {
  atomUri: PropTypes.string.isRequired,
  ngRedux: PropTypes.object.isRequired,
};
