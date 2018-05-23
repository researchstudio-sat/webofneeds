import { h, render } from "preact";
import * as angular from "angular";

export interface Props {
  value: string;
  changeHandler: (string) => any;
}

export default function DescriptionPicker({ value, changeHandler }) {
  const updateText = e => {
    changeHandler({ description: e.target.value });
  };

  return (
    <div class="cis__description">
      <textarea
        class="cis__description__text won-txt"
        onInput={updateText}
        value={value}
      />
    </div>
  );
}

// Needed to bind to angular
const directiveName = angular
  .module("won.owner.components.descriptionPicker", [])
  .directive("wonDescriptionPicker", () => {
    return {
      scope: {
        initialDescription: "=",
        onDescriptionUpdated: "&",
      },
      link: function(scope, el) {
        scope.$watch("initialDescription", newValue => {
          render(
            <DescriptionPicker
              value={newValue}
              changeHandler={scope["onDescriptionUpdated"]}
            />,
            el[0]
          );
        });
      },
    };
  }).name;

export { directiveName as name };
