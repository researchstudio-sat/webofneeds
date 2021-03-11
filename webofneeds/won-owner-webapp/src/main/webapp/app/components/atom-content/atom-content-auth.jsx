/**
 * Created by ms on 15.02.2021
 */
import React from "react";
import Immutable from "immutable";
import PropTypes from "prop-types";
import * as atomUtils from "../../redux/utils/atom-utils.js";
import { get, getIn } from "~/app/utils";
import vocab from "~/app/service/vocab.js";

import "~/style/_atom-content-auth.scss";

export default function WonAtomContentAuth({ atom }) {
  const authImm = atomUtils.getAuth(atom);
  const authElements = [];

  const generateGrantElement = grant => {
    const grantElements = [];

    const generateInnerGrantElements = g => {
      const operations = get(g, vocab.AUTH.operation);

      if (operations) {
        const opElements = [];
        const remainingOperations = operations.filter(operation => {
          if (get(operation, vocab.AUTH.requestToken)) {
            const expirationTime = getIn(operation, [
              vocab.AUTH.requestToken,
              vocab.AUTH.expiresAfter,
            ]);
            const tokenScopeUri = getIn(operation, [
              vocab.AUTH.requestToken,
              vocab.AUTH.tokenScope,
              "@id",
            ]);

            if (tokenScopeUri) {
              opElements.push(
                <span
                  className="acauth__item__grant acauth__item__grant--op"
                  key={tokenScopeUri}
                  title={
                    "Expires After: " +
                    expirationTime +
                    "\n\n" +
                    JSON.stringify(operation.toJS(), undefined, 2)
                  }
                >
                  {"🔑 " + tokenScopeUri}
                </span>
              );
              return false;
            }
            return true;
          }
          return true;
        });
        grantElements.push(
          <React.Fragment key="multigrants">
            {opElements}
            {remainingOperations.size > 0 && (
              <pre className="acauth__item__grant">
                {g &&
                  JSON.stringify(
                    g.set(vocab.AUTH.operation, remainingOperations).toJS(),
                    undefined,
                    2
                  )}
              </pre>
            )}
          </React.Fragment>
        );
      } else {
        grantElements.push(
          <pre className="acauth__item__grant" key="singlegrants">
            {g && JSON.stringify(g.toJS(), undefined, 2)}
          </pre>
        );
      }
    };

    if (Immutable.List.isList(grant)) {
      grant.map(g => generateInnerGrantElements(g));
    } else {
      generateInnerGrantElements(grant);
    }

    return grantElements;
  };

  const generateGranteeElement = grantee => {
    if (get(grantee, "@id") === vocab.AUTH.anyone) {
      return (
        <span
          className="acauth__item__grantee"
          title={grantee && JSON.stringify(grantee.toJS(), undefined, 2)}
        >
          Anyone
        </span>
      );
    } else if (
      getIn(grantee, [
        vocab.AUTH.socket,
        vocab.AUTH.connection,
        vocab.AUTH.connectionState,
        "@id",
      ]) === vocab.WON.Connected
    ) {
      if (
        getIn(grantee, [vocab.AUTH.socket, vocab.AUTH.socketType, "@id"]) ===
        vocab.BUDDY.BuddySocket
      ) {
        return (
          <span
            className="acauth__item__grantee"
            title={grantee && JSON.stringify(grantee.toJS(), undefined, 2)}
          >
            Any Buddy
          </span>
        );
      } else if (
        getIn(grantee, [vocab.AUTH.socket, vocab.AUTH.socketType, "@id"]) ===
        vocab.WXSCHEMA.MemberSocket
      ) {
        return (
          <span
            className="acauth__item__grantee"
            title={grantee && JSON.stringify(grantee.toJS(), undefined, 2)}
          >
            Any Member
          </span>
        );
      } else if (getIn(grantee, [vocab.AUTH.socket, vocab.AUTH.socketType])) {
        return (
          <span
            className="acauth__item__grantee"
            title={grantee && JSON.stringify(grantee.toJS(), undefined, 2)}
          >
            Anyone who is Connected
          </span>
        );
      }
    }

    return (
      <pre className="acauth__item__grantee">
        {grantee && JSON.stringify(grantee.toJS(), undefined, 2)}
      </pre>
    );
  };

  authImm &&
    authImm.map((auth, idx) => {
      const grantee = get(auth, vocab.AUTH.grantee);
      const grant = get(auth, vocab.AUTH.grant);
      const bearer = get(auth, vocab.AUTH.bearer);
      const provideAuthInfo = get(auth, vocab.AUTH.provideAuthInfo);
      const requestedBy = get(auth, vocab.AUTH.requestedBy);

      authElements.push(
        <div key={"key_" + idx} className="acauth__item">
          <div className="acauth__item__label">{"Auth_" + idx}</div>
          {grantee && (
            <React.Fragment>
              <div className="acauth__item__granteelabel">Grantee</div>
              {generateGranteeElement(grantee)}
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
              {generateGrantElement(grant)}
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
