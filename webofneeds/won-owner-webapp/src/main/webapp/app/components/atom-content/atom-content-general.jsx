/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import PropTypes from "prop-types";
import * as generalSelectors from "../../redux/selectors/general-selectors";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { relativeTime } from "../../won-label-utils.js";
import { useSelector } from "react-redux";

import "~/style/_atom-content-general.scss";

export default function WonAtomContentGeneral({ atom }) {
  const creationDate = atomUtils.getCreationDate(atom);
  const modifiedDate = atomUtils.getModifiedDate(atom);

  const globalLastUpdateTime = useSelector(
    generalSelectors.selectLastUpdateTime
  );
  const friendlyCreationDate =
    creationDate && relativeTime(globalLastUpdateTime, creationDate);
  const friendlyModifiedDate =
    modifiedDate &&
    modifiedDate !== creationDate &&
    relativeTime(globalLastUpdateTime, modifiedDate);

  return (
    <won-atom-content-general>
      {friendlyModifiedDate && (
        <div className="acg__item">
          <div className="acg__item__label">Modified</div>
          <div className="acg__item__value">{friendlyModifiedDate}</div>
        </div>
      )}
      {friendlyCreationDate && (
        <div className="acg__item">
          <div className="acg__item__label">Created</div>
          <div className="acg__item__value">{friendlyCreationDate}</div>
        </div>
      )}
    </won-atom-content-general>
  );
}
WonAtomContentGeneral.propTypes = {
  atom: PropTypes.object.isRequired,
};
