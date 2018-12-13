// code adapted from https://stackoverflow.com/questions/17731083/how-to-autogrow-text-area-with-css3
import angular from "angular";
import { clamp } from "../utils.js";

function genDirectiveConf() {
  return {
    restrict: "A",
    link: (scope, element, attributes) => {
      const minRows = parseInt(attributes.minRows) || 1;
      const maxRows = parseInt(attributes.maxRows) || 999;
      const area = element[0];

      /*
       * necessary styling
       */

      //Height math depends on content box sizing
      area.style.boxSizing = "content-box";
      // no reason to allow manual resizing, as it will automatically be resized on first input
      area.style.resize = "none";

      /*
       * calculate line-height
       */
      let lineHeight, offsets;
      function updateLineHeightAndOffsets() {
        const style = window.getComputedStyle(area, null);
        offsets =
          parseFloat(style.paddingTop) + parseFloat(style.paddingBottom);

        if (style.lineHeight == "normal") {
          lineHeight = parseFloat(style.fontSize) * 1.2;
        } else {
          lineHeight = parseFloat(style.lineHeight);
        }
        return { lineHeight, offsets };
      }
      updateLineHeightAndOffsets();

      /*
       * update height
       */

      function updateHeight() {
        window.requestAnimationFrame(() => {
          stopObservingStyleChange(); // we don't want the style listener to trigger due to the height-change
          if (area.value == "") {
            area.style.height = "0";
          }

          const height = area.scrollHeight - offsets;

          const lines = clamp(
            Math.round(height / lineHeight),
            minRows,
            maxRows
          );
          if (height < lineHeight * maxRows) {
            area.style.overflowY = "hidden";
          } else {
            area.style.overflowY = "scroll";
          }
          area.style.height = lines * lineHeight + "px";

          startObservingStyleChange(); // start listening for style changes again
        });
      }

      area.style.height = "0";
      updateHeight();

      /*
       * Listen for changes of input and style
       */

      element.on("input", updateHeight);
      // area.addEventListener('input', updateHeight);

      const observer = new MutationObserver(mutations =>
        mutations.forEach(() => {
          updateLineHeightAndOffsets();
          updateHeight();
        })
      );
      function startObservingStyleChange() {
        observer &&
          observer.observe(area, {
            attributes: true,
            attributeFilter: ["style", "class"],
          });
      }
      function stopObservingStyleChange() {
        observer && observer.disconnect();
      }
      startObservingStyleChange();
    },
  };
}

export default angular
  .module("won.owner.directives.textareaAutogrow", [])
  .directive("wonTextareaAutogrow", genDirectiveConf).name;
