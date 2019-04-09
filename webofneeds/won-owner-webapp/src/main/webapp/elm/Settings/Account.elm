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
import Element.Font as Font
import Element.Input as Input
import Elements
import Http
import Json.Encode as Encode
import Old.Skin as Skin exposing (Skin)


type alias Model =
    { accountState : AccountState
    , isVerifiedAccount : Bool
    , username : String
    , passwordList : PasswordList
    }


type alias PasswordList =
    { newPassword : String
    , oldPassword : String
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
    ( { isVerifiedAccount = False
      , username = ""
      , accountState = EnteringPassword { newPassword = "", oldPassword = "" }
      , passwordList = { newPassword = "", oldPassword = "" }
      }
    , Cmd.batch
        [ getVerifiedAccount ()
        , getAccountInfo ()
        ]
    )



---- UPDATE ----


type Msg
    = PasswordChanged PasswordList
    | OldPasswordChanged String
    | NewPasswordChanged String
    | VerificationStatusChanged Bool
    | AccountInfoChanged String
    | ChangeButtonPressed
    | ChangeRequestReturned (Result Http.Error ())


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        PasswordChanged newPasswordList ->
            updatePasswordList newPasswordList model

        NewPasswordChanged newPwd ->
            updateNewPassword newPwd model

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

        VerificationStatusChanged newStatus ->
            ( { model
                | isVerifiedAccount = newStatus
              }
            , Cmd.none
            )

        AccountInfoChanged newUsername ->
            ( { model
                | username = newUsername
              }
            , Cmd.none
            )


changePassword : Model -> ( Model, Cmd Msg )
changePassword model =
    case model.accountState of
        EnteringPassword { newPassword, oldPassword } ->
            ( { model
                | accountState = ChangeInProgress
              }
            , changeRequest ( { newPassword = newPassword, oldPassword = oldPassword }, model )
            )

        _ ->
            ( model, Cmd.none )



{- username: email,
   oldPassword: oldPassword,
   newPassword: newPassword,
-}


changeRequest : ( PasswordList, Model ) -> Cmd Msg
changeRequest ( { newPassword, oldPassword }, model ) =
    Http.post
        { url = "./rest/users/changePassword"
        , body =
            Http.jsonBody <|
                Encode.object
                    [ ( "username", Encode.string model.username )
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
        EnteringPassword { oldPassword, newPassword } ->
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
                    [ Input.newPassword [ width fill ]
                        { onChange = OldPasswordChanged
                        , text = oldPassword
                        , placeholder = Nothing
                        , label =
                            Input.labelLeft
                                [ centerY
                                , paddingEach
                                    { left = 9
                                    , top = 0
                                    , bottom = 0
                                    , right = 10
                                    }
                                ]
                                (text "Old:")
                        , show = False
                        }
                    ]
                , row
                    [ spacing 10
                    ]
                    [ Input.newPassword [ width fill ]
                        { onChange = NewPasswordChanged
                        , text = newPassword
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
                                (text "New:")
                        , show = False
                        }
                    , Elements.mainButton skin
                        [ height fill ]
                        { onPress =
                            if String.isEmpty oldPassword then
                                Nothing

                            else if String.isEmpty newPassword then
                                Nothing

                            else
                                Just ChangeButtonPressed
                        , label = text "Change"
                        }
                    ]
                ]

        ChangeInProgress ->
            el
                [ width fill
                , Background.color skin.lightGray
                , Font.color Skin.white
                , padding 20
                ]
            <|
                text "Changing Password"

        ChangeCompleted ->
            el
                [ width fill
                , Background.color skin.lightGray
                , Font.color Skin.white
                , padding 20
                ]
            <|
                text "Change completed"

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
        , if model.isVerifiedAccount then
            changeView skin model

          else
            changeView skin model
        ]



{- DEBUG
   paragraph [ width fill ]
       [ text "You need an account with a verified email address to export your data" ]
-}
---- SUBSCRIPTIONS ----


port isVerifiedAccount : (Bool -> msg) -> Sub msg


port getVerifiedAccount : () -> Cmd msg


port getAccountInfo : () -> Cmd msg


port accountInfoIn : (String -> msg) -> Sub msg


subscriptions : Sub Msg
subscriptions =
    Sub.batch
        [ isVerifiedAccount VerificationStatusChanged
        , accountInfoIn AccountInfoChanged
        ]



-- subscriptions =
