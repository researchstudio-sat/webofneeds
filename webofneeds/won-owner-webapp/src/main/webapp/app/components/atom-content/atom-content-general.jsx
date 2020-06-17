/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import * as generalSelectors from "../../redux/selectors/general-selectors";
import { get } from "../../utils.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { relativeTime } from "../../won-label-utils.js";
import * as viewUtils from "../../redux/utils/view-utils.js";
import { useSelector } from "react-redux";

import "~/style/_atom-content-general.scss";

export default function WonAtomContentGeneral({ atom }) {
  const viewState = useSelector(generalSelectors.getViewState);

  const creationDate = get(atom, "creationDate");
  const modifiedDate = get(atom, "modifiedDate");

  const typeLabel = atom && atomUtils.generateTypeLabel(atom);
  const fullFlagLabels = atom && atomUtils.generateFullFlagLabels(atom);
  const shortFlagLabels = atom && atomUtils.generateShortFlagLabels(atom);
  const fullSocketLabels = atom && atomUtils.generateFullSocketLabels(atom);
  const shortSocketLabels = atom && atomUtils.generateShortSocketLabels(atom);
  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyCreationDate =
    creationDate && relativeTime(globalLastUpdateTime, creationDate);
  const friendlyModifiedDate =
    modifiedDate &&
    modifiedDate != creationDate &&
    relativeTime(globalLastUpdateTime, modifiedDate);
  const shouldShowRdf = viewUtils.showRdf(viewState);

  let flags;
  let sockets;

  if (shouldShowRdf) {
    if (fullFlagLabels && fullFlagLabels.length > 0) {
      const flagArray = fullFlagLabels.map((flag, index) => {
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

    if (fullSocketLabels && fullSocketLabels.length > 0) {
      const socketArray = fullSocketLabels.map((socket, index) => {
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
    if (shortFlagLabels && shortFlagLabels.length > 0) {
      const flagArray = shortFlagLabels.map((flag, index) => {
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
    if (shortSocketLabels && shortSocketLabels.length > 0) {
      const socketArray = shortSocketLabels.map((socket, index) => {
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
      {friendlyCreationDate && (
        <div className="acg__item">
          <div className="acg__item__label">Created</div>
          <div className="acg__item__value">{friendlyCreationDate}</div>
        </div>
      )}
      <div className="acg__item">
        <div className="acg__item__label">Type</div>
        <div className="acg__item__value">{typeLabel}</div>
      </div>
      {friendlyModifiedDate && (
        <div className="acg__item">
          <div className="acg__item__label">Modified</div>
          <div className="acg__item__value">{friendlyModifiedDate}</div>
        </div>
      )}
      {flags}
      {sockets}
    </won-atom-content-general>
  );
}
WonAtomContentGeneral.propTypes = {
  atom: PropTypes.object.isRequired,
};
