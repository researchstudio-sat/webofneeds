module ForgotPassword exposing (main)

import Basics
import Browser.Events exposing (onResize)
import Element exposing (..)
import Element.Background as Background
import Element.Events as Events
import Element.Font as Font
import Element.Input as Input
import Elements
import Html exposing (Html)
import Http
import Json.Decode as Decode
import Json.Encode as Encode
import Old.Skin as Skin exposing (Skin)


main =
    Skin.skinnedElement
        { init = init
        , update = update
        , view = view
        , subscriptions = always Sub.none
        }


type alias Model =
    { resetState : ResetState
    , credentials : Credentials
    }


type ResetState
    = EnteringCredentials
    | ResettingPassword
    | ResetSuccessful
    | ResetFailed


type alias Credentials =
    { newPassword : String
    , email : String
    , recoveryKey : String
    }


type alias Response =
    { code : Int
    , message : String
    }


type Msg
    = ResetButtonPressed
    | MailStringChanged String
    | NewPasswordChanged String
    | RecoveryKeyChanged String
    | ExportRequestReturned (Result Http.Error ())



---- MODEL ----


init : () -> ( Model, Cmd Msg )
init () =
    ( { resetState = EnteringCredentials
      , credentials = { newPassword = "", email = "", recoveryKey = "" }
      }
    , Cmd.none
    )



---- UPDATE ----


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        ResetButtonPressed ->
            startReset model

        MailStringChanged mailString ->
            updateMailString mailString model

        NewPasswordChanged newPasswordString ->
            updatePasswordString newPasswordString model

        RecoveryKeyChanged recoveryKeyString ->
            updateRecoveryKey recoveryKeyString model

        ExportRequestReturned result ->
            case result of
                Ok () ->
                    ( { model
                        | resetState = ResetSuccessful
                      }
                    , Cmd.none
                    )

                Err err ->
                    ( { model
                        | resetState = ResetFailed
                      }
                    , Cmd.none
                    )


decodeResponse : Decode.Decoder Response
decodeResponse =
    Decode.map2 Response
        (Decode.field "code" Decode.int)
        (Decode.field "message" Decode.string)


updateMailString : String -> Model -> ( Model, Cmd Msg )
updateMailString newEnteredMailString model =
    ( case model.resetState of
        EnteringCredentials ->
            { model
                | credentials = { email = newEnteredMailString, newPassword = model.credentials.newPassword, recoveryKey = model.credentials.recoveryKey }
            }

        _ ->
            model
    , Cmd.none
    )


updatePasswordString : String -> Model -> ( Model, Cmd Msg )
updatePasswordString newPasswordString model =
    ( case model.resetState of
        EnteringCredentials ->
            { model
                | credentials = { email = model.credentials.email, newPassword = newPasswordString, recoveryKey = model.credentials.recoveryKey }
            }

        _ ->
            model
    , Cmd.none
    )


updateRecoveryKey : String -> Model -> ( Model, Cmd Msg )
updateRecoveryKey newRecoveryKeyString model =
    ( case model.resetState of
        EnteringCredentials ->
            { model
                | credentials = { email = model.credentials.email, newPassword = model.credentials.newPassword, recoveryKey = newRecoveryKeyString }
            }

        _ ->
            model
    , Cmd.none
    )


startReset : Model -> ( Model, Cmd Msg )
startReset model =
    case model.resetState of
        EnteringCredentials ->
            ( { model
                | resetState = ResettingPassword
              }
            , resetRequest model.credentials
            )

        _ ->
            ( model, Cmd.none )


resetRequest : Credentials -> Cmd Msg
resetRequest credentials =
    Http.post
        { url = "./rest/users/resetPassword"
        , body =
            Http.jsonBody <|
                Encode.object
                    [ ( "username", Encode.string credentials.email )
                    , ( "recoveryKey", Encode.string credentials.recoveryKey )
                    , ( "newPassword", Encode.string credentials.newPassword )
                    , ( "verificationToken", Encode.string "" )
                    ]
        , expect = Http.expectWhatever ExportRequestReturned
        }



---- VIEW ----


progressView : Skin -> Model -> Element Msg
progressView skin model =
    el
        [ width fill
        , Background.color skin.lineGray
        , Font.color Skin.white
        , padding 20
        ]
    <|
        text "Resetting Password..."


changeFailedView : Skin -> Model -> Element Msg
changeFailedView skin model =
    column
        [ width fill
        , spacing 20
        , Font.size 16
        ]
        [ el
            [ width fill
            , Background.color skin.primaryColor
            , Font.color Skin.white
            , padding 20
            ]
          <|
            text "Could not reset password"
        , textColumn
            [ width fill ]
            [ text "Reasons: Wrong email address/password/recovery key"
            ]
        ]


successView : Skin -> Model -> Element Msg
successView skin model =
    column
        [ width fill
        , spacing 20
        , Font.size 16
        ]
        [ el
            [ width fill
            , Background.color skin.secondaryColorLighter
            , Font.color Skin.white
            , padding 20
            ]
          <|
            text "Password reset successful"
        , textColumn
            [ width fill ]
            [ text "Check your email account for your new Recovery Key"
            ]
        ]


changeView : Skin -> Model -> Element Msg
changeView skin model =
    column
        [ width fill
        , spacing 20
        , Font.size 16
        ]
        [ textColumn
            [ width fill
            , spacing 10
            ]
            [ paragraph [ width fill ]
                [ text "Reset your password here:"
                ]
            ]
        , row
            [ spacing 10
            ]
            [ Input.email
                [ width fill
                , paddingEach
                    { left = 0
                    , top = 0
                    , bottom = 0
                    , right = 0
                    }
                ]
                { onChange = MailStringChanged
                , text = model.credentials.email
                , placeholder = Nothing
                , label =
                    Input.labelLeft
                        [ centerY
                        , Font.size 14
                        , paddingEach
                            { left = 0
                            , top = 0
                            , bottom = 0
                            , right = 70
                            }
                        ]
                        (text "Email:")
                }
            ]
        , row
            [ spacing 10
            ]
            [ Input.newPassword
                [ width fill
                , paddingEach
                    { left = 0
                    , top = 0
                    , bottom = 0
                    , right = 0
                    }
                ]
                { onChange = NewPasswordChanged
                , text = model.credentials.newPassword
                , placeholder = Nothing
                , label =
                    Input.labelLeft
                        [ centerY
                        , Font.size 14
                        , paddingEach
                            { left = 0
                            , top = 0
                            , bottom = 0
                            , right = 10
                            }
                        ]
                        (text "New Password:")
                , show = False
                }
            ]
        , if String.length model.credentials.newPassword > 0 && String.length model.credentials.newPassword < 6 then
            row [ spacing 10 ]
                [ el
                    [ width fill
                    , Font.color skin.primaryColor
                    , Font.size 10
                    ]
                  <|
                    text "Password too short, must be at least 6 Characters"
                ]

          else
            none
        , row
            [ spacing 10
            ]
            [ Input.text
                [ width fill
                , paddingEach
                    { left = 0
                    , top = 0
                    , bottom = 0
                    , right = 0
                    }
                ]
                { onChange = RecoveryKeyChanged
                , text = model.credentials.recoveryKey
                , placeholder = Nothing
                , label =
                    Input.labelLeft
                        [ centerY
                        , Font.size 14
                        , paddingEach
                            { left = 0
                            , top = 0
                            , bottom = 0
                            , right = 20
                            }
                        ]
                        (text "Recovery Key:")
                }
            ]
        , row
            [ width fill
            , spacing 20
            ]
            [ Elements.mainButton
                skin
                [ width <| maximum 320 fill
                ]
                { onPress =
                    if String.isEmpty model.credentials.newPassword then
                        Nothing

                    else if String.isEmpty model.credentials.email then
                        Nothing

                    else if String.isEmpty model.credentials.recoveryKey then
                        Nothing

                    else if String.length model.credentials.newPassword < 6 then
                        Nothing

                    else
                        Just ResetButtonPressed
                , label =
                    el
                        [ centerX
                        ]
                    <|
                        text "Reset Password"
                }
            ]
        ]


view : Skin -> Model -> Html Msg
view skin model =
    layout [ width fill ] <|
        el
            [ width <| maximum 800 fill
            , centerX
            , padding 10
            ]
        <|
            case model.resetState of
                EnteringCredentials ->
                    changeView skin model

                ResettingPassword ->
                    progressView skin model

                ResetSuccessful ->
                    successView skin model

                ResetFailed ->
                    changeFailedView skin model



---- SUBSCRIPTIONS ----
