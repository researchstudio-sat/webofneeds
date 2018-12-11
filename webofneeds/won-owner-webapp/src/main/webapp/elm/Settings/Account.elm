module Settings.Account exposing
    ( Model
    , Msg
    , init
    , update
    , view
    )

import Element exposing (..)
import Element.Background as Background
import Element.Font as Font
import Element.Input as Input
import Elements
import Http
import Skin exposing (Skin)


type alias Model =
    { exportState : ExportState
    }


type alias HasFailed =
    Bool


type ExportState
    = EnteringPassword String
    | StartingExport
    | ExportStarted
    | ExportFailed



---- MODEL ----


init : () -> ( Model, Cmd Msg )
init () =
    ( { exportState = EnteringPassword "" }, Cmd.none )



---- UPDATE ----


type Msg
    = ExportPasswordChanged String
    | ExportButtonPressed
    | ExportRequestReturned (Result Http.Error ())


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        ExportPasswordChanged pwd ->
            updateExportPassword pwd model

        ExportButtonPressed ->
            startExport model

        ExportRequestReturned result ->
            case result of
                Ok () ->
                    ( { model
                        | exportState = ExportStarted
                      }
                    , Cmd.none
                    )

                Err _ ->
                    ( { model
                        | exportState = ExportFailed
                      }
                    , Cmd.none
                    )


startExport : Model -> ( Model, Cmd Msg )
startExport model =
    case model.exportState of
        EnteringPassword password ->
            ( { model
                | exportState = StartingExport
              }
            , exportRequest password
            )

        _ ->
            ( model, Cmd.none )


exportRequest : String -> Cmd Msg
exportRequest password =
    Http.post
        { url = "./rest/users/exportAccount"
        , body =
            Http.multipartBody
                [ Http.stringPart "password" password
                ]
        , expect = Http.expectWhatever ExportRequestReturned
        }


updateExportPassword : String -> Model -> ( Model, Cmd Msg )
updateExportPassword password model =
    ( case model.exportState of
        EnteringPassword _ ->
            { model
                | exportState = EnteringPassword password
            }

        _ ->
            model
    , Cmd.none
    )



---- VIEW ----


view : Skin -> Model -> Element Msg
view skin model =
    case model.exportState of
        EnteringPassword password ->
            column
                [ width fill
                , spacing 20
                , Font.size 16
                ]
                [ el [ Font.size 24 ] <| text "Account data export"
                , textColumn
                    [ width fill
                    , spacing 10
                    ]
                    [ paragraph [ width fill ]
                        [ text "All your account data will be sent to your email address"
                        ]
                    , paragraph [ width fill ]
                        [ text "Your private keys will be encrypted."
                        ]
                    ]
                , row
                    [ spacing 10
                    ]
                    [ Input.newPassword [ width fill ]
                        { onChange = ExportPasswordChanged
                        , text = password
                        , placeholder = Nothing
                        , label =
                            Input.labelLeft
                                [ centerY
                                , paddingEach
                                    { left = 0
                                    , top = 0
                                    , bottom = 0
                                    , right = 10
                                    }
                                ]
                                (text "Data encryption password:")
                        , show = False
                        }
                    , Elements.mainButton skin
                        [ height fill ]
                        { onPress =
                            if String.isEmpty password then
                                Nothing

                            else
                                Just ExportButtonPressed
                        , label = text "Export"
                        }
                    ]
                ]

        StartingExport ->
            el
                [ width fill
                , Background.color skin.primaryColor
                , Font.color Skin.white
                , padding 20
                ]
            <|
                text "Starting Export..."

        ExportStarted ->
            el
                [ width fill
                , Background.color skin.primaryColor
                , Font.color Skin.white
                , padding 20
                ]
            <|
                text "Export Started. You will get an e-mail soon"

        ExportFailed ->
            el
                [ width fill
                , Background.color skin.primaryColor
                , Font.color Skin.white
                , padding 20
                ]
            <|
                text "Export failed. Please try again later"
