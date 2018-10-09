port module Settings.Identities exposing (main)

import Browser
import Dict exposing (Dict)
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events as Events
import Element.Font as Font
import Element.Input as Input
import Html exposing (Html, node)
import Html.Attributes as HA
import Skin exposing (Skin)
import String.Extra as String
import Validate exposing (Valid, Validator)


main =
    Browser.element
        { init = \() -> ( init, Cmd.none )
        , update = update
        , view = view
        , subscriptions = subscriptions
        }



---- IDENTIY ----


type alias IdentityForm =
    { displayName : String
    , website : String
    , aboutMe : String
    }


type alias Identity =
    { displayName : String
    , website : Maybe String
    , aboutMe : Maybe String
    }


type ValidationError
    = DisplayNameError String


identityValidator : Validator ValidationError IdentityForm
identityValidator =
    Validate.all
        [ Validate.ifBlank .displayName (DisplayNameError "Please enter a display name.")
        ]


blankForm : IdentityForm
blankForm =
    { displayName = ""
    , website = ""
    , aboutMe = ""
    }


fromForm : Valid IdentityForm -> Identity
fromForm valid =
    let
        form =
            Validate.fromValid valid
    in
    { displayName = form.displayName
    , website = String.nonEmpty form.website
    , aboutMe = String.nonEmpty form.aboutMe
    }



---- PORTS ----


port identitiesInPort :
    ({ url : Url
     , identity : Identity
     }
     -> msg
    )
    -> Sub msg


port identitiesOutPort : Identity -> Cmd msg



---- MODEL ----


type Model
    = Loading
        { skin : Skin
        , creating : Maybe IdentityForm
        , createQueue : List Identity
        }
    | Loaded
        { skin : Skin
        , viewState : ViewState
        , createQueue : List Identity
        , identities : Dict Url Identity
        }


type ViewState
    = Inactive
    | Creating IdentityForm
    | Viewing Url


type alias Url =
    String


init : Model
init =
    Loading
        { skin =
            { primaryColor = rgb255 240 70 70
            , lightGray = rgb255 240 242 244
            , lineGray = rgb255 203 210 209
            , subtitleGray = rgb255 128 128 128
            , black = rgb255 0 0 0
            , white = rgb255 255 255 255
            }
        , creating = Nothing
        , createQueue = []
        }



---- UPDATE ----


type Msg
    = Create
    | Save
    | ReceivedIdentity Url Identity
    | View Url
    | Cancel
    | FormUpdated IdentityForm


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case model of
        --
        -- Loading State --
        --
        Loading loadingModel ->
            case ( msg, loadingModel.creating ) of
                ( Create, Nothing ) ->
                    ( Loading
                        { loadingModel
                            | creating = Just blankForm
                        }
                    , Cmd.none
                    )

                ( Save, Just form ) ->
                    case saveForm form of
                        Just ( identity, cmd ) ->
                            ( Loading
                                { loadingModel
                                    | creating = Nothing
                                    , createQueue = identity :: loadingModel.createQueue
                                }
                            , cmd
                            )

                        Nothing ->
                            ( model, Cmd.none )

                ( Cancel, Just _ ) ->
                    ( Loading
                        { loadingModel
                            | creating = Nothing
                        }
                    , Cmd.none
                    )

                ( ReceivedIdentity url id, _ ) ->
                    ( Loaded
                        { skin = loadingModel.skin
                        , viewState =
                            case loadingModel.creating of
                                Just form ->
                                    Creating form

                                Nothing ->
                                    Inactive
                        , identities = Dict.singleton url id
                        , createQueue =
                            List.filter (\identity -> identity /= id) loadingModel.createQueue
                        }
                    , Cmd.none
                    )

                ( FormUpdated newForm, Just _ ) ->
                    ( Loading
                        { loadingModel
                            | creating = Just newForm
                        }
                    , Cmd.none
                    )

                _ ->
                    ( model, Cmd.none )

        --
        -- Loaded State --
        --
        Loaded loadedModel ->
            let
                unsavedContent =
                    case loadedModel.viewState of
                        Creating form ->
                            form /= blankForm

                        _ ->
                            False
            in
            case msg of
                Save ->
                    case loadedModel.viewState of
                        Creating form ->
                            case saveForm form of
                                Just ( identity, cmd ) ->
                                    ( Loaded
                                        { loadedModel
                                            | viewState = Inactive
                                            , createQueue = identity :: loadedModel.createQueue
                                        }
                                    , cmd
                                    )

                                Nothing ->
                                    ( model, Cmd.none )

                        _ ->
                            ( model, Cmd.none )

                Cancel ->
                    ( Loaded
                        { loadedModel
                            | viewState = Inactive
                        }
                    , Cmd.none
                    )

                View url ->
                    if unsavedContent then
                        ( model, Cmd.none )

                    else
                        ( Loaded
                            { loadedModel
                                | viewState = Viewing url
                            }
                        , Cmd.none
                        )

                Create ->
                    if unsavedContent then
                        ( model, Cmd.none )

                    else
                        ( Loaded
                            { loadedModel
                                | viewState = Creating blankForm
                            }
                        , Cmd.none
                        )

                ReceivedIdentity url id ->
                    ( Loaded
                        { loadedModel
                            | identities = Dict.insert url id loadedModel.identities
                            , createQueue =
                                List.filter (\identity -> identity /= id) loadedModel.createQueue
                        }
                    , Cmd.none
                    )

                FormUpdated newForm ->
                    case loadedModel.viewState of
                        Creating _ ->
                            ( Loaded
                                { loadedModel
                                    | viewState = Creating newForm
                                }
                            , Cmd.none
                            )

                        _ ->
                            ( model, Cmd.none )


saveForm : IdentityForm -> Maybe ( Identity, Cmd Msg )
saveForm form =
    case Validate.validate identityValidator form of
        Ok valid ->
            -- TODO: Send create command
            let
                identity =
                    fromForm valid
            in
            Just ( identity, identitiesOutPort identity )

        Err _ ->
            Nothing


subscriptions : Model -> Sub Msg
subscriptions _ =
    identitiesInPort (\{ url, identity } -> ReceivedIdentity url identity)



---- VIEW ----


view : Model -> Html Msg
view model =
    layout [] <|
        el
            [ padding 20
            , Font.size 14
            , width <| maximum 600 fill
            , centerX
            ]
        <|
            case model of
                --
                -- Loading
                --
                Loading { skin, creating, createQueue } ->
                    column
                        [ width fill
                        , spacing 20
                        ]
                        [ case creating of
                            Just form ->
                                createInterface skin form

                            Nothing ->
                                createButton skin
                        , listUnsaved skin createQueue
                        , el [ Font.color skin.subtitleGray ] <|
                            text "Loading Identities..."
                        ]

                --
                -- Loaded
                --
                Loaded { skin, viewState, identities, createQueue } ->
                    case viewState of
                        Inactive ->
                            column
                                [ width fill
                                , spacing 20
                                ]
                                [ createButton skin
                                , listUnsaved skin createQueue
                                , viewIdentities
                                    { skin = skin
                                    , viewedUrl = Nothing
                                    , identities = identities
                                    }
                                ]

                        Viewing url ->
                            column
                                [ width fill
                                , spacing 20
                                ]
                                [ createButton skin
                                , listUnsaved skin createQueue
                                , viewIdentities
                                    { skin = skin
                                    , viewedUrl = Just url
                                    , identities = identities
                                    }
                                ]

                        Creating form ->
                            column
                                [ width fill
                                , spacing 20
                                ]
                                [ createInterface skin form
                                , listUnsaved skin createQueue
                                , viewIdentities
                                    { skin = skin
                                    , viewedUrl = Nothing
                                    , identities = identities
                                    }
                                ]


createButton : Skin -> Element Msg
createButton skin =
    Input.button
        [ width fill
        , Border.color skin.lineGray
        , padding 5
        , Border.width 1
        ]
        { onPress = Just Create
        , label =
            el
                [ Font.color skin.lineGray
                , centerX
                , Font.size 32
                ]
            <|
                text "+"
        }


createInterface : Skin -> IdentityForm -> Element Msg
createInterface skin form =
    let
        validated =
            Validate.validate identityValidator form

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
        , header =
            column
                [ width fill
                , spacing 10
                ]
                [ Input.text
                    [ centerY
                    ]
                    { onChange = \str -> FormUpdated { form | displayName = str }
                    , text = form.displayName
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
            [ identityForm form
            , row
                [ spacing 10
                , width fill
                ]
                [ mainButton
                    { disabled = not isValid || form == blankForm
                    , onClick = Save
                    , text = "Save"
                    }
                , outlinedButton
                    { disabled = False
                    , onClick = Cancel
                    , text = "Cancel"
                    }
                ]
            ]
        }


identityForm : IdentityForm -> Element Msg
identityForm form =
    column
        [ spacing 10
        , width fill
        ]
        [ Input.text []
            { onChange = \str -> FormUpdated { form | website = str }
            , text = form.website
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "Website")
            }
        , Input.multiline []
            { onChange = \str -> FormUpdated { form | aboutMe = str }
            , text = form.aboutMe
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "About Me")
            , spellcheck = True
            }
        ]


listUnsaved : Skin -> List Identity -> Element Msg
listUnsaved skin unsaved =
    if List.isEmpty unsaved then
        none

    else
        column
            [ spacing 20
            , width fill
            ]
        <|
            List.map
                (\id ->
                    viewUnsaved skin id
                )
                unsaved


viewIdentities :
    { skin : Skin
    , viewedUrl : Maybe Url
    , identities : Dict Url Identity
    }
    -> Element Msg
viewIdentities { skin, viewedUrl, identities } =
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
        Dict.map
            (\url id ->
                viewIdentity
                    { skin = skin
                    , open = open url
                    , url = url
                    , identity = id
                    }
            )
            identities
            |> Dict.values


viewUnsaved : Skin -> Identity -> Element Msg
viewUnsaved skin identity =
    card
        [ width fill

        -- overlay: "Saving..." --
        , inFront <|
            el
                [ width fill
                , height fill
                , Background.color (Skin.setAlpha 0.5 skin.black)
                ]
            <|
                el
                    [ Font.color skin.white
                    , Font.size 18
                    , centerX
                    , centerY
                    ]
                    (text "Saving...")
        ]
        -- card in background --
        { skin = skin
        , header =
            row
                [ spacing 10
                , width fill
                ]
                [ el
                    [ width (px 50)
                    , height (px 50)
                    , Background.color skin.lineGray
                    ]
                    none
                , el
                    [ centerY
                    , Font.size 18
                    ]
                  <|
                    text identity.displayName
                ]
        , sections = []
        }


viewIdentity :
    { skin : Skin
    , open : Bool
    , url : Url
    , identity : Identity
    }
    -> Element Msg
viewIdentity { skin, open, url, identity } =
    card
        [ width fill
        , Events.onClick
            (if open then
                Cancel

             else
                View url
            )
        ]
        { skin = skin
        , header =
            row
                [ spacing 15
                , width fill
                ]
                [ identicon
                    [ width (px 50)
                    , height (px 50)
                    ]
                    url
                , column
                    [ centerY ]
                    [ el [ Font.size 18 ] <|
                        text identity.displayName
                    ]
                ]
        , sections =
            if open then
                [ details skin
                    [ Maybe.map
                        (\website ->
                            Inline
                                { title = "Website"
                                , value = website
                                }
                        )
                        identity.website
                    , Maybe.map
                        (\aboutMe ->
                            Block
                                { title = "About Me"
                                , value = aboutMe
                                }
                        )
                        identity.aboutMe
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
        }
    -> Element msg
card attributes { skin, header, sections } =
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
        ([ el
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


type alias ButtonConfig msg =
    { disabled : Bool
    , text : String
    , onClick : msg
    }


mainButton : ButtonConfig msg -> Element msg
mainButton { disabled, text, onClick } =
    el [ Events.onClick onClick ] <|
        html <|
            Html.button
                [ HA.classList
                    [ ( "won-button--filled", True )
                    , ( "red", True )
                    ]
                , HA.disabled disabled
                ]
                [ Html.text text ]


outlinedButton : ButtonConfig msg -> Element msg
outlinedButton { disabled, text, onClick } =
    el [ Events.onClick onClick ] <|
        html <|
            Html.button
                [ HA.classList
                    [ ( "won-button--outlined", True )
                    , ( "thin", True )
                    , ( "red", True )
                    ]
                , HA.disabled disabled
                ]
                [ Html.text text ]


identicon : List (Attribute msg) -> String -> Element msg
identicon attributes string =
    el
        attributes
    <|
        html <|
            node "won-identicon"
                [ HA.attribute "data" string
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []
