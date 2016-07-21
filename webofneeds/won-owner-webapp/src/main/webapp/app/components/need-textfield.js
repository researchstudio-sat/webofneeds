/**
 * Created by ksinger on 17.06.2016.
 */

;

import Medium from '../mediumjs-es6';
import angular from 'angular';
import 'ng-redux';
import Immutable from 'immutable';
import 'angular-sanitize';
import { dispatchEvent, attach, delay } from '../utils';
import { actionCreators }  from '../actions/actions';

window.Medium4dbg = Medium;

function genComponentConf() {
    let template = `
        <div class="wdt__left">
            <div class="wdt__text"
                    ng-class="{ 'valid' : self.valid(), 'invalid' : !self.valid() }">
                <div class="medium-mount"></div>
            </div>
            <span class="wdt__charcount">
                {{ self.charactersLeft() }} characters left
            </span>
        </div>
    `;

    const serviceDependencies = ['$scope', '$element', '$ngRedux', /*injections as strings here*/];

    class Controller {
        constructor(/* arguments <- serviceDependencies */) {
            attach(this, serviceDependencies, arguments);
            window.ntf4dbg = this;

            this.characterLimit = 140; //TODO move to conf

            const selectFromState = (state) => ({
                draftId: state.getIn(['router', 'currentParams', 'draftId'])
            })
            const disconnect = this.$ngRedux.connect(selectFromState, actionCreators)(this);
            this.$scope.$on('$destroy', disconnect);

            this.initMedium();

            this.mediumMountNg().bind('input', e => {
                var description;
                var title;
                var tags;

                angular.element(".medium-mount p").removeClass("medium_title");

                if(angular.element(".medium-mount p") && angular.element(".medium-mount p").length > 1){
                    angular.element(".medium-mount p:first").addClass("medium_title");
                    title  = angular.element(".medium-mount p.medium_title").text();

                    if(angular.element(".medium-mount p:not('.medium_title')") && angular.element(".medium-mount p:not('.medium_title')").length > 0){
                        description = "";
                        angular.element(".medium-mount p:not('.medium_title')").each(function(){
                            description += angular.element(this).text() +"\n";
                        });
                    }
                }else {
                    title  = angular.element(".medium-mount p:first").text();
                    description = undefined;
                }

                //ADD TAGS
                var titleTags = title? title.match(/#(\w+)/gi) : [];
                var descriptionTags = description? description.match(/#(\w+)/gi) : [];

                tags = angular.element.unique(
                    angular.element.merge(
                        titleTags ? titleTags : [],
                        descriptionTags ? descriptionTags : []
                    )
                );

                for(var i=0; i<tags.length; i++){
                    tags[i] = tags[i].substr(1);
                }

                //SAVE TO STATE
                this.drafts__change__description({
                    draftId: this.draftId,
                    description : description
                });

                this.drafts__change__title({
                    draftId: this.draftId,
                    title: title.replace("&nbsp;","") //TODO: MOVE THIS HACK TO VIEW LEVEL
                });

                this.drafts__change__tags({
                    draftId: this.draftId,
                    tags: tags && tags.length > 0? tags : undefined
                });
            });
        }
        charactersLeft() {
            return this.characterLimit - this.medium.value().length;
        }
        valid() {
            return this.charactersLeft() >= 0;
        }

        value() {
            return this.medium
                .value()
                /*
                 * the replace fixes odd behaviour of FF. it inserts
                 * a `<br>` at the end after the first space is
                 * typed -- unless the space is the first character
                 * in the field.
                 */
                .replace(/<br>$/, '');
        }

        initMedium() {
            // initialising editor. see http://jakiestfu.github.io/Medium.js/docs/
            this.medium = new Medium({
                element: this.mediumMount(),

                modifier: 'auto',
                placeholder: 'What',
                autoHR: false, //if true, inserts <hr> after two empty lines
                //mode: Medium.inlineMode, // no newlines, no styling
                mode: Medium.partialMode, // allows newlines, no styling
                //maxLength: this.maxChars, // -1 would disable it
                tags: {
                    /*
                     'break': 'br',
                     'horizontalRule': 'hr',
                     'paragraph': 'p',
                     'outerLevel': ['pre', 'blockquote', 'figure'],
                     'innerLevel': ['a', 'b', 'u', 'i', 'img', 'strong']
                     */
                    //'outerLevel': [], //should disable all tags
                    //'innerLevel': [], //should disable all tags
                },
                attributes: {
                    //remove: ['style', 'class'] //TODO does this remove the ng-class?
                    remove: ['style'] //TODO does this remove the ng-class?
                },
                /*
                pasteAsText: true,
                beforeInvokeElement: function () {
                 //this = Medium.Element
                     console.log('beforeInvokeElement: ', this)
                 },
                 beforeInsertHtml: function () {
                 //this = Medium.Html
                     console.log('beforeInsertHtml: ', this)
                 },
                 beforeAddTag: function (tag, shouldFocus, isEditable, afterElement) {
                     console.log('beforeAddTag: ', this, arguments)
                 },
                 keyContext: null, //what does this do?
                 pasteEventHandler: function(e) {
                    //default paste event handler
                    //enables paste (dunno why) but also breaks the inlineMode (suddenly there's <p> elements)
                    console.log('pasteEventHandler: ', this, arguments)
                 }
                */

            });

            //remove the inline-styles placed by medium.js
            this.medium.placeholder.style = "";
            this.mediumMount().addEventListener('blur', e =>
                delay(0) //push to end end of task-queue (and thus all other `blur`-listeners
                    .then(() => {
                        const style = this.medium.placeholder.style;
                        style.minHeight = 0;
                        style.minWidth = 0;
                    })
            );
        }

        mediumMountNg() {
            if(!this._mediumMount) {
                this._mediumMount = this.textFieldNg().find('.medium-mount')
            }
            return this._mediumMount;
        }

        mediumMount() {
            return this.mediumMountNg()[0];
        }

        textFieldNg() {
            if(!this._textField) {
                this._textField = this.$element.find('.wdt__text');
            }
            return this._textField;
        }
        textField() {
            return this.textFieldNg()[0];
        }
    }
    Controller.$inject = serviceDependencies;


    return {
        restrict: 'E',
        controller: Controller,
        controllerAs: 'self',
        bindToController: true, //scope-bindings -> ctrl
        scope: { },
        template: template
    }
}


export default angular.module('won.owner.components.needTextfield', [ ])
    .directive('needTextfield', genComponentConf)
    .name;