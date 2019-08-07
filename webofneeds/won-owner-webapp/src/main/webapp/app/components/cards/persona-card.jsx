/**
 * Created by quasarchimaere on 30.07.2019.
 */
import React from "react";
import {get, getIn} from "../../utils.js";
import {actionCreators} from "../../actions/actions.js";
import * as atomUtils from "../../redux/utils/atom-utils.js";

import "~/style/_persona-card.scss";
import Immutable from "immutable";

export default class WonPersonaCard extends React.Component {
  componentDidMount() {
    this.atomUri = this.props.atomUri;
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
    this.setState(this.selectFromState(this.props.ngRedux.getState()));
  }

  selectFromState(state) {
    const atom = getIn(state, ["atoms", this.atomUri]);
    const identiconSvg = atomUtils.getIdenticonSvg(atom);
    const atomImage = atomUtils.getDefaultPersonaImage(atom);

    return {
      isInactive: atomUtils.isInactive(atom),
      atom,
      personaName: get(atom, "humanReadable"),
      atomImage,
      showDefaultIcon: !atomImage,
      identiconSvg,
    };
  }

  render() {
    if (!this.state) {
      console.debug("render with null state");
      return <div/>;
    }

    const personaIdenticon = this.state.showDefaultIcon && this.state.identiconSvg
      ? <img className="identicon" alt="Auto-generated title image" src={"data:image/svg+xml;base64,"+this.state.identiconSvg}/>
      : undefined;

    const personaImage = this.state.atomImage
      ? <img className="image" alt={this.state.atomImage.get('name')} src={"data:"+this.state.atomImage.get('type')+";base64,"+this.state.atomImage.get('data')}/>
      : undefined;

    return (
      <won-persona-card onClick={() => this.atomClick()}>
        <div className={"card__icon clickable " + (this.state.isInactive ? "inactive" : "")}>
          {personaIdenticon}
          {personaImage}
        </div>
        <div className="card__main clickable">
          <div className="card__main__name">{this.state.personaName}</div>
        </div>
      </won-persona-card>
    );
  }

  atomClick() {
    if (this.props.onAtomClick) {
      this.props.onAtomClick();
    } else {
      this.props.ngRedux.dispatch(actionCreators.atoms__selectTab(
        Immutable.fromJS({ atomUri: this.atomUri, selectTab: "DETAIL" })
      ));
      this.props.ngRedux.dispatch(actionCreators.router__stateGo("post", { postUri: this.atomUri }));
    }
  }
}