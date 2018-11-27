module Settings.Personas exposing (main)

import Browser
import Dict exposing (Dict)
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events as Events
import Element.Font as Font
import Element.Input as Input
import Elements
import Html exposing (Html, node)
import Html.Attributes as HA
import Json.Decode as Decode exposing (Value)
import NonEmpty
import Persona exposing (Persona, PersonaData, SaveState(..))
import Skin exposing (Skin)
import String.Extra as String
import Time
import Url
import Validate exposing (Valid, Validator)


main =
    Skin.skinnedElement
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }



---- PERSONA ----


type alias Draft =
    { displayName : String
    , website : String
    , aboutMe : String
    }


type ValidationError
    = DisplayNameError String
    | UnknownError String


personaValidator : Validator ValidationError Draft
personaValidator =
    Validate.all
        [ Validate.ifBlank .displayName (DisplayNameError "Please enter a display name.")
        ]


blankDraft : Draft
blankDraft =
    { displayName = ""
    , website = ""
    , aboutMe = ""
    }


validatePersona : Draft -> Result (List ValidationError) PersonaData
validatePersona draftToValidate =
    Validate.validate personaValidator draftToValidate
        |> Result.andThen
            (\valid ->
                let
                    draft =
                        Validate.fromValid valid
                in
                case NonEmpty.string draft.displayName of
                    Just displayName ->
                        Ok
                            { displayName = displayName
                            , website = NonEmpty.string draft.website
                            , aboutMe = NonEmpty.string draft.aboutMe
                            }

                    Nothing ->
                        Err [ UnknownError "Unknown error occurred while parsing persona data" ]
            )



---- MODEL ----


type alias Model =
    { viewState : ViewState
    , personas : Dict Url Persona
    }


type ViewState
    = Inactive
    | Creating Draft
    | Viewing Url


type alias Url =
    String


init : () -> ( Model, Cmd Msg )
init () =
    ( { viewState = Inactive
      , personas = Dict.empty
      }
    , Cmd.none
    )



---- UPDATE ----


type Msg
    = Create
    | Save
    | ReceivedPersonas (Dict Url Persona)
    | View Url
    | Cancel
    | DraftUpdated Draft
    | NoOp


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    let
        unsavedContent =
            case model.viewState of
                Creating draft ->
                    draft /= blankDraft

                _ ->
                    False
    in
    case msg of
        Save ->
            case model.viewState of
                Creating draft ->
                    case saveDraft draft of
                        Just ( persona, cmd ) ->
                            ( { model
                                | viewState = Inactive
                              }
                            , cmd
                            )

                        Nothing ->
                            ( model, Cmd.none )

                _ ->
                    ( model, Cmd.none )

        Cancel ->
            ( { model
                | viewState = Inactive
              }
            , Cmd.none
            )

        View url ->
            if unsavedContent then
                ( model, Cmd.none )

            else
                ( { model
                    | viewState = Viewing url
                  }
                , Cmd.none
                )

        Create ->
            if unsavedContent then
                ( model, Cmd.none )

            else
                ( { model
                    | viewState = Creating blankDraft
                  }
                , Cmd.none
                )

        ReceivedPersonas newPersonas ->
            ( { model
                | personas = newPersonas
              }
            , Cmd.none
            )

        DraftUpdated newDraft ->
            case model.viewState of
                Creating _ ->
                    ( { model
                        | viewState = Creating newDraft
                      }
                    , Cmd.none
                    )

                _ ->
                    ( model, Cmd.none )

        NoOp ->
            ( model, Cmd.none )


pruneSaveQueue : Dict Url Persona -> List PersonaData -> List PersonaData
pruneSaveQueue personas queue =
    let
        newData =
            List.map Persona.data (Dict.values personas)
    in
    List.filter (\persona -> not <| List.member persona newData) queue


saveDraft : Draft -> Maybe ( PersonaData, Cmd Msg )
saveDraft draft =
    validatePersona draft
        |> Result.map
            (\personaData ->
                ( personaData, Persona.savePersona personaData )
            )
        |> Result.toMaybe


subscriptions : Model -> Sub Msg
subscriptions _ =
    Sub.batch
        [ Persona.subscription ReceivedPersonas (\_ -> NoOp)
        ]



---- VIEW ----


view : Skin -> Model -> Html Msg
view skin { viewState, personas } =
    layout [] <|
        el
            [ padding 20
            , Font.size 16
            , width <| maximum 600 fill
            , centerX
            ]
        <|
            case viewState of
                Inactive ->
                    column
                        [ width fill
                        , spacing 20
                        ]
                        [ createButton skin
                        , listUnsaved skin personas
                        , listPersonas
                            { skin = skin
                            , viewedUrl = Nothing
                            , personas = personas
                            }
                        ]

                Viewing url ->
                    column
                        [ width fill
                        , spacing 20
                        ]
                        [ createButton skin
                        , listUnsaved skin personas
                        , listPersonas
                            { skin = skin
                            , viewedUrl = Just url
                            , personas = personas
                            }
                        ]

                Creating draft ->
                    column
                        [ width fill
                        , spacing 20
                        ]
                        [ createInterface skin draft
                        , listUnsaved skin personas
                        , listPersonas
                            { skin = skin
                            , viewedUrl = Nothing
                            , personas = personas
                            }
                        ]


createButton : Skin -> Element Msg
createButton skin =
    Input.button
        [ width fill
        , Background.color skin.primaryColor
        , padding 5
        ]
        { onPress = Just Create
        , label =
            el
                [ Font.color Skin.white
                , centerX
                , Font.size 32
                ]
            <|
                text "+"
        }


createInterface : Skin -> Draft -> Element Msg
createInterface skin draft =
    let
        validated =
            Validate.validate personaValidator draft

        ( isValid, errors ) =
            case validated of
                Ok _ ->
                    ( True, [] )

                Err err ->
                    ( False, err )
    in
    card
        [ width fill ]
        { skin = skin
        , onPress = Nothing
        , header =
            column
                [ width fill
                , spacing 10
                ]
                [ Input.text
                    [ centerY
                    ]
                    { onChange = \str -> DraftUpdated { draft | displayName = str }
                    , text = draft.displayName
                    , placeholder = Just (Input.placeholder [] <| text "Display Name")
                    , label =
                        Input.labelAbove
                            [ width (px 0)
                            , height (px 0)
                            , htmlAttribute (HA.style "display" "none")
                            ]
                            (text "Display Name")
                    }
                , errors
                    |> List.filterMap
                        (\error ->
                            case error of
                                DisplayNameError str ->
                                    Just str

                                _ ->
                                    Nothing
                        )
                    |> List.head
                    |> Maybe.map
                        (\str ->
                            el [ Font.color skin.primaryColor ] <|
                                text str
                        )
                    |> Maybe.withDefault none
                ]
        , sections =
            [ personaForm draft
            , row
                [ spacing 10
                , width fill
                ]
                [ Elements.mainButton
                    { disabled = not isValid || draft == blankDraft
                    , onClick = Save
                    , text = "Save"
                    }
                , Elements.outlinedButton
                    { disabled = False
                    , onClick = Cancel
                    , text = "Cancel"
                    }
                ]
            ]
        }


personaForm : Draft -> Element Msg
personaForm draft =
    column
        [ spacing 10
        , width fill
        ]
        [ Input.text []
            { onChange = \str -> DraftUpdated { draft | website = str }
            , text = draft.website
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "Website")
            }
        , Input.multiline []
            { onChange = \str -> DraftUpdated { draft | aboutMe = str }
            , text = draft.aboutMe
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "About Me")
            , spellcheck = True
            }
        ]


listUnsaved : Skin -> Dict Url Persona -> Element Msg
listUnsaved skin personas =
    let
        unsaved =
            Dict.values personas
                |> List.filterMap
                    (\persona ->
                        case Persona.saved persona of
                            Saved _ ->
                                Nothing

                            Unsaved ->
                                Just
                                    { url = Persona.url persona
                                    , data = Persona.data persona
                                    }
                    )
    in
    if List.isEmpty unsaved then
        none

    else
        column
            [ spacing 20
            , width fill
            ]
        <|
            List.map
                (\{ url, data } ->
                    viewUnsaved
                        { skin = skin
                        , data = data
                        , url = Url.toString url
                        }
                )
                unsaved


listPersonas :
    { skin : Skin
    , viewedUrl : Maybe Url
    , personas : Dict Url Persona
    }
    -> Element Msg
listPersonas { skin, viewedUrl, personas } =
    column
        [ spacing 20
        , width fill
        ]
    <|
        let
            open =
                case viewedUrl of
                    Just url ->
                        \targetUrl -> url == targetUrl

                    Nothing ->
                        always False
        in
        Dict.values personas
            |> List.filterMap
                (\persona ->
                    case Persona.saved persona of
                        Saved timestamp ->
                            Just
                                { timestamp = timestamp
                                , url = Persona.url persona
                                , data = Persona.data persona
                                }

                        Unsaved ->
                            Nothing
                )
            |> List.sortWith
                (\left right ->
                    case
                        compare
                            (Time.posixToMillis left.timestamp)
                            (Time.posixToMillis right.timestamp)
                    of
                        LT ->
                            GT

                        GT ->
                            LT

                        EQ ->
                            EQ
                )
            |> List.map
                (\{ url, data } ->
                    viewPersona
                        { skin = skin
                        , open = open (Url.toString url)
                        , url = Url.toString url
                        , data = data
                        }
                )


viewUnsaved :
    { skin : Skin
    , data : PersonaData
    , url : Url
    }
    -> Element Msg
viewUnsaved { skin, data, url } =
    card
        [ width fill

        -- overlay: "Saving..." --
        , inFront <|
            el
                [ width fill
                , height fill
                , Background.color (Skin.setAlpha 0.5 Skin.black)
                ]
            <|
                el
                    [ Font.color Skin.white
                    , Font.size 18
                    , centerX
                    , centerY
                    ]
                    (text "Saving...")
        ]
        -- card in background --
        { skin = skin
        , onPress = Nothing
        , header =
            row
                [ spacing 10
                , width fill
                ]
                [ Elements.identicon
                    [ width (px 50)
                    , height (px 50)
                    ]
                    url
                , el
                    [ centerY
                    , Font.size 18
                    ]
                  <|
                    text <|
                        NonEmpty.get data.displayName
                ]
        , sections = []
        }


viewPersona :
    { skin : Skin
    , open : Bool
    , url : Url
    , data : PersonaData
    }
    -> Element Msg
viewPersona { skin, open, url, data } =
    card
        [ width fill
        ]
        { skin = skin
        , onPress =
            Just <|
                if open then
                    Cancel

                else
                    View url
        , header =
            row
                [ spacing 15
                , width fill
                ]
                [ Elements.identicon
                    [ width (px 50)
                    , height (px 50)
                    ]
                    url
                , column
                    [ centerY ]
                    [ el [ Font.size 18 ] <|
                        text <|
                            NonEmpty.get data.displayName
                    ]
                ]
        , sections =
            if open then
                [ details skin
                    [ Maybe.map
                        (\website ->
                            Inline
                                { title = "Website"
                                , value = NonEmpty.get website
                                }
                        )
                        data.website
                    , Maybe.map
                        (\aboutMe ->
                            Block
                                { title = "About Me"
                                , value = NonEmpty.get aboutMe
                                }
                        )
                        data.aboutMe
                    ]
                ]

            else
                []
        }


type Detail
    = Inline
        { title : String
        , value : String
        }
    | Block
        { title : String
        , value : String
        }


details : Skin -> List (Maybe Detail) -> Element msg
details skin d =
    let
        detailList =
            List.filterMap
                (Maybe.map
                    (\detail ->
                        case detail of
                            Inline { title, value } ->
                                row [ spacing 10 ]
                                    [ el [ Font.bold, width (minimum 80 shrink) ] <| text (title ++ ":")
                                    , text value
                                    ]

                            Block { title, value } ->
                                column [ spacing 10 ]
                                    [ el [ Font.bold ] <| text (title ++ ":")
                                    , paragraph [] [ text value ]
                                    ]
                    )
                )
                d
    in
    if List.isEmpty detailList then
        el
            [ Font.size 20
            , width fill
            , padding 10
            , Font.center
            , Font.color skin.lineGray
            ]
        <|
            text "No Details"

    else
        column [ spacing 10 ] detailList



---- ELEMENTS ----


card :
    List (Attribute msg)
    ->
        { skin : Skin
        , header : Element msg
        , sections : List (Element msg)
        , onPress : Maybe msg
        }
    -> Element msg
card attributes { skin, header, sections, onPress } =
    let
        baseStyle =
            [ Border.width 1
            , Border.color skin.lineGray
            , padding 10
            , width fill
            ]
    in
    column
        (attributes
            ++ [ spacing -1 ]
        )
        ([ case onPress of
            Just msg ->
                Input.button
                    (baseStyle
                        ++ [ Background.color skin.lightGray
                           , focused
                                []
                           ]
                    )
                    { onPress = Just msg
                    , label = header
                    }

            Nothing ->
                el
                    (baseStyle
                        ++ [ Background.color skin.lightGray ]
                    )
                    header
         ]
            ++ List.map
                (\section ->
                    el baseStyle section
                )
                sections
        )
