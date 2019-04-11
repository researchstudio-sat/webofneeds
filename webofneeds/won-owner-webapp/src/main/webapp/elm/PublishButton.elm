port module PublishButton exposing (main)

import Actions
import Application
import Color
import Dict exposing (Dict)
import Html exposing (Html)
import Html.Attributes as HA
import Html.Events as Events
import Icons
import Json.Decode as Decode exposing (Decoder, Value)
import Json.Encode as Encode
import Palette
import Persona exposing (Persona)
import Time exposing (Posix)


main =
    Application.element
        { init = init
        , subscriptions = always Sub.none
        , update = update
        , view = view
        , propDecoder = propDecoder
        }



---- PROPS ----


type alias Props =
    { draftValid : Bool
    , showPersonas : Bool
    , personas : Dict String Persona
    }


propDecoder : Decoder Props
propDecoder =
    let
        dictDecoder =
            Decode.list Persona.decoder
                |> Decode.map
                    (List.map (\persona -> ( persona.uri, persona ))
                        >> Dict.fromList
                    )
    in
    Decode.map3 Props
        (Decode.field "draftValid" Decode.bool)
        (Decode.field "showPersonas" Decode.bool)
        (Decode.field "personas" <| dictDecoder)



---- MODEL ----


type alias Model =
    { state : State
    , selectedPersona : SelectedPersona
    }


type State
    = Closed
    | Open


type SelectedPersona
    = Anonymous
    | Persona String


init : Props -> ( Model, Cmd Msg )
init props =
    ( { state = Closed
      , selectedPersona = Anonymous
      }
    , Cmd.none
    )



---- VIEW ----


view :
    { model : Model
    , props : Props
    , style : Application.Style
    }
    -> Html Msg
view { model, props } =
    Html.div [ HA.class "won-publish-button" ]
        [ if not props.showPersonas then
            Palette.wonButton
                [ HA.disabled <| not props.draftValid
                , Events.onClick Publish
                ]
                [ Html.text "Publish Anonymously"
                ]

          else
            Html.div [ HA.class "won-button-row" ]
                ([ Palette.wonButton
                    [ HA.class "left"
                    , HA.disabled <| not props.draftValid
                    , Events.onClick Publish
                    ]
                    [ Html.text
                        ((case model.selectedPersona of
                            Persona url ->
                                Just url

                            Anonymous ->
                                Nothing
                         )
                            |> Maybe.andThen
                                (\url ->
                                    Dict.get url props.personas
                                )
                            |> Maybe.map
                                (\persona ->
                                    "Publish as " ++ persona.name
                                )
                            |> Maybe.withDefault "Publish Anonymously"
                        )
                    ]
                 , Palette.wonButton
                    [ HA.class "right"
                    , HA.class "won-dropdown-button"
                    , Events.onClick ToggleDropdown
                    ]
                    [ Icons.icon
                        (case model.state of
                            Open ->
                                Icons.arrowDown

                            Closed ->
                                Icons.arrowUp
                        )
                        Color.white
                    ]
                 ]
                    ++ (case model.state of
                            Open ->
                                [ personaList (Dict.values props.personas) ]

                            Closed ->
                                []
                       )
                )
        ]


personaList : List Persona -> Html Msg
personaList personas =
    let
        listElements =
            if List.isEmpty personas then
                [ Html.div
                    [ HA.class "won-persona-list-entry"
                    , HA.class "empty"
                    ]
                    [ Html.a
                        [ HA.href "#!/settings"
                        , HA.target "_blank"
                        , HA.class "won-persona-name"
                        ]
                        [ Html.text "Create Personas" ]
                    ]
                ]

            else
                personaEntries ++ [ anonymousEntry ]

        personaEntries =
            personas
                |> List.sortBy .name
                |> List.map personaEntry

        anonymousEntry =
            Html.div
                [ HA.class "anonymous"
                , HA.class "won-persona-list-entry"
                , Events.onClick <| SelectPersona Anonymous
                ]
                [ Html.div [ HA.class "won-persona-name" ]
                    [ Html.text "Anonymous" ]
                ]
    in
    Html.div [ HA.class "won-persona-list" ]
        listElements


personaEntry : Persona -> Html Msg
personaEntry persona =
    Persona.inlineView
        [ Events.onClick (SelectPersona <| Persona persona.uri)
        ]
        persona



---- UPDATE ----


type Msg
    = SelectPersona SelectedPersona
    | ToggleDropdown
    | Publish


update :
    Msg
    ->
        { model : Model
        , props : Props
        }
    -> ( Model, Cmd Msg )
update msg { model, props } =
    case msg of
        ToggleDropdown ->
            ( { model
                | state =
                    case model.state of
                        Open ->
                            Closed

                        Closed ->
                            Open
              }
            , Cmd.none
            )

        SelectPersona newSelection ->
            ( { model
                | selectedPersona =
                    case newSelection of
                        Persona url ->
                            if Dict.member url props.personas then
                                newSelection

                            else
                                Anonymous

                        Anonymous ->
                            Anonymous
                , state = Closed
              }
            , Cmd.none
            )

        Publish ->
            ( { model
                | state = Closed
              }
            , publish model.selectedPersona
            )


publish : SelectedPersona -> Cmd msg
publish personaChoice =
    Actions.emitEvent
        { eventName = "publish"
        , payload =
            case personaChoice of
                Persona personaId ->
                    Encode.string personaId

                Anonymous ->
                    Encode.null
        }
