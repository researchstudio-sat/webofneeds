port module Settings.Account exposing
    ( Model
    , Msg
    , init
    , subscriptions
    , update
    , view
    )

import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Font as Font
import Element.Input as Input
import Elements
import Http
import Json.Encode as Encode
import Old.Skin as Skin exposing (Skin)


type alias Model =
    { accountState : AccountState
    , accountInfo : AccountInfo
    , passwordList : PasswordList
    }


type alias PasswordList =
    { newPassword : String
    , newPasswordRepeat : String
    , oldPassword : String
    }


type alias AccountInfo =
    { email : String
    , isVerified : Bool
    }


type alias HasFailed =
    Bool


type AccountState
    = EnteringPassword PasswordList
    | ChangeInProgress
    | ChangeFailed
    | ChangeCompleted



---- MODEL ----


init : () -> ( Model, Cmd Msg )
init () =
    ( { accountInfo = { email = "", isVerified = False }
      , accountState = EnteringPassword { newPassword = "", newPasswordRepeat = "", oldPassword = "" }
      , passwordList = { newPassword = "", newPasswordRepeat = "", oldPassword = "" }
      }
    , getAccountInfo ()
    )



---- UPDATE ----


type Msg
    = PasswordChanged PasswordList
    | OldPasswordChanged String
    | NewPasswordChanged String
    | NewPasswordRepeatChanged String
    | AccountInfoChanged AccountInfo
    | ChangeButtonPressed
    | ChangeRequestReturned (Result Http.Error ())


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        PasswordChanged newPasswordList ->
            updatePasswordList newPasswordList model

        NewPasswordChanged newPwd ->
            updateNewPassword newPwd model

        NewPasswordRepeatChanged newPwdR ->
            updateNewPasswordRepeat newPwdR model

        OldPasswordChanged oldPwd ->
            updateOldPassword oldPwd model

        ChangeButtonPressed ->
            changePassword model

        ChangeRequestReturned result ->
            case result of
                Ok () ->
                    ( { model
                        | accountState = ChangeCompleted
                      }
                    , Cmd.none
                    )

                Err _ ->
                    ( { model
                        | accountState = ChangeFailed
                      }
                    , Cmd.none
                    )

        AccountInfoChanged newAccountInfo ->
            ( { model
                | accountInfo = newAccountInfo
              }
            , Cmd.none
            )


changePassword : Model -> ( Model, Cmd Msg )
changePassword model =
    case model.accountState of
        EnteringPassword { newPassword, newPasswordRepeat, oldPassword } ->
            ( { model
                | accountState = ChangeInProgress
              }
            , changeRequest ( { newPassword = newPassword, newPasswordRepeat = newPasswordRepeat, oldPassword = oldPassword }, model )
            )

        _ ->
            ( model, Cmd.none )


changeRequest : ( PasswordList, Model ) -> Cmd Msg
changeRequest ( { newPassword, oldPassword }, model ) =
    Http.post
        { url = "./rest/users/changePassword"
        , body =
            Http.jsonBody <|
                Encode.object
                    [ ( "username", Encode.string model.accountInfo.email )
                    , ( "oldPassword", Encode.string oldPassword )
                    , ( "newPassword", Encode.string newPassword )
                    ]
        , expect = Http.expectWhatever ChangeRequestReturned
        }


updatePasswordList : PasswordList -> Model -> ( Model, Cmd Msg )
updatePasswordList newPasswordList model =
    ( case model.accountState of
        EnteringPassword _ ->
            { model
                | accountState = EnteringPassword newPasswordList
            }

        _ ->
            model
    , Cmd.none
    )


updateNewPassword : String -> Model -> ( Model, Cmd Msg )
updateNewPassword newEnteredPassword model =
    ( case model.accountState of
        EnteringPassword _ ->
            let
                newPasswordList =
                    { newPassword = newEnteredPassword
                    , newPasswordRepeat = model.passwordList.newPasswordRepeat
                    , oldPassword = model.passwordList.oldPassword
                    }
            in
            { model
                | accountState = EnteringPassword newPasswordList
                , passwordList = newPasswordList
            }

        _ ->
            model
    , Cmd.none
    )


updateNewPasswordRepeat : String -> Model -> ( Model, Cmd Msg )
updateNewPasswordRepeat newEnteredrepeatPassword model =
    ( case model.accountState of
        EnteringPassword _ ->
            let
                newPasswordList =
                    { newPassword = model.passwordList.newPassword
                    , newPasswordRepeat = newEnteredrepeatPassword
                    , oldPassword = model.passwordList.oldPassword
                    }
            in
            { model
                | accountState = EnteringPassword newPasswordList
                , passwordList = newPasswordList
            }

        _ ->
            model
    , Cmd.none
    )


updateOldPassword : String -> Model -> ( Model, Cmd Msg )
updateOldPassword oldEnteredPassword model =
    ( case model.accountState of
        EnteringPassword _ ->
            let
                newPasswordList =
                    { newPassword = model.passwordList.newPassword
                    , newPasswordRepeat = model.passwordList.newPasswordRepeat
                    , oldPassword = oldEnteredPassword
                    }
            in
            { model
                | accountState = EnteringPassword newPasswordList
                , passwordList = newPasswordList
            }

        _ ->
            model
    , Cmd.none
    )



---- VIEW ----


changeView : Skin -> Model -> Element Msg
changeView skin model =
    case model.accountState of
        EnteringPassword { oldPassword, newPassword, newPasswordRepeat } ->
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
                        [ text "Change your password here."
                        ]
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
                        { onChange = OldPasswordChanged
                        , text = oldPassword
                        , placeholder = Nothing
                        , label =
                            Input.labelLeft
                                [ centerY
                                , Font.size 14
                                , paddingEach
                                    { left = 0
                                    , top = 0
                                    , bottom = 0
                                    , right = 39
                                    }
                                ]
                                (text "Current:")
                        , show = False
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
                        , text = newPassword
                        , placeholder = Nothing
                        , label =
                            Input.labelLeft
                                [ centerY
                                , Font.size 14
                                , paddingEach
                                    { left = 0
                                    , top = 0
                                    , bottom = 0
                                    , right = 60
                                    }
                                ]
                                (text "New:")
                        , show = False
                        }
                    ]
                , if String.length newPassword > 0 && String.length newPassword < 6 then
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
                    [ Input.newPassword [ width fill ]
                        { onChange = NewPasswordRepeatChanged
                        , text = newPasswordRepeat
                        , placeholder = Nothing
                        , label =
                            Input.labelLeft
                                [ centerY
                                , Font.size 14
                                , paddingEach
                                    { left = 0
                                    , top = 0
                                    , bottom = 0
                                    , right = 9
                                    }
                                ]
                                (text "Re-type new:")
                        , show = False
                        }
                    ]
                , if String.length newPasswordRepeat > 0 && newPassword /= newPasswordRepeat then
                    row [ spacing 10 ]
                        [ el
                            [ width fill
                            , Font.color skin.primaryColor
                            , Font.size 10
                            ]
                          <|
                            text "Password is not equal"
                        ]

                  else
                    none
                , row
                    [ width fill
                    , spacing 20
                    ]
                    [ Elements.mainButton
                        skin
                        [ width <| maximum 320 fill
                        , padding 5
                        ]
                        { onPress =
                            if String.isEmpty oldPassword then
                                Nothing

                            else if String.isEmpty newPassword then
                                Nothing

                            else if String.length newPassword < 6 then
                                Nothing

                            else if String.isEmpty newPasswordRepeat then
                                Nothing

                            else if newPassword /= newPasswordRepeat then
                                Nothing

                            else
                                Just ChangeButtonPressed
                        , label =
                            el
                                [ centerX
                                ]
                            <|
                                if String.length newPassword < 1 then
                                    text "Save Changes"

                                else if String.length newPassword < 6 then
                                    text "New password to short"

                                else if newPassword /= newPasswordRepeat then
                                    text "Do not match"

                                else
                                    text "Save Changes"
                        }
                    ]
                ]

        ChangeInProgress ->
            el
                [ width fill
                , Background.color skin.lineGray
                , Font.color Skin.white
                , padding 20
                ]
            <|
                text "Changing Password"

        ChangeCompleted ->
            el
                [ width fill
                , Background.color skin.lineGray
                , Font.color Skin.white
                , padding 20
                ]
            <|
                text "Password changed"

        ChangeFailed ->
            el
                [ width fill
                , Background.color skin.primaryColor
                , Font.color Skin.white
                , padding 20
                ]
            <|
                text "Change failed"


view : Skin -> Model -> Element Msg
view skin model =
    column
        [ width fill
        , spacing 20
        , Font.size 16
        ]
        [ el [ Font.size 24 ] <| text "Account Settings"
        , paragraph [ width fill ]
            [ text "Here you can change your account data."
            ]
        , if model.accountInfo.isVerified then
            changeView skin model

          else
            paragraph [ width fill ]
                [ text "You need an account with a verified email address to export your data" ]
        ]



---- SUBSCRIPTIONS ----


port getAccountInfo : () -> Cmd msg


port accountInfoIn : (AccountInfo -> msg) -> Sub msg


subscriptions : Sub Msg
subscriptions =
    accountInfoIn AccountInfoChanged
