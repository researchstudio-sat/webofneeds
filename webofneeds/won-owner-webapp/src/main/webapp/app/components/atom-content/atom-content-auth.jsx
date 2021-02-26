/**
 * Created by ms on 15.02.2021
 */
import React from "react";
import PropTypes from "prop-types";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import vocab from "~/app/service/vocab.js";

import "~/style/_atom-content-auth.scss";

export default function WonAtomContentAuth({ atom }) {
  const authImm = atomUtils.getAuth(atom);
  const authorizations = authImm && authImm.toJS();
  window.auth4dg = authorizations;

  //TODO: Also display vocab.AUTH.bearer vocab.AUTH.provideAuthInfo and vocab.AUTH.requestedBy

  return (
    <won-atom-content-auth>
      {authorizations && authorizations.length > 0 ? (
        authorizations.map((auth, index) => {
          return (
            <div key={"key_" + index} className="acauth__item">
              <div className="acauth__item__label">{"Auth_" + index++}</div>
              <div className="acauth__item__granteelabel">Grantee</div>
              <pre className="acauth__item__grantee">
                {JSON.stringify(
                  auth[vocab.AUTH.granteeCompacted],
                  undefined,
                  2
                )}
              </pre>
              <div className="acauth__item__grantlabel">Grant</div>
              <pre className="acauth__item__grant">
                {JSON.stringify(auth[vocab.AUTH.grantCompacted], undefined, 2)}
              </pre>
            </div>
          );
        })
      ) : (
        <div className="acauth__nodata">No Authorization Data available</div>
      )}
    </won-atom-content-auth>
  );
}
WonAtomContentAuth.propTypes = {
  atom: PropTypes.object.isRequired,
};
