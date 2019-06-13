module Settings.Personas exposing
    ( Model
    , Msg
    , init
    , subscriptions
    , update
    , view
    )

import Dict exposing (Dict)
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Font as Font
import Element.Input as Input
import Elements
import Html.Attributes as HA
import Markdown
import NonEmpty
import Old.Persona as Persona exposing (Persona, PersonaData, SaveState(..))
import Old.Skin as Skin exposing (Skin)
import Regex
import String.Extra as String
import Url
import Validate exposing (Validator)



---- PERSONA ----


type alias Draft =
    { displayName : String
    , website : String
    , aboutMe : String
    }


type ValidationError
    = DisplayNameError String
    | UrlError String
    | UnknownError String


urlRegex : Regex.Regex
urlRegex =
    Regex.fromStringWith
        { caseInsensitive = True
        , multiline = False
        }
        "^(?:[a-z][a-z\\d\\-\\+\\.]*:(?:\\/\\/)?)?[\\w\\-.]+(?:\\.|@|:)[\\w\\-.]{2,}\\b(?:[\\w\\-.~:/?#[\\]@!$&'()*+,;=%])*$"
        |> Maybe.withDefault Regex.never


ifInvalidUrl : (subject -> String) -> error -> Validator error subject
ifInvalidUrl getter error =
    Validate.ifFalse
        (\subject ->
            let
                url =
                    getter subject
            in
            Regex.contains urlRegex url
                || String.isEmpty url
        )
        error


personaValidator : Validator ValidationError Draft
personaValidator =
    Validate.all
        [ Validate.ifBlank .displayName (DisplayNameError "Please enter a display name.")
        , ifInvalidUrl .website (UrlError "Entered website url is not valid")
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
    , Persona.getNewPersonas
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
                        Just ( _, cmd ) ->
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


view : Skin -> Model -> Element Msg
view skin { viewState, personas } =
    column
        [ width fill
        , spacing 10
        , Font.size 16
        ]
        [ el [ Font.size 24 ] <| text "Persona Management"
        , textColumn [ width fill ]
            [ paragraph []
                [ text "Your posts are anonymous by default. You can make them more personalized by attaching a persona." ]
            ]
        , case viewState of
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
        ]


createButton : Skin -> Element Msg
createButton skin =
    Input.button
        [ width <| maximum 300 fill
        , centerX
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
            [ personaForm skin draft
            , row
                [ spacing 10
                , width fill
                ]
                [ Elements.mainButton
                    skin
                    []
                    { onPress =
                        if isValid then
                            Just Save

                        else
                            Nothing
                    , label = text "Save"
                    }
                , Elements.outlinedButton
                    skin
                    { onPress = Just Cancel
                    , label = text "Cancel"
                    }
                ]
            ]
        }


personaForm : Skin -> Draft -> Element Msg
personaForm skin draft =
    let
        validated =
            Validate.validate personaValidator draft

        ( _, errors ) =
            case validated of
                Ok _ ->
                    ( True, [] )

                Err err ->
                    ( False, err )
    in
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
        , errors
            |> List.filterMap
                (\error ->
                    case error of
                        UrlError str ->
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
            |> List.sortBy
                (\persona ->
                    NonEmpty.get persona.data.displayName
                        |> String.toLower
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
                    , Border.rounded 25
                    , clip
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
                    , Border.rounded 25
                    , clip
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
                            Markdown
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
    | Markdown
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

                            Markdown { title, value } ->
                                column
                                    [ spacing 10
                                    , width fill
                                    ]
                                    [ el [ Font.bold ] <| text (title ++ ":")
                                    , el
                                        [ Border.widthEach
                                            { top = 1
                                            , bottom = 0
                                            , left = 0
                                            , right = 0
                                            }
                                        , Border.color skin.lineGray
                                        , width fill
                                        ]
                                        none
                                    , el
                                        [ width fill
                                        , padding 5
                                        ]
                                      <|
                                        html <|
                                            Markdown.toHtml
                                                [ HA.class "markdown"
                                                , HA.style "white-space" "normal"
                                                ]
                                                value
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
        column
            [ spacing 10
            , width fill
            ]
            detailList



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
