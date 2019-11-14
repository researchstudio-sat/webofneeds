package won.protocol.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WonUriCheckHelper {
    private static Pattern PATTERN_CONNECTION = Pattern.compile("/[a-zA-Z0-9]+/c/[a-zA-Z0-9]+");
    private static Pattern PATTERN_SOCKET = Pattern.compile("/[a-zA-Z0-9]+#[a-zA-Z0-9]+");

    public static boolean isValidConnectionURI(String atomURIPrefix, String connectionUriToCheck) {
        Objects.requireNonNull(atomURIPrefix);
        Objects.requireNonNull(connectionUriToCheck);
        if (!connectionUriToCheck.startsWith(atomURIPrefix)) {
            return false;
        }
        String rest = connectionUriToCheck.substring(atomURIPrefix.length());
        Matcher m = PATTERN_CONNECTION.matcher(rest);
        return m.matches();
    }

    public static boolean isValidSocketURI(String atomURIPrefix, String socketUriToCheck) {
        Objects.requireNonNull(atomURIPrefix);
        Objects.requireNonNull(socketUriToCheck);
        if (!socketUriToCheck.startsWith(atomURIPrefix)) {
            return false;
        }
        String rest = socketUriToCheck.substring(atomURIPrefix.length());
        Matcher m = PATTERN_SOCKET.matcher(rest);
        return m.matches();
    }
}
