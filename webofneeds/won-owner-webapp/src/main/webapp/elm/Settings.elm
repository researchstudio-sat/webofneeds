module Settings exposing (main)

import Element exposing (layout)
import Html exposing (Html)
import Settings.Account as Account
import Settings.Personas as Personas
import Skin exposing (Skin)


main =
    Skin.skinnedElement
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }



---- MODEL ----


type Model
    = Personas Personas.Model
    | Account Account.Model


init : () -> ( Model, Cmd Msg )
init () =
    let
        ( model, cmd ) =
            Account.init ()
    in
    ( Account model, Cmd.map AccountMsg cmd )



---- UPDATE ----


type Msg
    = PersonasMsg Personas.Msg
    | AccountMsg Account.Msg


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case ( msg, model ) of
        ( PersonasMsg subMsg, Personas subModel ) ->
            Personas.update subMsg subModel
                |> updateWith Personas PersonasMsg model

        ( AccountMsg subMsg, Account subModel ) ->
            Account.update subMsg subModel
                |> updateWith Account AccountMsg model

        ( _, _ ) ->
            ( model, Cmd.none )


updateWith : (subModel -> Model) -> (subMsg -> Msg) -> Model -> ( subModel, Cmd subMsg ) -> ( Model, Cmd Msg )
updateWith toModel toMsg model ( subModel, subCmd ) =
    ( toModel subModel
    , Cmd.map toMsg subCmd
    )



---- VIEW ----


view : Skin -> Model -> Html Msg
view skin model =
    layout [] <|
        let
            viewPage toMsg viewFunc viewModel =
                Element.map toMsg (viewFunc skin viewModel)
        in
        case model of
            Personas subModel ->
                viewPage PersonasMsg Personas.view subModel

            Account subModel ->
                viewPage AccountMsg Account.view subModel



---- SUBSCRIPTIONS ----


subscriptions : Model -> Sub Msg
subscriptions model =
    case model of
        Personas subModel ->
            Personas.subscriptions subModel
                |> Sub.map PersonasMsg

        Account _ ->
            Sub.none
