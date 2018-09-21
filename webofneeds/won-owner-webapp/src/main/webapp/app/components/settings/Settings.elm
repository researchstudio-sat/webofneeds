module Main exposing (main)

import Array exposing (Array)
import Browser
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events as Events
import Element.Font as Font
import Element.Input as Input
import Html exposing (Html, node)
import Html.Attributes as HA
import String.Extra as String
import Validate exposing (Valid, Validator)


type IdentityEditor
    = NotEditing
    | Editing Int TempIdentity
    | Unchanged Int


type alias Model =
    { identities : Array Identity
    , editingIdentity : IdentityEditor
    }


type alias TempIdentity =
    { description : String
    , displayName : String
    , image : String
    , website : String
    , aboutMe : String
    }


type alias Identity =
    { description : Maybe String
    , displayName : String
    , image : Maybe String
    , website : Maybe String
    , aboutMe : Maybe String
    }


mockIdentities : List Identity
mockIdentities =
    [ { description = Nothing
      , displayName = "Test"
      , image = Nothing
      , aboutMe = Nothing
      , website = Nothing
      }
    , { description = Just "my id"
      , displayName = "John"
      , image = Just "https://foxrudor.de/"
      , aboutMe = Nothing
      , website = Nothing
      }
    ]


init : Model
init =
    { identities = Array.fromList mockIdentities
    , editingIdentity = NotEditing
    }


type Msg
    = SelectIdentity Int
    | SaveIdentity
    | CancelEditing
    | EditIdentity TempIdentity


update : Msg -> Model -> Model
update msg model =
    case msg of
        SelectIdentity id ->
            case model.editingIdentity of
                NotEditing ->
                    { model | editingIdentity = Unchanged id }

                Unchanged _ ->
                    { model | editingIdentity = Unchanged id }

                Editing _ _ ->
                    model

        CancelEditing ->
            { model | editingIdentity = NotEditing }

        SaveIdentity ->
            case model.editingIdentity of
                Editing id tempIdentity ->
                    case Validate.validate identityValidator tempIdentity of
                        Ok valid ->
                            { model
                                | identities = Array.set id (toIdentity valid) model.identities
                                , editingIdentity = NotEditing
                            }

                        Err _ ->
                            model

                NotEditing ->
                    model

                Unchanged _ ->
                    { model
                        | editingIdentity = NotEditing
                    }

        EditIdentity tempIdentity ->
            case model.editingIdentity of
                Editing id _ ->
                    { model | editingIdentity = Editing id tempIdentity }

                NotEditing ->
                    model

                Unchanged id ->
                    { model | editingIdentity = Editing id tempIdentity }


identityValidator : Validator String TempIdentity
identityValidator =
    Validate.all
        [ Validate.ifBlank .displayName "Please enter a display name."
        ]


toTempIdentity : Identity -> TempIdentity
toTempIdentity identity =
    { description = Maybe.withDefault "" identity.description
    , displayName = identity.displayName
    , image = Maybe.withDefault "" identity.image
    , website = Maybe.withDefault "" identity.website
    , aboutMe = Maybe.withDefault "" identity.aboutMe
    }


toIdentity : Valid TempIdentity -> Identity
toIdentity valid =
    let
        tempIdentity =
            Validate.fromValid valid
    in
    { description = String.nonEmpty tempIdentity.description
    , displayName = tempIdentity.displayName
    , image = String.nonEmpty tempIdentity.image
    , website = String.nonEmpty tempIdentity.website
    , aboutMe = String.nonEmpty tempIdentity.aboutMe
    }


type alias Col =
    ( Int, Int, Int )


toColor : Col -> Color
toColor ( r, g, b ) =
    rgb255 r g b


toCSSColor : Col -> String
toCSSColor ( r, g, b ) =
    "rgb(" ++ String.fromInt r ++ ", " ++ String.fromInt g ++ ", " ++ String.fromInt b ++ ")"


primaryColor : Col
primaryColor =
    ( 240, 70, 70 )


lightGray : Col
lightGray =
    ( 203, 210, 209 )


black : Col
black =
    ( 0, 0, 0 )


svgIcon : String -> String -> Element msg
svgIcon name color =
    el
        [ width (px 20)
        , height (px 20)
        ]
    <|
        html <|
            node "svg-icon"
                [ HA.attribute "icon" name
                , HA.attribute "color" color
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []


category : Bool -> String -> String -> Element msg
category active icon name =
    let
        color =
            if active then
                primaryColor

            else
                black
    in
    row
        [ spacing 10
        , Font.color (toColor color)
        ]
        [ svgIcon icon (toCSSColor color)
        , text name
        ]


sidebar : Element msg
sidebar =
    column
        [ width shrink
        , alignTop
        ]
        [ category True "ico36_person" "Identities" ]


identityCard : Identity -> Element msg
identityCard id =
    row
        [ Border.width 1
        , Border.color (toColor lightGray)
        , padding 10
        , spacing 5
        , width fill
        ]
        ([ column
            [ height fill
            , width fill
            ]
           <|
            case id.description of
                Just description ->
                    [ el [ height fill ] <| none
                    , el [ Font.size 30 ] <| text description
                    , el [ height fill ] <| none
                    , text id.displayName
                    , el [ height fill ] <| none
                    ]

                Nothing ->
                    [ el [ Font.size 30, centerY ] <| text id.displayName
                    ]
         ]
            ++ (case id.image of
                    Just img ->
                        [ image
                            [ width (shrink |> maximum 100)
                            , height (shrink |> maximum 100)
                            ]
                            { src = img
                            , description = ""
                            }
                        ]

                    Nothing ->
                        []
               )
        )


mainButton : Bool -> String -> msg -> Element msg
mainButton disabled text tag =
    el [ Events.onClick tag ] <|
        html <|
            Html.button
                [ HA.classList
                    [ ( "won-button--filled", True )
                    , ( "red", True )
                    ]
                , HA.disabled disabled
                ]
                [ Html.text text ]


outlinedButton : Bool -> String -> msg -> Element msg
outlinedButton disabled text tag =
    el [ Events.onClick tag ] <|
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


identityEditor : Bool -> TempIdentity -> Element Msg
identityEditor modified tempIdentity =
    let
        validated =
            Validate.validate identityValidator tempIdentity

        ( isValid, errors ) =
            case validated of
                Ok _ ->
                    ( True, [] )

                Err err ->
                    ( False, err )
    in
    textColumn
        [ Border.width 1
        , Border.color (toColor lightGray)
        , padding 10
        , spacing 10
        , width fill
        ]
        [ image
            [ width (shrink |> maximum 200)
            , alignRight
            ]
            { src = tempIdentity.image
            , description = ""
            }
        , paragraph []
            [ Input.text []
                { onChange = \str -> EditIdentity { tempIdentity | description = str }
                , text = tempIdentity.description
                , placeholder = Nothing
                , label = Input.labelAbove [] (text "Description")
                }
            ]
        , paragraph []
            [ Input.text []
                { onChange = \str -> EditIdentity { tempIdentity | displayName = str }
                , text = tempIdentity.displayName
                , placeholder = Nothing
                , label = Input.labelAbove [] (text "Display Name")
                }
            ]
        , paragraph []
            [ Input.text []
                { onChange = \str -> EditIdentity { tempIdentity | website = str }
                , text = tempIdentity.website
                , placeholder = Nothing
                , label = Input.labelAbove [] (text "Website")
                }
            ]
        , paragraph []
            [ el [ width fill ] <|
                Input.multiline []
                    { onChange = \str -> EditIdentity { tempIdentity | aboutMe = str }
                    , text = tempIdentity.aboutMe
                    , placeholder = Nothing
                    , label = Input.labelAbove [] (text "About Me")
                    , spellcheck = True
                    }
            ]
        , paragraph
            [ Font.color (toColor primaryColor)
            , Font.size 14
            ]
            (List.map
                text
                errors
            )
        , paragraph []
            [ row
                [ alignBottom
                , spacing 10
                , alignRight
                ]
                [ mainButton (not isValid || not modified) "Save" SaveIdentity
                , el [ width fill ] none
                , outlinedButton (not modified) "Cancel" CancelEditing
                ]
            ]
        ]


identitySettings : Model -> Element Msg
identitySettings model =
    let
        cards =
            Array.indexedMap
                (\id identity ->
                    el [ Events.onClick (SelectIdentity id), width fill ] (identityCard identity)
                )
                model.identities

        editingCards =
            case model.editingIdentity of
                Editing id tempIdentity ->
                    Array.set id (identityEditor True tempIdentity) cards

                NotEditing ->
                    cards

                Unchanged id ->
                    Array.get id model.identities
                        |> Maybe.map toTempIdentity
                        |> Maybe.map
                            (\tempIdentity ->
                                Array.set id (identityEditor False tempIdentity) cards
                            )
                        |> Maybe.withDefault cards
    in
    column
        [ centerX
        , spacing 10
        , width
            (fill
                |> maximum 600
            )
        ]
    <|
        Array.toList editingCards
            ++ [-- Insert + button here
               ]


view : Model -> Html Msg
view model =
    Element.layout [] <|
        row
            [ centerX
            , width
                (fill
                    |> maximum 1000
                )
            , padding 20
            , spacing 20
            ]
            [ sidebar
            , identitySettings model
            ]


main =
    Browser.sandbox
        { init = init
        , update = update
        , view = view
        }
