port module SubmitButton exposing (main)

import Browser
import Browser.Events
import Dict exposing (Dict)
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Font as Font
import Element.Input as Input
import Elements exposing (svgIcon)
import Html exposing (Html)
import Html.Attributes as HA
import Json.Decode as Decode exposing (Value)
import NonEmpty
import Old.Persona as Persona exposing (Persona, PersonaData, SaveState(..))
import Old.Skin as Skin exposing (Skin)
import Time exposing (Posix)
import Url exposing (Url)


main =
    Skin.skinnedElement
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
    , options : Options
    , size : Size
    }


type alias Size =
    { width : Int
    , height : Int
    }


type alias Options =
    { isValid : Bool
    , loggedIn : Bool
    , showPersonas : Bool
    , label : String
    }


type State
    = Closed
    | Open


type SelectedPersona
    = Anonymous
    | Persona String


init : { width : Int, height : Int } -> ( Model, Cmd Msg )
init { width, height } =
    ( { personas = Dict.empty
      , state = Closed
      , selectedPersona = Anonymous
      , options =
            { isValid = False
            , loggedIn = False
            , showPersonas = False
            , label = "TBD"
            }
      , size =
            { width = width
            , height = height
            }
      }
    , Cmd.none
    )



---- VIEW ----


view : Skin -> Model -> Html Msg
view skin model =
    let
        buttonColor =
            if model.options.isValid then
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
        [ Font.size 16
        , width shrink
        ]
    <|
        if not model.options.loggedIn || not model.options.showPersonas then
            Input.button
                [ width (maximum (model.size.width // 2) shrink)
                , height fill
                , focusStyle
                , Background.color buttonColor
                , Border.roundEach
                    { topLeft = 0
                    , bottomLeft = 0
                    , topRight = 3
                    , bottomRight = 3
                    }
                , paddingEach
                    { left = 10
                    , right = 10
                    , top = 0
                    , bottom = 0
                    }
                ]
                { onPress = Just Publish
                , label =
                    paragraph
                        [ centerY
                        , centerX
                        , Font.color Skin.white
                        ]
                        [ text model.options.label ]
                }

        else
            row
                [ spacing 2
                , height fill
                , width (maximum (model.size.width // 2) fill)
                , above <|
                    case model.state of
                        Open ->
                            personaList skin
                                model.size
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
                    [ height fill
                    ]
                  <|
                    Input.button
                        [ Background.color buttonColor
                        , width fill
                        , height fill
                        , focusStyle
                        , paddingEach
                            { left = 10
                            , right = 10
                            , top = 0
                            , bottom = 0
                            }
                        ]
                        { onPress =
                            if model.options.isValid then
                                Just Publish

                            else
                                Nothing
                        , label =
                            paragraph
                                [ width fill
                                , centerY
                                , centerX
                                , Font.color Skin.white
                                ]
                                [ text
                                    (case model.selectedPersona of
                                        Persona url ->
                                            case Dict.get url model.personas of
                                                Just persona ->
                                                    model.options.label
                                                        ++ " as "
                                                        ++ (Persona.data persona
                                                                |> .displayName
                                                                |> NonEmpty.get
                                                           )

                                                Nothing ->
                                                    ""

                                        Anonymous ->
                                            model.options.label ++ " Anonymously"
                                    )
                                ]
                        }

                -- Persona-Drop-Up Button
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
    ->
        List
            { url : Url
            , timestamp : Posix
            , data : PersonaData
            }
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
                |> List.sortBy (.timestamp >> Time.posixToMillis)
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
        , alignLeft
        , height (maximum maxHeight shrink)
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
                    [ width (px 45)
                    , height (px 45)
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
    | OptionsUpdated Options
    | Publish
    | SizeChanged Size
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
            ( { model
                | state = Closed
              }
            , publishOut
                (case model.selectedPersona of
                    Persona url ->
                        Just url

                    Anonymous ->
                        Nothing
                )
            )

        OptionsUpdated options ->
            ( { model
                | options = options
              }
            , Cmd.none
            )

        SizeChanged size ->
            ( { model
                | size = size
              }
            , Cmd.none
            )

        NoOp ->
            ( model, Cmd.none )


subscriptions : Model -> Sub Msg
subscriptions model =
    Sub.batch
        [ Persona.subscription ReceivedPersonas (always NoOp)
        , publishIn OptionsUpdated
        , Browser.Events.onResize
            (\width height ->
                SizeChanged <|
                    Size width height
            )
        ]



---- PORTS ----


port publishIn : (Options -> msg) -> Sub msg


port publishOut : Maybe String -> Cmd msg
