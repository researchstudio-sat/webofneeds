/**
 * Created by ms on 15.02.2021
 */
import React from "react";
import PropTypes from "prop-types";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { get } from "~/app/utils";
import vocab from "~/app/service/vocab.js";

import "~/style/_atom-content-auth.scss";

export default function WonAtomContentAuth({ atom }) {
  const authImm = atomUtils.getAuth(atom);
  const authElements = [];

  authImm &&
    authImm.map((auth, idx) => {
      const grantee = get(auth, vocab.AUTH.granteeCompacted);
      const grant = get(auth, vocab.AUTH.grantCompacted);
      const bearer = get(auth, vocab.AUTH.bearerCompacted);
      const provideAuthInfo = get(auth, vocab.AUTH.provideAuthInfoCompacted);
      const requestedBy = get(auth, vocab.AUTH.requestedByCompacted);

      authElements.push(
        <div key={"key_" + idx} className="acauth__item">
          <div className="acauth__item__label">{"Auth_" + idx}</div>
          {grantee && (
            <React.Fragment>
              <div className="acauth__item__granteelabel">Grantee</div>
              <pre className="acauth__item__grantee">
                {grantee && JSON.stringify(grantee.toJS(), undefined, 2)}
              </pre>
            </React.Fragment>
          )}
          {bearer && (
            <React.Fragment>
              <div className="acauth__item__bearerlabel">Bearer</div>
              <pre className="acauth__item__bearer">
                {bearer && JSON.stringify(bearer.toJS(), undefined, 2)}
              </pre>
            </React.Fragment>
          )}
          {grant && (
            <React.Fragment>
              <div className="acauth__item__grantlabel">Grant</div>
              <pre className="acauth__item__grant">
                {grant && JSON.stringify(grant.toJS(), undefined, 2)}
              </pre>
            </React.Fragment>
          )}
          {provideAuthInfo && (
            <React.Fragment>
              <div className="acauth__item__provideAuthInfolabel">
                ProvideAuthInfo
              </div>
              <pre className="acauth__item__provideAuthInfo">
                {provideAuthInfo &&
                  JSON.stringify(provideAuthInfo.toJS(), undefined, 2)}
              </pre>
            </React.Fragment>
          )}
          {requestedBy && (
            <React.Fragment>
              <div className="acauth__item__requestedBylabel">RequestedBy</div>
              <pre className="acauth__item__requestedBy">
                {requestedBy &&
                  JSON.stringify(requestedBy.toJS(), undefined, 2)}
              </pre>
            </React.Fragment>
          )}
        </div>
      );
    });

  return (
    <won-atom-content-auth>
      {authElements.length > 0 ? (
        authElements
      ) : (
        <div className="acauth__nodata">No Authorization Data available</div>
      )}
    </won-atom-content-auth>
  );
}
WonAtomContentAuth.propTypes = {
  atom: PropTypes.object.isRequired,
};
