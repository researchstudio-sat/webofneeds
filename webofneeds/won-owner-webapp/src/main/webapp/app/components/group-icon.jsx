/**
 * Component for rendering the icon of a groupChat (renders participants icons)
 * Can be included with either:
 *    connection-uri: then the participants of the targetAtom are shown
 *    atom-uri: then the participants of the atom behind the atom uri are shown
 * Created by quasarchimaere on 15.01.2019.
 */
import React from "react";
import {get, getIn} from "../utils.js";
import {actionCreators} from "../actions/actions.js";
import * as generalSelectors from "../redux/selectors/general-selectors.js";
import WonAtomIcon from "./atom-icon.jsx";

import "~/style/_group-icon.scss";

export default class WonGroupIcon extends React.Component {
  componentDidMount() {
    this.atomUri = this.props.atomUri;
    this.connectionUri = this.props.connectionUri;
    this.disconnect = this.props.ngRedux.connect(
      this.selectFromState.bind(this),
      actionCreators
    )(state => {
      this.setState(state);
    });
  }

  componentWillUnmount() {
    this.disconnect();
  }

  UNSAFE_componentWillReceiveProps(nextProps) {
    this.atomUri = nextProps.atomUri;
    this.connectionUri = nextProps.connectionUri;
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    let groupMembers;

    if (this.connectionUri) {
      const ownedAtom = generalSelectors.getOwnedAtomByConnectionUri(state, this.connectionUri);
      const connection = getIn(ownedAtom, ["connections", this.connectionUri]);
      const targetAtom = get(generalSelectors.getAtoms(state), get(connection, "targetAtomUri"));
      groupMembers = get(targetAtom, "groupMembers");
    } else if (this.atomUri) {
      const atom = get(generalSelectors.getAtoms(state), this.atomUri);

      groupMembers = get(atom, "groupMembers");
    }

    return {
      groupMembersArray: groupMembers ? groupMembers.toArray() : [],
      groupMembersSize: groupMembers ? groupMembers.size : 0,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    const groupMemberElements = this.state.groupMembersArray.map((groupMemberUri, index) => {
      if(this.state.groupMembersSize <= 4 || index < 3) {
        return (
          <div key={groupMemberUri} className={"gi__icons__icon " + (this.state.groupMembersSize == 1 ? " gi__icons__icon--spanCol " : "")}>
            <WonAtomIcon atomUri={groupMemberUri} ngRedux={this.props.ngRedux}/>
          </div>
        );
      }
    });

    let groupMembersSize;

    if(this.state.groupMembersSize <= 3) {
      groupMembersSize = (
          <div className={"gi__icons__more " + ((this.state.groupMembersSize == 2 || this.state.groupMembersSize == 0 || this.state.groupMembersArray == 1) ? " gi__icons__more--spanCol " : "") + ((this.state.groupMembersSize == 0)? " gi__icons__more--spanRow " : "")}>
            {this.state.groupMembersSize}
          </div>
        );
    } else if(this.state.groupMembersSize > 4) {
      groupMembersSize = (
        <div className="gi__icons__more">
          +
        </div>
      );
    }

    return (
      <won-group-icon>
        {groupMemberElements}
        {groupMembersSize}
      </won-group-icon>
    );
  }
}
