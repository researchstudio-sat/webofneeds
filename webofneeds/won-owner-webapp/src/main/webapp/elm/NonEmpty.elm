module NonEmpty exposing
    ( NonEmpty
    , get
    , string
    , stringDecoder
    )

import Json.Decode as Decode exposing (Decoder)


type NonEmpty a
    = NonEmpty a


string : String -> Maybe (NonEmpty String)
string str =
    if String.isEmpty str then
        Nothing

    else
        Just (NonEmpty str)


stringDecoder : Decoder (NonEmpty String)
stringDecoder =
    Decode.string
        |> Decode.andThen
            (\string_ ->
                case string string_ of
                    Just nonEmpty ->
                        Decode.succeed nonEmpty

                    Nothing ->
                        Decode.fail "String may not be empty"
            )


get : NonEmpty a -> a
get (NonEmpty a) =
    a
