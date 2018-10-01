module Elements exposing
    ( mainButton
    , outlinedButton
    )

import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events as Events
import Html exposing (node)
import Html.Attributes as HA
import Skin exposing (Skin)


type alias Button msg =
    { disabled : Bool
    , text : String
    , onClick : msg
    }


mainButton : Button msg -> Element msg
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


outlinedButton : Button msg -> Element msg
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
