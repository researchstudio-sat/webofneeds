module PublishButton exposing (main)

import Actions
import Application
import Browser.Dom as Dom exposing (Element)
import Browser.Events
import Color
import Dict exposing (Dict)
import Html exposing (Html)
import Html.Attributes as HA
import Html.Events as Events
import Icons
import Json.Decode as Decode exposing (Decoder)
import Json.Encode as Encode
import Palette
import Persona exposing (Persona)
import Random
import Task
import Uuid exposing (Uuid)


main =
    Application.element
        { init = init
        , subscriptions = subscriptions
        , update = update
        , view = view
        , propDecoder = propDecoder
        }



---- PROPS ----


type alias Props =
    { buttonEnabled : Bool
    , showPersonas : Bool
    , personas : Dict String Persona
    , label : String
    }


defaultLabel =
    "Publish"


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
    Decode.map4 Props
        (Decode.field "buttonEnabled" Decode.bool)
        (Decode.field "showPersonas" Decode.bool)
        (Decode.field "personas" dictDecoder)
        (Decode.field "label" Decode.string
            |> Decode.maybe
            |> Decode.map (Maybe.withDefault defaultLabel)
        )



---- MODEL ----


type alias Model =
    { state : State
    , selectedPersona : SelectedPersona
    , buttonPosition : ButtonPosition
    }


type ButtonPosition
    = NotQueried
    | HasId Uuid
    | HasDimensions
        { id : Uuid
        , element : Element
        }


getUuid : ButtonPosition -> Maybe Uuid
getUuid buttonPosition =
    case buttonPosition of
        NotQueried ->
            Nothing

        HasId id ->
            Just id

        HasDimensions { id } ->
            Just id


type PopupDirection
    = Up
    | Down


type State
    = Closed
    | Open


type SelectedPersona
    = Anonymous
    | Persona String


init : Props -> ( Model, Cmd Msg )
init _ =
    ( { state = Closed
      , selectedPersona = Anonymous
      , buttonPosition = NotQueried
      }
    , getRandomId
    )



---- VIEW ----


selectedPersona : Model -> Props -> Maybe Persona
selectedPersona model props =
    (case model.selectedPersona of
        Persona url ->
            Just url

        Anonymous ->
            Nothing
    )
        |> Maybe.andThen
            (\url ->
                Dict.get url props.personas
            )


popupDirection : ButtonPosition -> PopupDirection
popupDirection buttonPosition =
    case buttonPosition of
        NotQueried ->
            Up

        HasId _ ->
            Up

        HasDimensions { element } ->
            let
                distanceTop =
                    element.element.y - element.viewport.y

                distanceBottom =
                    (element.viewport.y + element.viewport.height) - (element.element.y + element.element.height)
            in
            if distanceTop < distanceBottom then
                Down

            else
                Up


view :
    { model : Model
    , props : Props
    }
    -> Html Msg
view { model, props } =
    let
        fullLabel =
            selectedPersona model props
                |> Maybe.map (\persona -> props.label ++ " as " ++ persona.name)
                |> Maybe.withDefault (props.label ++ " Anonymously")
    in
    if not props.showPersonas then
        Palette.wonButton
            [ HA.disabled <| not props.buttonEnabled
            , HA.class "won-publish-button"
            , Events.onClick Publish
            , HA.id
                (getUuid model.buttonPosition
                    |> Maybe.map Uuid.toString
                    |> Maybe.withDefault ""
                )
            ]
            [ Html.text props.label
            ]

    else
        Html.div
            [ HA.class "won-button-row"
            , HA.class "won-publish-button"
            , HA.classList
                [ ( "up", popupDirection model.buttonPosition == Up )
                , ( "down", popupDirection model.buttonPosition == Down )
                ]
            , HA.id
                (getUuid model.buttonPosition
                    |> Maybe.map Uuid.toString
                    |> Maybe.withDefault ""
                )
            ]
            ([ Palette.wonButton
                [ HA.class "left"
                , HA.class "submit-button"
                , HA.disabled <| not props.buttonEnabled
                , Events.onClick Publish
                ]
                [ Html.text fullLabel
                ]
             , Palette.wonButton
                [ HA.class "right"
                , HA.class "won-dropdown-button"
                , Events.onClick ToggleDropdown
                ]
                [ Icons.icon
                    (case ( popupDirection model.buttonPosition, model.state ) of
                        ( Up, Open ) ->
                            Icons.arrowDown

                        ( Up, Closed ) ->
                            Icons.arrowUp

                        ( Down, Open ) ->
                            Icons.arrowUp

                        ( Down, Closed ) ->
                            Icons.arrowDown
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
                |> List.sortBy (.name >> String.toLower)
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
    | GotRandomId Uuid
    | GotDimensions Element
    | FailedToGetDimensions Dom.Error
    | ToggleDropdown
    | GotAnimationFrame
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

        GotRandomId uuid ->
            case model.buttonPosition of
                NotQueried ->
                    ( { model
                        | buttonPosition = HasId uuid
                      }
                    , Cmd.none
                    )

                _ ->
                    ( model, Cmd.none )

        GotDimensions element ->
            case model.buttonPosition of
                HasId uuid ->
                    ( { model
                        | buttonPosition =
                            HasDimensions
                                { id = uuid
                                , element = element
                                }
                      }
                    , Cmd.none
                    )

                HasDimensions { id } ->
                    ( { model
                        | buttonPosition =
                            HasDimensions
                                { id = id
                                , element = element
                                }
                      }
                    , Cmd.none
                    )

                NotQueried ->
                    ( model, Cmd.none )

        FailedToGetDimensions error ->
            ( model
            , Application.logError
                (case error of
                    Dom.NotFound id ->
                        "Could not find element id " ++ id
                )
            )

        GotAnimationFrame ->
            ( model
            , case model.buttonPosition of
                HasId uuid ->
                    getDimensions uuid

                HasDimensions { id } ->
                    getDimensions id

                NotQueried ->
                    Cmd.none
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


getRandomId : Cmd Msg
getRandomId =
    Random.generate GotRandomId Uuid.uuidGenerator


getDimensions : Uuid -> Cmd Msg
getDimensions uuid =
    Dom.getElement (Uuid.toString uuid)
        |> Task.attempt
            (\result ->
                case result of
                    Ok dimensions ->
                        GotDimensions dimensions

                    Err error ->
                        FailedToGetDimensions error
            )


subscriptions : Model -> Sub Msg
subscriptions model =
    let
        sub =
            Browser.Events.onAnimationFrame (always GotAnimationFrame)
    in
    case model.buttonPosition of
        HasId _ ->
            sub

        HasDimensions _ ->
            sub

        NotQueried ->
            Sub.none
