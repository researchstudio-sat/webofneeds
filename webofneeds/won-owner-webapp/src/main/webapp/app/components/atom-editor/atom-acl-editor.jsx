import React, { useState, useEffect } from "react";
import PropTypes from "prop-types";
import Immutable from "immutable";

import "~/style/_atom-acl-editor.scss";
import {
  connectedConnectionsAuthorization,
  connectToSocketsAuthorization,
  defaultPublicAtomAuthorization,
} from "~/config/detail-definitions";

export default function WonAtomAclEditor({ initialDraftImm, onUpdateImm }) {
  const [draftAclImm, setDraftAclImm] = useState(initialDraftImm);

  const publicACL = Immutable.fromJS([
    defaultPublicAtomAuthorization,
    connectedConnectionsAuthorization,
    connectToSocketsAuthorization,
  ]);
  const privateACL = Immutable.fromJS([connectedConnectionsAuthorization]);

  useEffect(
    () => {
      onUpdateImm(draftAclImm);
    },
    [draftAclImm]
  );

  const updateAclImm = () => {
    if (draftAclImm.size === 3) {
      setDraftAclImm(privateACL);
    } else {
      setDraftAclImm(publicACL);
    }
  };

  console.debug("draftAclImm:", draftAclImm);

  return (
    <React.Fragment>
      <div className="cp__content__branchheader">Access Control</div>
      <won-atom-acl-editor>
        <div className="aae__item">
          <input
            id="publicAtomCB"
            type="checkbox"
            onChange={updateAclImm}
            defaultChecked={draftAclImm.size === 3}
          />
          <label htmlFor="publicAtomCB">Public</label>
        </div>
        <span className="aae__infoText">
          If checked this Atom will be visible and open to connect with for
          anyone
        </span>
      </won-atom-acl-editor>
    </React.Fragment>
  );
}

WonAtomAclEditor.propTypes = {
  initialDraftImm: PropTypes.object,
  onUpdateImm: PropTypes.func.isRequired,
};
