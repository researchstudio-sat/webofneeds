/**
 * Created by ms on 15.02.2021
 */
import React from "react";
import PropTypes from "prop-types";
import * as atomUtils from "../../redux/utils/atom-utils.js";

export default function WonAtomContentAuth({ atom }) {
  const authImm = atomUtils.getAuth(atom);
  const authorizations = authImm && authImm.toJS();
  window.auth4dg = authorizations;

  return (
    <won-atom-content-auth>
      {authorizations && authorizations.length > 0 ? (
        authorizations.map((auth, index) => {
          return (
            <div key={"key_" + index} className="acg__item">
              <div className="acg__item__label">{"Auth_" + index++}</div>
              <div className="acg__item__value">{JSON.stringify(auth)}</div>
            </div>
          );
        })
      ) : (
        <div>No Authorization Data available</div>
      )}
    </won-atom-content-auth>
  );
}
WonAtomContentAuth.propTypes = {
  atom: PropTypes.object.isRequired,
};
