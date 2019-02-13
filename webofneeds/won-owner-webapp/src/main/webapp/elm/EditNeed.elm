module EditNeed exposing (main)

import Application exposing (State)
import Browser
import Element.Styled as Element exposing (Element, none)
import Json.Decode as JD exposing (Value)
import Palette exposing (..)


type alias Attributes =
    String


type alias Model =
    {}


type Msg
    = Msg


init : Attributes -> State -> ( Model, Cmd Msg )
init attrs state =
    let
        log =
            Debug.log "state" state
    in
    ( {}, Cmd.none )


update : Attributes -> State -> Msg -> Model -> ( Model, Cmd Msg )
update attrs state msg model =
    ( model, Cmd.none )


view : Attributes -> State -> Model -> Element Msg
view _ _ _ =
    dropdownButton
        { direction = DropDown
        , label = "Add Persona"
        , menu = none
        , open = False
        , toggleMsg = Msg
        , onPress = Nothing
        }


attributeParser : JD.Decoder Attributes
attributeParser =
    JD.string


main =
    Application.element
        { init = init
        , update = update
        , view = view
        , subscriptions = \_ _ _ -> Sub.none
        , attributeParser = attributeParser
        }
