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


main =
    Browser.sandbox
        { init = init
        , update = update
        , view = view
        }



--
-- Model
--


type IdentityEditor
    = NotEditing
    | Editing Int IdentityForm
    | Unchanged Int


type alias Model =
    { identities : Array Identity
    , editingIdentity : IdentityEditor
    }


type Msg
    = SelectIdentity Int
    | SaveIdentity
    | CancelEditing
    | EditIdentity IdentityForm



-- Identity


type alias IdentityForm =
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


identityValidator : Validator String IdentityForm
identityValidator =
    Validate.all
        [ Validate.ifBlank .displayName "Please enter a display name."
        ]


toForm : Identity -> IdentityForm
toForm identity =
    { description = Maybe.withDefault "" identity.description
    , displayName = identity.displayName
    , image = Maybe.withDefault "" identity.image
    , website = Maybe.withDefault "" identity.website
    , aboutMe = Maybe.withDefault "" identity.aboutMe
    }


fromForm : Valid IdentityForm -> Identity
fromForm valid =
    let
        form =
            Validate.fromValid valid
    in
    { description = String.nonEmpty form.description
    , displayName = form.displayName
    , image = String.nonEmpty form.image
    , website = String.nonEmpty form.website
    , aboutMe = String.nonEmpty form.aboutMe
    }



--
-- Init
--


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



--
-- Update
--


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
                Editing id form ->
                    case Validate.validate identityValidator form of
                        Ok valid ->
                            { model
                                | identities = Array.set id (fromForm valid) model.identities
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

        EditIdentity form ->
            case model.editingIdentity of
                Editing id _ ->
                    { model | editingIdentity = Editing id form }

                NotEditing ->
                    model

                Unchanged id ->
                    { model | editingIdentity = Editing id form }



--
-- View
--


category : Bool -> String -> String -> Element msg
category active icon name =
    let
        color =
            if active then
                skin.primaryColor

            else
                black
    in
    row
        [ spacing 10
        , Font.color (toColor color)
        , Font.size 18
        ]
        [ svgIcon
            [ height (px 18)
            , width (px 18)
            ]
            icon
            (toCSSColor color)
        , text name
        ]


sidebar : Element msg
sidebar =
    column
        [ width shrink
        , alignTop
        ]
        [ category True "ico36_person" "Identities" ]


identityImage : Identity -> Element msg
identityImage identity =
    el
        [ width (px 50)
        , height (px 50)
        , Background.color (toColor skin.lineGray)
        ]
    <|
        case identity.image of
            Just img ->
                el
                    [ Background.uncropped img
                    , width fill
                    , height fill
                    ]
                    none

            Nothing ->
                identicon identity


identityCard : Identity -> Element msg
identityCard identity =
    card [ width fill ]
        (row
            [ spacing 10
            , width fill
            ]
            [ identityImage identity
            , column
                [ height fill
                ]
                [ el [ Font.size 18 ] <|
                    case identity.description of
                        Just description ->
                            text description

                        Nothing ->
                            el [ Font.italic ] <| text "Unnamed Identity"
                , el [ height fill ] none
                , el
                    [ Font.color (toColor skin.subtitleGray)
                    ]
                  <|
                    text ("Name: " ++ identity.displayName)
                ]
            ]
        )
        []


identityEditor : Bool -> IdentityForm -> Element Msg
identityEditor modified form =
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
    column
        [ spacing -1
        , width fill
        ]
        [ identityForm form
        , column
            [ width fill
            , Border.width 1
            , Border.color (toColor skin.lineGray)
            , padding 10
            , spacing 10
            ]
            [ column
                [ Font.color (toColor skin.primaryColor)
                , Font.size 14
                ]
                (List.map
                    text
                    errors
                )
            , row
                [ spacing 10
                , width fill
                ]
                [ mainButton (not isValid || not modified) "Save" SaveIdentity
                , outlinedButton False "Cancel" CancelEditing
                ]
            ]
        ]


identityForm : IdentityForm -> Element Msg
identityForm form =
    column
        [ padding 10
        , spacing 10
        , width fill
        , Border.width 1
        , Border.color (toColor skin.lineGray)
        ]
        [ Input.text []
            { onChange = \str -> EditIdentity { form | displayName = str }
            , text = form.displayName
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "Display Name")
            }
        , Input.text []
            { onChange = \str -> EditIdentity { form | website = str }
            , text = form.website
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "Website")
            }
        , Input.multiline []
            { onChange = \str -> EditIdentity { form | aboutMe = str }
            , text = form.aboutMe
            , placeholder = Nothing
            , label = Input.labelAbove [] (text "About Me")
            , spellcheck = True
            }
        ]


identitySettings : Model -> Element Msg
identitySettings model =
    let
        cards =
            Array.indexedMap
                (\id identity ->
                    el
                        [ Events.onClick (SelectIdentity id), width fill ]
                        (identityCard identity)
                )
                model.identities

        editingCards =
            case model.editingIdentity of
                Editing id form ->
                    updateArray
                        (\idCard ->
                            column
                                [ spacing -1
                                , width fill
                                ]
                                [ idCard
                                , identityEditor True form
                                ]
                        )
                        id
                        cards

                NotEditing ->
                    cards

                Unchanged id ->
                    Array.get id model.identities
                        |> Maybe.map toForm
                        |> Maybe.map
                            (\form ->
                                updateArray
                                    (\idCard ->
                                        column
                                            [ spacing -1
                                            , width fill
                                            ]
                                            [ idCard
                                            , identityEditor False form
                                            ]
                                    )
                                    id
                                    cards
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
            , Font.size 12
            ]
            [ sidebar
            , identitySettings model
            ]



-- Elements


card : List (Attribute msg) -> Element msg -> List (Element msg) -> Element msg
card attributes header body =
    let
        baseStyle =
            [ Border.width 1
            , Border.color (toColor skin.lineGray)
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
                ++ [ Background.color (toColor skin.lightGray) ]
            )
            header
         ]
            ++ List.map
                (\section ->
                    el baseStyle section
                )
                body
        )


identicon : Identity -> Element msg
identicon identity =
    el
        []
    <|
        html <|
            node "won-identicon"
                [ HA.attribute "data" (identityString identity)
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []


svgIcon : List (Attribute msg) -> String -> String -> Element msg
svgIcon attributes name color =
    el attributes <|
        html <|
            node "svg-icon"
                [ HA.attribute "icon" name
                , HA.attribute "color" color
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []


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



--
-- Utilities
--


type alias Skin =
    { primaryColor : Col
    , lightGray : Col
    , lineGray : Col
    , subtitleGray : Col
    }


type alias Col =
    ( Int, Int, Int )


skin : Skin
skin =
    { primaryColor = ( 240, 70, 70 )
    , lightGray = ( 240, 242, 244 )
    , lineGray = ( 203, 210, 209 )
    , subtitleGray = ( 128, 128, 128 )
    }


toColor : Col -> Color
toColor ( r, g, b ) =
    rgb255 r g b


toCSSColor : Col -> String
toCSSColor ( r, g, b ) =
    "rgb(" ++ String.fromInt r ++ ", " ++ String.fromInt g ++ ", " ++ String.fromInt b ++ ")"


black : Col
black =
    ( 0, 0, 0 )


identityString : Identity -> String
identityString identity =
    Maybe.withDefault "" identity.description
        ++ identity.displayName
        ++ Maybe.withDefault "" identity.website
        ++ Maybe.withDefault "" identity.aboutMe


updateArray : (a -> a) -> Int -> Array a -> Array a
updateArray fn id array =
    Array.get id array
        |> Maybe.map fn
        |> Maybe.map (\element -> Array.set id element array)
        |> Maybe.withDefault array
