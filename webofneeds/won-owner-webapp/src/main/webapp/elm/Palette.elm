module Palette exposing
    ( DropdownDirection(..)
    , dropdownButton
    )

import Color
import Element.Background.Styled as Background
import Element.Border.Styled as Border
import Element.Font.Styled as Font
import Element.Input.Styled as Input
import Element.Styled exposing (..)


type DropdownDirection
    = DropDown
    | DropUp


scaled =
    modular 16 1.25


buttonRadius =
    3


type ButtonRole
    = Primary
    | Secondary


type alias Button msg =
    { role : ButtonRole
    , enabled : Bool
    , onPress : msg
    , label : Element msg
    }


rawButton :
    { left : Bool
    , right : Bool
    }
    -> Button msg
    -> Element msg
rawButton { left, right } { role, enabled, onPress, label } =
    let
        enabledRadius hasRadius =
            if hasRadius then
                buttonRadius

            else
                0

        radius =
            { topLeft = enabledRadius left
            , bottomLeft = enabledRadius left
            , topRight = enabledRadius right
            , bottomRight = enabledRadius right
            }

        col color =
            if enabled then
                color

            else
                color
    in
    withStyle <|
        \style ->
            Debug.todo ""


dropdownButton :
    { direction : DropdownDirection
    , menu : Element msg
    , label : String
    , open : Bool
    , onPress : Maybe msg
    , toggleMsg : msg
    }
    -> Element msg
dropdownButton { direction, menu, label, open, toggleMsg, onPress } =
    let
        dropattr =
            case direction of
                DropDown ->
                    below

                DropUp ->
                    above

        defaultAttrs =
            [ width fill
            , spacing 3
            ]

        attrs =
            defaultAttrs
                ++ (if open then
                        [ dropattr menu ]

                    else
                        []
                   )
    in
    withStyle
        (\skin ->
            row
                attrs
                [ Input.button
                    [ Background.color skin.primary
                    , Border.roundEach
                        { topLeft = 5
                        , bottomLeft = 5
                        , topRight = 0
                        , bottomRight = 0
                        }
                    , width fill
                    ]
                    { label =
                        el
                            [ padding <| round <| scaled 1 / 2
                            , width fill
                            , Font.size <|
                                round <|
                                    scaled 1
                            , Font.color Color.white
                            , Font.center
                            ]
                        <|
                            text label
                    , onPress = onPress
                    }
                , Input.button
                    [ Background.color skin.primary
                    , Border.roundEach
                        { topLeft = 0
                        , bottomLeft = 0
                        , topRight = 5
                        , bottomRight = 5
                        }
                    ]
                    { label = none
                    , onPress = Just toggleMsg
                    }
                ]
        )
