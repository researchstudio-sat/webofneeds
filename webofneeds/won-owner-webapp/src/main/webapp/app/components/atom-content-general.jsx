/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import {
  getConnectionUriFromRoute,
  getOwnedAtomByConnectionUri,
  selectLastUpdateTime,
} from "../redux/selectors/general-selectors";
import { getOwnedConnectionByUri } from "../redux/selectors/connection-selectors.js";
import { get, getIn } from "../utils.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import { relativeTime } from "../won-label-utils.js";
import * as viewUtils from "../redux/utils/view-utils.js";
import { connect } from "react-redux";

import "~/style/_atom-content-general.scss";

const mapStateToProps = (state, ownProps) => {
  const connectionUri = getConnectionUriFromRoute(state);
  const connection = getOwnedConnectionByUri(state, connectionUri);

  const ownAtom = getOwnedAtomByConnectionUri(state, connectionUri);

  const ratingConnectionUri =
    get(connection, "targetAtomUri") == ownProps.atomUri &&
    atomUtils.getHeldByUri(ownAtom)
      ? connectionUri
      : null;

  const atom = ownProps.atomUri && getIn(state, ["atoms", ownProps.atomUri]);
  const viewState = get(state, "view");

  const creationDate = get(atom, "creationDate");
  const modifiedDate = get(atom, "modifiedDate");

  return {
    atomUri: ownProps.atomUri,
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
};

const AtomContentGeneral = props => {
  let flags;
  let sockets;

  if (props.shouldShowRdf) {
    if (props.fullFlagLabels && props.fullFlagLabels.length > 0) {
      const flagArray = props.fullFlagLabels.map((flag, index) => {
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

    if (props.fullSocketLabels && props.fullSocketLabels.length > 0) {
      const socketArray = props.fullSocketLabels.map((socket, index) => {
        return (
          <span key={socket + "-" + index} className="acg__item__value__socket">
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
    if (props.shortFlagLabels && props.shortFlagLabels.length > 0) {
      const flagArray = props.shortFlagLabels.map((flag, index) => {
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
    if (props.shortSocketLabels && props.shortSocketLabels.length > 0) {
      const socketArray = props.shortSocketLabels.map((socket, index) => {
        return (
          <span key={socket + "-" + index} className="acg__item__value__socket">
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
  }

  return (
    <won-atom-content-general>
      {props.friendlyCreationDate && (
        <div className="acg__item">
          <div className="acg__item__label">Created</div>
          <div className="acg__item__value">{props.friendlyCreationDate}</div>
        </div>
      )}
      <div className="acg__item">
        <div className="acg__item__label">Type</div>
        <div className="acg__item__value">{props.typeLabel}</div>
      </div>
      {props.friendlyModifiedDate && (
        <div className="acg__item">
          <div className="acg__item__label">Modified</div>
          <div className="acg__item__value">{props.friendlyModifiedDate}</div>
        </div>
      )}
      {flags}
      {sockets}
    </won-atom-content-general>
  );
};

AtomContentGeneral.propTypes = {
  atomUri: PropTypes.string.isRequired,
  typeLabel: PropTypes.string,
  fullFlagLabels: PropTypes.arrayOf(PropTypes.string),
  shortFlagLabels: PropTypes.arrayOf(PropTypes.string),
  fullSocketLabels: PropTypes.arrayOf(PropTypes.string),
  shortSocketLabels: PropTypes.arrayOf(PropTypes.string),
  friendlyCreationDate: PropTypes.string,
  friendlyModifiedDate: PropTypes.string,
  ratingConnectionUri: PropTypes.string,
  shouldShowRdf: PropTypes.bool,
};

export default connect(mapStateToProps)(AtomContentGeneral);
