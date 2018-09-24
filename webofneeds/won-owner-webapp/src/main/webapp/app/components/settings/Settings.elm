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



-- State


type alias EditingModel =
    { id : Int
    , form : IdentityForm
    }


type alias EditingInfo =
    { id : Int
    , original : Identity
    , form : IdentityForm
    }


editingInfo : Model -> Maybe EditingInfo
editingInfo model =
    let
        editedIdentity =
            model.editing
                |> Maybe.andThen (\{ id } -> Array.get id model.identities)
    in
    Maybe.map2
        (\{ id, form } original ->
            { id = id
            , form = form
            , original = original
            }
        )
        model.editing
        editedIdentity


modified : EditingInfo -> Bool
modified { form, original } =
    toForm original /= form


type alias Model =
    { identities : Array Identity
    , editing : Maybe EditingModel
    }


type Msg
    = SelectIdentity Int
    | SaveIdentity
    | CancelEditing
    | EditIdentity IdentityForm



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
    , editing = Nothing
    }



--
-- Update
--


update : Msg -> Model -> Model
update msg model =
    case msg of
        SelectIdentity id ->
            let
                edited =
                    Maybe.map modified (editingInfo model) == Just True
            in
            Array.get id model.identities
                |> Maybe.andThen
                    (\original ->
                        if edited then
                            Nothing

                        else
                            Just
                                { model
                                    | editing =
                                        Just
                                            { id = id
                                            , form = toForm original
                                            }
                                }
                    )
                |> Maybe.withDefault model

        CancelEditing ->
            { model | editing = Nothing }

        SaveIdentity ->
            case editingInfo model of
                Just { id, form } ->
                    case Validate.validate identityValidator form of
                        Ok valid ->
                            { model
                                | identities = Array.set id (fromForm valid) model.identities
                            }

                        Err _ ->
                            model

                Nothing ->
                    model

        EditIdentity newForm ->
            case model.editing of
                Just { id, form } ->
                    { model
                        | editing =
                            Just
                                { id = id
                                , form = newForm
                                }
                    }

                Nothing ->
                    model



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
        , Font.color color
        , Font.size 18
        ]
        [ svgIcon
            [ height (px 18)
            , width (px 18)
            ]
            icon
            (cssColor color)
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
        , Background.color skin.lineGray
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
                    [ Font.color skin.subtitleGray
                    ]
                  <|
                    text ("Name: " ++ identity.displayName)
                ]
            ]
        )
        []


identityEditor : EditingInfo -> Element Msg
identityEditor info =
    let
        form =
            info.form

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
        (row
            [ width fill
            , spacing 10
            ]
            [ identityImage info.original
            , Input.text [ alignTop ]
                { onChange = \str -> EditIdentity { form | description = str }
                , text = form.description
                , placeholder = Just (Input.placeholder [] <| text "Unnamed Identity")
                , label =
                    Input.labelAbove
                        [ width (px 0)
                        , height (px 0)
                        , htmlAttribute (HA.style "display" "none")
                        ]
                        (text "Display Name")
                }
            ]
        )
        [ identityForm form
        , column
            [ width fill
            , spacing 10
            ]
            [ if isValid then
                none

              else
                column
                    [ Font.color skin.primaryColor
                    ]
                    (List.map
                        text
                        errors
                    )
            , row
                [ spacing 10
                , width fill
                ]
                [ mainButton (not isValid || not (modified info)) "Save" SaveIdentity
                , outlinedButton False "Cancel" CancelEditing
                ]
            ]
        ]


identityForm : IdentityForm -> Element Msg
identityForm form =
    column
        [ spacing 10
        , width fill
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
            case editingInfo model of
                Just info ->
                    Array.set info.id (identityEditor info) cards

                Nothing ->
                    cards
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
    { primaryColor : Color
    , lightGray : Color
    , lineGray : Color
    , subtitleGray : Color
    }


skin : Skin
skin =
    { primaryColor = rgb255 240 70 70
    , lightGray = rgb255 240 242 244
    , lineGray = rgb255 203 210 209
    , subtitleGray = rgb255 128 128 128
    }


cssColor : Color -> String
cssColor color =
    let
        { red, green, blue, alpha } =
            toRgb color

        to255 col =
            String.fromInt <| round (col * 255)

        colors =
            ([ red
             , green
             , blue
             ]
                |> List.map to255
            )
                ++ [ String.fromFloat alpha ]
    in
    "rgba("
        ++ String.join "," colors
        ++ ")"


black : Color
black =
    rgb 0 0 0


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
