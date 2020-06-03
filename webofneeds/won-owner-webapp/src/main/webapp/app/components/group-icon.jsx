/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the targetAtom are shown
 *    atom-uri: then the participants of the atom behind the atom uri are shown
 * Created by quasarchimaere on 15.01.2019.
 */
import React from "react";
import { get } from "../utils.js";
import { useSelector } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import * as atomUtils from "../redux/utils/atom-utils.js";
import WonAtomIcon from "./atom-icon.jsx";

import "~/style/_group-icon.scss";
import PropTypes from "prop-types";

export default function WonGroupIcon({ connection }) {
  const allAtoms = useSelector(generalSelectors.getAtoms);

  const targetAtom = get(allAtoms, get(connection, "targetAtomUri"));
  const groupMembers = atomUtils.getGroupMemberUris(targetAtom);

  const groupMembersArray = groupMembers ? groupMembers.toArray() : [];
  const groupMembersSize = groupMembersArray.length;

  const groupMemberElements = groupMembersArray.map((groupMemberUri, index) => {
    if (groupMembersSize <= 4 || index < 3) {
      return (
        <div
          key={groupMemberUri}
          className={
            "gi__icons__icon " +
            (groupMembersSize == 1 ? " gi__icons__icon--spanCol " : "")
          }
        >
          <WonAtomIcon atom={get(allAtoms, groupMemberUri)} />
        </div>
      );
    }
  });

  let groupMembersSizeElement;

  if (groupMembersSize <= 3) {
    groupMembersSizeElement = (
      <div
        className={
          "gi__icons__more " +
          (groupMembersSize == 2 ||
          groupMembersSize == 0 ||
          groupMembersArray == 1
            ? " gi__icons__more--spanCol "
            : "") +
          (groupMembersSize == 0 ? " gi__icons__more--spanRow " : "")
        }
      >
        {groupMembersSize}
      </div>
    );
  } else if (groupMembersSize > 4) {
    groupMembersSizeElement = <div className="gi__icons__more">+</div>;
  }

  return (
    <won-group-icon>
      {groupMemberElements}
      {groupMembersSizeElement}
    </won-group-icon>
  );
}
WonGroupIcon.propTypes = {
  connection: PropTypes.object,
};
