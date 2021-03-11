/**
 * Created by ms on 15.02.2021
 */
import React from "react";
import { useSelector } from "react-redux";
import PropTypes from "prop-types";
import { get, getUri } from "~/app/utils";
import * as processSelectors from "~/app/redux/selectors/process-selectors";
import * as generalSelectors from "~/app/redux/selectors/general-selectors";
import * as processUtils from "~/app/redux/utils/process-utils";
import * as atomUtils from "~/app/redux/utils/atom-utils";

import "~/style/_atom-content-requests.scss";

export default function WonAtomContentRequests({ atom }) {
  const atomUri = getUri(atom);
  const atomRequests = useSelector(processSelectors.getAtomRequests(atomUri));
  const connectionContainerRequests = useSelector(
    processSelectors.getConnectionContainerRequests(atomUri)
  );

  const tokenScopeUriElements = [];
  const tokenScopeUris = atomUtils.getTokenScopeUris(atom);

  const processState = useSelector(generalSelectors.getProcessState);

  const possibleRequestCredentials = useSelector(
    generalSelectors.getPossibleRequestCredentialsForAtom(atomUri)
  );

  const priorAtomRequests = useSelector(
    processSelectors.getAtomRequests(atomUri)
  );
  const priorConnectionContainerRequests = useSelector(
    processSelectors.getConnectionContainerRequests(atomUri)
  );

  const atomRequestElements = [];
  const connectionContainerRequestElements = [];
  const atomRequestCredentialsElements = [];
  const connectionContainerRequestCredentialsElements = [];

  const generateResponseCodeClasses = responseCode => {
    const cssClassNames = ["acrequests__item__code"];

    if ((responseCode >= 200 && responseCode < 300) || responseCode === 304) {
      cssClassNames.push("acrequests__item__code--success");
    } else {
      cssClassNames.push("acrequests__item__code--failure");
    }

    return cssClassNames.join(" ");
  };

  const generateCredentialsCodeClasses = (priorRequests, responseCode) => {
    const cssClassNames = ["acrequests__item"];

    if (
      processUtils.isUsedCredentialsSuccessfully(priorRequests, responseCode)
    ) {
      cssClassNames.push("acrequests__item--success");
    } else if (
      processUtils.isUsedCredentialsUnsuccessfully(priorRequests, responseCode)
    ) {
      cssClassNames.push("acrequests__item--failure");
    } else {
      cssClassNames.push("acrequests__item--unknown");
    }

    return cssClassNames.join(" ");
  };

  tokenScopeUris &&
    tokenScopeUris.map((tokenScopeUri, idx) => {
      const fetchTokenRequests = processUtils.getFetchTokenRequests(
        processState,
        atomUri,
        tokenScopeUri
      );
      const fetchTokenRequestElements = [];
      const fetchTokenRequestCredentialsElements = [];

      possibleRequestCredentials &&
        possibleRequestCredentials.map((credentials, idx) => {
          fetchTokenRequestCredentialsElements.push(
            <div
              key={"rc_" + idx}
              className={generateCredentialsCodeClasses(
                fetchTokenRequests,
                credentials
              )}
            >
              <div className="acrequests__item__code">🔑</div>
              <pre className="acrequests__item__content">
                {credentials &&
                  JSON.stringify(credentials.toJS(), undefined, 2)}
              </pre>
            </div>
          );
        });

      fetchTokenRequests &&
        fetchTokenRequests.map((request, idx) => {
          const responseCode = get(request, "code");
          fetchTokenRequestElements.push(
            <div key={"ftr_" + idx} className="acrequests__item">
              <div className={generateResponseCodeClasses(responseCode)}>
                {responseCode}
              </div>
              <pre className="acrequests__item__content">
                {request && JSON.stringify(request.toJS(), undefined, 2)}
              </pre>
            </div>
          );
        });

      tokenScopeUriElements.push(
        <div key={"tr_" + idx} className="acrequests__tokens__item">
          <div className="acrequests__tokens__item__uri">
            {"Scope: " + tokenScopeUri}
          </div>
          <div className="acrequests__tokens__item__requests">
            <div className="acrequests__tokens__item__requests__header">
              Prior Requests
            </div>
            {fetchTokenRequestElements.length > 0 ? (
              fetchTokenRequestElements
            ) : (
              <div className="acrequests__tokens__nodata">
                No Fetch Token Request Data available
              </div>
            )}
          </div>
          <div className="acrequests__tokens__item__credentials">
            <div className="acrequests__tokens__item__credentials__header">
              Possible Credentials to Use
            </div>
            {fetchTokenRequestCredentialsElements.length > 0 ? (
              fetchTokenRequestCredentialsElements
            ) : (
              <div className="acrequests__tokens__nodata">
                No Credentials found in state
              </div>
            )}
          </div>
        </div>
      );
    });

  atomRequests &&
    atomRequests.map((request, idx) => {
      const responseCode = get(request, "code");
      atomRequestElements.push(
        <div key={"ar_" + idx} className="acrequests__item">
          <div className={generateResponseCodeClasses(responseCode)}>
            {responseCode}
          </div>
          <pre className="acrequests__item__content">
            {request && JSON.stringify(request.toJS(), undefined, 2)}
          </pre>
        </div>
      );
    });

  connectionContainerRequests &&
    connectionContainerRequests.map((request, idx) => {
      const responseCode = get(request, "code");
      connectionContainerRequestElements.push(
        <div key={"cr_" + idx} className="acrequests__item">
          <div className={generateResponseCodeClasses(responseCode)}>
            {responseCode}
          </div>
          <pre className="acrequests__item__content">
            {request && JSON.stringify(request.toJS(), undefined, 2)}
          </pre>
        </div>
      );
    });

  possibleRequestCredentials &&
    possibleRequestCredentials.map((credentials, idx) => {
      atomRequestCredentialsElements.push(
        <div
          key={"rc_" + idx}
          className={generateCredentialsCodeClasses(
            priorAtomRequests,
            credentials
          )}
        >
          <div className="acrequests__item__code">🔑</div>
          <pre className="acrequests__item__content">
            {credentials && JSON.stringify(credentials.toJS(), undefined, 2)}
          </pre>
        </div>
      );

      connectionContainerRequestCredentialsElements.push(
        <div
          key={"rc_" + idx}
          className={generateCredentialsCodeClasses(
            priorConnectionContainerRequests,
            credentials
          )}
        >
          <div className="acrequests__item__code">🔑</div>
          <pre className="acrequests__item__content">
            {credentials && JSON.stringify(credentials.toJS(), undefined, 2)}
          </pre>
        </div>
      );
    });

  return (
    <won-atom-content-requests>
      <div className="acrequests__req">
        <div className="acrequests__atom">
          <div className="acrequests__atom__header">Atom Request Status</div>
          {atomRequestElements.length > 0 ? (
            atomRequestElements
          ) : (
            <div className="acrequests__atom__nodata">
              No Atom Request Data available
            </div>
          )}
          <div className="acrequests__atom__header">
            Possible Credentials to Use
          </div>
          {atomRequestCredentialsElements.length > 0 ? (
            atomRequestCredentialsElements
          ) : (
            <div className="acrequests__credentials__nodata">
              No Credentials found in state
            </div>
          )}
        </div>
        <div className="acrequests__cc">
          <div className="acrequests__cc__header">
            ConnectionContainer Request Status
          </div>
          {connectionContainerRequestElements.length > 0 ? (
            connectionContainerRequestElements
          ) : (
            <div className="acrequests__cc__nodata">
              No Atom Request Data available
            </div>
          )}
          <div className="acrequests__cc__header">
            Possible Credentials to Use
          </div>
          {connectionContainerRequestCredentialsElements.length > 0 ? (
            connectionContainerRequestCredentialsElements
          ) : (
            <div className="acrequests__credentials__nodata">
              No Credentials found in state
            </div>
          )}
        </div>
      </div>
      <div className="acrequests__tokens">
        <div className="acrequests__tokens__header">Fetchable Tokens</div>
        {tokenScopeUriElements.length > 0 ? (
          tokenScopeUriElements
        ) : (
          <div className="acrequests__tokens__nodata">No Fetchable Tokens</div>
        )}
      </div>
    </won-atom-content-requests>
  );
}
WonAtomContentRequests.propTypes = {
  atom: PropTypes.object.isRequired,
};
