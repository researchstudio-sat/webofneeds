port module PublishButton exposing (main)

import Actions
import Application
import Browser
import Browser.Events
import Dict exposing (Dict)
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Font as Font
import Element.Input as Input
import Elements
import Html exposing (Html)
import Html.Attributes as HA
import Json.Decode as Decode exposing (Decoder, Value)
import Json.Encode as Encode
import NonEmpty
import Old.Skin as Skin exposing (Skin)
import Persona exposing (Persona)
import Time exposing (Posix)
import Url exposing (Url)


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
    , size : Size
    }


type alias Size =
    { width : Int
    , height : Int
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
      , size =
            { width = 0
            , height = 0
            }
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
    let
        skin =
            Skin.default

        buttonColor =
            if props.draftValid then
                skin.primaryColor

            else
                skin.lineGray

        focusStyle =
            focused
                [ Border.shadow
                    { offset = ( 0, 0 )
                    , size = 2
                    , blur = 0
                    , color = Skin.setAlpha 0.4 buttonColor
                    }
                ]
    in
    layout
        [ -- This is needed so .ui doesn't set the min-height to 100%
          height (minimum 0 shrink)
        , Font.size 16
        ]
    <|
        if not props.showPersonas then
            Input.button
                [ width fill
                , height (px 43)
                , focusStyle
                , Background.color buttonColor
                , Border.rounded 3
                ]
                { onPress = Just Publish
                , label =
                    el
                        [ centerY
                        , centerX
                        , Font.color Skin.white
                        ]
                    <|
                        text "Publish Anonymously"
                }

        else
            row
                [ width fill
                , spacing 2
                , height (px 43)
                , above <|
                    case model.state of
                        Open ->
                            personaList skin
                                model.size
                                (props.personas
                                    |> Dict.values
                                )

                        Closed ->
                            none
                ]
                [ el
                    [ width fill
                    , height fill
                    ]
                  <|
                    Input.button
                        [ Background.color buttonColor
                        , width fill
                        , height fill
                        , Border.roundEach
                            { topLeft = 3
                            , bottomLeft = 3
                            , topRight = 0
                            , bottomRight = 0
                            }
                        , focusStyle
                        , paddingEach
                            { left = 42
                            , right = 0
                            , top = 0
                            , bottom = 0
                            }
                        ]
                        { onPress =
                            if props.draftValid then
                                Just Publish

                            else
                                Nothing
                        , label =
                            el
                                [ centerY
                                , centerX
                                , Font.color Skin.white
                                ]
                            <|
                                text
                                    (case model.selectedPersona of
                                        Persona url ->
                                            case Dict.get url props.personas of
                                                Just persona ->
                                                    "Publish as "
                                                        ++ persona.name

                                                Nothing ->
                                                    ""

                                        Anonymous ->
                                            "Publish Anonymously"
                                    )
                        }
                , Input.button
                    [ Background.color skin.primaryColor
                    , Border.roundEach
                        { topLeft = 0
                        , bottomLeft = 0
                        , topRight = 3
                        , bottomRight = 3
                        }
                    , width (px 40)
                    , height fill
                    , focusStyle
                    ]
                    { onPress = Just ToggleDropdown
                    , label =
                        svgIcon
                            [ width (px 20)
                            , height (px 20)
                            , centerX
                            , centerY
                            ]
                            { color = Skin.white
                            , name =
                                case model.state of
                                    Open ->
                                        "ico16_arrow_down"

                                    Closed ->
                                        "ico16_arrow_up"
                            }
                    }
                ]


personaList :
    Skin
    -> Size
    -> List Persona
    -> Element Msg
personaList skin screenSize personas =
    let
        listElements =
            if List.isEmpty personas then
                [ newTabLink [ width fill, height (px 45) ]
                    { url = "#!/settings"
                    , label =
                        el [ centerX ] <|
                            text "Create Personas"
                    }
                ]

            else
                personaEntries ++ [ anonymousEntry ]

        personaEntries =
            personas
                |> List.sortBy (.created >> Time.posixToMillis)
                |> List.map (personaEntry skin)

        anonymousEntry =
            Input.button [ width fill ]
                { onPress = Just <| SelectPersona Anonymous
                , label =
                    row
                        [ width fill
                        , spacing 5
                        ]
                        [ el
                            [ width (px 45)
                            , height (px 45)
                            ]
                            none
                        , text "Anonymous"
                        ]
                }

        line =
            el
                [ width fill
                , Border.widthEach
                    { bottom = 1
                    , left = 0
                    , right = 0
                    , top = 0
                    }
                , Border.color skin.lightGray
                ]
                none

        maxHeight =
            screenSize.height - 70
    in
    column
        [ width fill
        , Border.color skin.lineGray
        , Border.width 1
        , moveUp 5
        , Background.color Skin.white
        , scrollbarY
        , height (maximum maxHeight shrink)
        ]
        (List.intersperse line listElements)


personaEntry :
    Skin
    -> Persona
    -> Element Msg
personaEntry skin persona =
    Input.button [ width fill ]
        { onPress = Just <| SelectPersona <| Persona <| persona.uri
        , label =
            row
                [ width fill
                , spacing 5
                ]
                [ Elements.identicon
                    [ width (px 45)
                    , height (px 45)
                    ]
                    persona.uri
                , text persona.name
                ]
        }



---- UPDATE ----


type Msg
    = SelectPersona SelectedPersona
    | ToggleDropdown
    | Publish
    | SizeChanged Size
    | NoOp


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

        SizeChanged size ->
            ( { model
                | size = size
              }
            , Cmd.none
            )

        NoOp ->
            ( model, Cmd.none )


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


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Browser.Events.onResize
            (\width height ->
                SizeChanged <|
                    Size width height
            )
        ]



---- ELEMENTS ----


svgIcon :
    List (Attribute msg)
    ->
        { color : Color
        , name : String
        }
    -> Element msg
svgIcon attributes { color, name } =
    el attributes <|
        html <|
            Html.node "won-svg-icon"
                [ HA.attribute "icon" name
                , HA.attribute "color" (Skin.cssColor color)
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []
