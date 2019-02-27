module Palette exposing
    ( DropdownDirection(..)
    , button
    , dropdown
    , identicon
    )

import Color
import Color.Manipulate as Color
import Element.Background.Styled as Background
import Element.Border.Styled as Border
import Element.Font.Styled as Font
import Element.Input.Styled as Input
import Element.Styled as Element exposing (..)
import Html
import Html.Attributes as HA
import Icons


scaled =
    modular 16 1.25


borderRadius =
    3


borderColor =
    Color.gray


hoverColor color =
    Color.weightedMix Color.black color 0.1


activeColor =
    Color.fadeOut 0.8


button :
    { label : String
    , onPress : Maybe msg
    }
    -> Element msg
button { label, onPress } =
    withStyle <|
        \style ->
            Input.button
                [ width fill
                , Background.color style.primary
                , Border.rounded borderRadius
                , focused
                    [ Border.shadow
                        { offset = ( 0, 0 )
                        , size = 2
                        , blur = 0
                        , color = activeColor style.primary
                        }
                    ]
                ]
                { label =
                    el
                        [ centerX
                        , Font.color Color.white
                        , Font.size <|
                            round <|
                                scaled 1
                        , padding <| round <| scaled 1 / 2
                        ]
                    <|
                        text label
                , onPress = onPress
                }


type DropdownDirection
    = DropDown
    | DropUp


dropdown :
    { direction : DropdownDirection
    , menu : Maybe (Element msg)
    , label : String
    , onToggle : Maybe msg
    }
    -> Element msg
dropdown { direction, menu, label, onToggle } =
    withStyle <|
        \style ->
            let
                dropattr =
                    case direction of
                        DropDown ->
                            below

                        DropUp ->
                            above

                ( openArrow, closeArrow ) =
                    case direction of
                        DropDown ->
                            ( Icons.arrowDown, Icons.arrowUp )

                        DropUp ->
                            ( Icons.arrowUp, Icons.arrowDown )

                dropdownMenu subMenu =
                    el
                        [ case direction of
                            DropDown ->
                                moveDown 3

                            DropUp ->
                                moveUp 3
                        , Element.width fill
                        , Background.color Color.white
                        , Border.color borderColor
                        , Border.width 1
                        ]
                        subMenu

                ( attrs, open ) =
                    case menu of
                        Just menu_ ->
                            ( [ dropattr <| dropdownMenu menu_ ], True )

                        Nothing ->
                            ( [], False )

                enabled =
                    case onToggle of
                        Just _ ->
                            True

                        Nothing ->
                            False

                mainColor =
                    if enabled then
                        style.primary

                    else
                        Color.gray
            in
            el
                ([ Element.width fill
                 ]
                    ++ attrs
                )
            <|
                Input.button
                    [ Background.color mainColor
                    , Border.rounded borderRadius
                    , Element.width fill
                    , mouseOver <|
                        if enabled then
                            [ Background.color <| hoverColor mainColor ]

                        else
                            []
                    , focused
                        [ Border.shadow
                            { offset = ( 0, 0 )
                            , size = 2
                            , blur = 0
                            , color = activeColor mainColor
                            }
                        ]
                    ]
                    { label =
                        row
                            [ padding <| round <| scaled 1 / 2
                            , Font.size <|
                                round <|
                                    scaled 1
                            , Element.width fill
                            , Font.color Color.white
                            ]
                            [ el [ centerX ] <| text label
                            , el
                                [ alignRight
                                , Element.width <| px (round <| scaled 1)
                                , height <| px (round <| scaled 1)
                                ]
                              <|
                                if open then
                                    closeArrow Color.white

                                else
                                    openArrow Color.white
                            ]
                    , onPress = onToggle
                    }


identicon : Int -> String -> Element msg
identicon size string =
    el
        [ width <| px <| round <| scaled size
        , height <| px <| round <| scaled size
        ]
    <|
        html <|
            Html.node "won-identicon"
                [ HA.attribute "data" string
                , HA.style "width" "100%"
                , HA.style "height" "100%"
                ]
                []
