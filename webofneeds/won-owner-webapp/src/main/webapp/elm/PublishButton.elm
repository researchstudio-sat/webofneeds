module PublishButton exposing (Model)

import Browser
import Dict exposing (Dict)
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Font as Font
import Element.Input as Input
import Elements
import Html exposing (Html)
import Html.Attributes as HA
import NonEmpty
import Persona exposing (Persona, PersonaData, SaveState(..))
import Skin exposing (Skin)
import Time exposing (Posix)
import Url exposing (Url)


main =
    Browser.element
        { init = init
        , subscriptions = subscriptions
        , update = update
        , view = view
        }



---- MODEL ----


type alias Model =
    { personas : Dict String Persona
    , state : State
    , selectedPersona : SelectedPersona
    , skin : Skin
    , draftValid : Bool
    }


type State
    = Closed
    | Open


type SelectedPersona
    = Anonymous
    | Persona String


init : () -> ( Model, Cmd Msg )
init () =
    ( { personas = Dict.empty
      , state = Closed
      , selectedPersona = Anonymous
      , skin =
            { primaryColor = rgb255 240 70 70
            , lightGray = rgb255 240 242 244
            , lineGray = rgb255 203 210 209
            , subtitleGray = rgb255 128 128 128
            , black = rgb255 0 0 0
            , white = rgb255 255 255 255
            }
      , draftValid = False
      }
    , Cmd.none
    )



---- VIEW ----


view : Model -> Html Msg
view model =
    let
        skin =
            model.skin

        buttonColor =
            skin.primaryColor

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
        row
            [ width fill
            , spacing 2
            , height (px 43)
            , above <|
                case model.state of
                    Open ->
                        personaList skin
                            (model.personas
                                |> Dict.values
                                |> List.filterMap
                                    (\persona ->
                                        case Persona.saved persona of
                                            Saved timestamp ->
                                                Just
                                                    { timestamp = timestamp
                                                    , data = Persona.data persona
                                                    , url = Persona.url persona
                                                    }

                                            Unsaved ->
                                                Nothing
                                    )
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
                    ]
                    { onPress =
                        if model.draftValid then
                            Just Publish

                        else
                            Nothing
                    , label =
                        el [ centerY, centerX, Font.color skin.white ] <|
                            text
                                (case model.selectedPersona of
                                    Persona url ->
                                        case Dict.get url model.personas of
                                            Just persona ->
                                                "Publish as "
                                                    ++ (Persona.data persona
                                                            |> .displayName
                                                            |> NonEmpty.get
                                                       )

                                            Nothing ->
                                                ""

                                    Anonymous ->
                                        "Publish anonymously"
                                )
                    }
            , Input.button
                [ Background.color buttonColor
                , Border.roundEach
                    { topLeft = 0
                    , bottomLeft = 0
                    , topRight = 3
                    , bottomRight = 3
                    }
                , padding 10
                , height fill
                , focusStyle
                ]
                { onPress = Just ToggleDropdown
                , label =
                    svgIcon
                        [ width (px 16)
                        , height (px 16)
                        , centerX
                        , centerY
                        , moveUp 2
                        ]
                        { color = skin.white
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
    ->
        List
            { url : Url
            , timestamp : Posix
            , data : PersonaData
            }
    -> Element Msg
personaList skin personas =
    let
        listElements =
            (personas
                |> List.sortBy (.timestamp >> Time.posixToMillis)
                |> List.map (personaEntry skin)
            )
                ++ [ anonymousEntry ]

        anonymousEntry =
            Input.button [ width fill ]
                { onPress = Just <| SelectPersona Anonymous
                , label =
                    row
                        [ width fill
                        , spacing 5
                        ]
                        [ el
                            [ width (px 32)
                            , height (px 32)
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
    in
    column
        [ width fill
        , Border.color skin.lineGray
        , Border.width 1
        , moveUp 5
        , Background.color skin.white
        ]
        (List.intersperse line listElements)


personaEntry :
    Skin
    ->
        { url : Url
        , timestamp : Posix
        , data : PersonaData
        }
    -> Element Msg
personaEntry skin persona =
    Input.button [ width fill ]
        { onPress = Just <| SelectPersona <| Persona <| Url.toString persona.url
        , label =
            row
                [ width fill
                , spacing 5
                ]
                [ Elements.identicon
                    [ width (px 32)
                    , height (px 32)
                    ]
                    (Url.toString persona.url)
                , text <| NonEmpty.get persona.data.displayName
                ]
        }



---- UPDATE ----


type Msg
    = SelectPersona SelectedPersona
    | ToggleDropdown
    | ReceivedPersonas (Dict String Persona)
    | DraftValidityChanged Bool
    | Publish
    | NoOp


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
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

        ReceivedPersonas personas ->
            ( { model
                | personas = personas
                , selectedPersona =
                    case model.selectedPersona of
                        Persona url ->
                            if Dict.member url model.personas then
                                model.selectedPersona

                            else
                                Anonymous

                        Anonymous ->
                            model.selectedPersona
              }
            , Cmd.none
            )

        SelectPersona newSelection ->
            ( { model
                | selectedPersona =
                    case newSelection of
                        Persona url ->
                            if Dict.member url model.personas then
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
            ( model, Cmd.none )

        DraftValidityChanged valid ->
            ( { model
                | draftValid = valid
              }
            , Cmd.none
            )

        NoOp ->
            ( model, Cmd.none )


subscriptions : Model -> Sub Msg
subscriptions model =
    Persona.subscription ReceivedPersonas (always NoOp)



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
