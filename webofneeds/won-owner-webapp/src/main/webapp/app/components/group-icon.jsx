/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the targetAtom are shown
 *    atom-uri: then the participants of the atom behind the atom uri are shown
 * Created by quasarchimaere on 15.01.2019.
 */
import React from "react";
import { get, getIn } from "../utils.js";
import { connect } from "react-redux";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import WonAtomIcon from "./atom-icon.jsx";

import "~/style/_group-icon.scss";
import PropTypes from "prop-types";

const mapStateToProps = (state, ownProps) => {
  let groupMembers;

  if (ownProps.connectionUri) {
    const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(
      state,
      ownProps.connectionUri
    );
    const connection = getIn(ownedAtom, [
      "connections",
      ownProps.connectionUri,
    ]);
    const targetAtom = get(
      generalSelectors.getAtoms(state),
      get(connection, "targetAtomUri")
    );
    groupMembers = get(targetAtom, "groupMembers");
  } else if (ownProps.atomUri) {
    const atom = get(generalSelectors.getAtoms(state), ownProps.atomUri);

    groupMembers = get(atom, "groupMembers");
  }

  return {
    atomUri: ownProps.atomUri,
    connectionUri: ownProps.connectionUri,
    groupMembersArray: groupMembers ? groupMembers.toArray() : [],
    groupMembersSize: groupMembers ? groupMembers.size : 0,
  };
};

class WonGroupIcon extends React.Component {
  render() {
    const groupMemberElements = this.props.groupMembersArray.map(
      (groupMemberUri, index) => {
        if (this.props.groupMembersSize <= 4 || index < 3) {
          return (
            <div
              key={groupMemberUri}
              className={
                "gi__icons__icon " +
                (this.props.groupMembersSize == 1
                  ? " gi__icons__icon--spanCol "
                  : "")
              }
            >
              <WonAtomIcon atomUri={groupMemberUri} />
            </div>
          );
        }
      }
    );

    let groupMembersSize;

    if (this.props.groupMembersSize <= 3) {
      groupMembersSize = (
        <div
          className={
            "gi__icons__more " +
            (this.props.groupMembersSize == 2 ||
            this.props.groupMembersSize == 0 ||
            this.props.groupMembersArray == 1
              ? " gi__icons__more--spanCol "
              : "") +
            (this.props.groupMembersSize == 0
              ? " gi__icons__more--spanRow "
              : "")
          }
        >
          {this.props.groupMembersSize}
        </div>
      );
    } else if (this.props.groupMembersSize > 4) {
      groupMembersSize = <div className="gi__icons__more">+</div>;
    }

    return (
      <won-group-icon>
        {groupMemberElements}
        {groupMembersSize}
      </won-group-icon>
    );
  }
}
WonGroupIcon.propTypes = {
  connectionUri: PropTypes.string,
  atomUri: PropTypes.string,
  groupMembersArray: PropTypes.arrayOf(PropTypes.string),
  groupMembersSize: PropTypes.number,
};

export default connect(mapStateToProps)(WonGroupIcon);
