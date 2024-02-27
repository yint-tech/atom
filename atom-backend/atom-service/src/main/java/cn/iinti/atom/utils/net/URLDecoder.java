package cn.iinti.atom.utils.net;


import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Objects;

/**
 * Utility class for HTML form decoding. This class contains static methods
 * for decoding a String from the <CODE>application/x-www-form-urlencoded</CODE>
 * MIME format.
 * <p>
 * The conversion process is the reverse of that used by the URLEncoder class. It is assumed
 * that all characters in the encoded string are one of the following:
 * &quot;{@code a}&quot; through &quot;{@code z}&quot;,
 * &quot;{@code A}&quot; through &quot;{@code Z}&quot;,
 * &quot;{@code 0}&quot; through &quot;{@code 9}&quot;, and
 * &quot;{@code -}&quot;, &quot;{@code _}&quot;,
 * &quot;{@code .}&quot;, and &quot;{@code *}&quot;. The
 * character &quot;{@code %}&quot; is allowed but is interpreted
 * as the start of a special escaped sequence.
 * <p>
 * The following rules are applied in the conversion:
 *
 * <ul>
 * <li>The alphanumeric characters &quot;{@code a}&quot; through
 *     &quot;{@code z}&quot;, &quot;{@code A}&quot; through
 *     &quot;{@code Z}&quot; and &quot;{@code 0}&quot;
 *     through &quot;{@code 9}&quot; remain the same.
 * <li>The special characters &quot;{@code .}&quot;,
 *     &quot;{@code -}&quot;, &quot;{@code *}&quot;, and
 *     &quot;{@code _}&quot; remain the same.
 * <li>The plus sign &quot;{@code +}&quot; is converted into a
 *     space character &quot; &nbsp; &quot; .
 * <li>A sequence of the form "<i>{@code %xy}</i>" will be
 *     treated as representing a byte where <i>xy</i> is the two-digit
 *     hexadecimal representation of the 8 bits. Then, all substrings
 *     that contain one or more of these byte sequences consecutively
 *     will be replaced by the character(s) whose encoding would result
 *     in those consecutive bytes.
 *     The encoding scheme used to decode these characters may be specified,
 *     or if unspecified, the default encoding of the platform will be used.
 * </ul>
 * <p>
 * There are two possible ways in which this decoder could deal with
 * illegal strings.  It could either leave illegal characters alone or
 * it could throw an {@link IllegalArgumentException}.
 * Which approach the decoder takes is left to the
 * implementation.
 *
 * @author Mark Chamness
 * @author Michael McCloskey
 * @since 1.2
 */

public class URLDecoder {

    // The platform default encoding
    static String dfltEncName = URLEncoder.dfltEncName;

    /**
     * Decodes a {@code x-www-form-urlencoded} string.
     * The platform's default encoding is used to determine what characters
     * are represented by any consecutive sequences of the form
     * "<i>{@code %xy}</i>".
     *
     * @param s the {@code String} to decode
     * @return the newly decoded {@code String}
     * @deprecated The resulting string may vary depending on the platform's
     * default encoding. Instead, use the decode(String,String) method
     * to specify the encoding.
     */
    @Deprecated
    public static String decode(String s) {

        String str = null;

        try {
            str = decode(s, dfltEncName);
        } catch (UnsupportedEncodingException e) {
            // The system should always have the platform default
        }

        return str;
    }

    /**
     * Decodes an {@code application/x-www-form-urlencoded} string using
     * a specific encoding scheme.
     *
     * <p>
     * This method behaves the same as {@linkplain decode(String s, Charset charset)}
     * except that it will {@linkplain Charset#forName look up the charset}
     * using the given encoding name.
     *
     * @param s   the {@code String} to decode
     * @param enc The name of a supported
     *            <a href="../lang/package-summary.html#charenc">character
     *            encoding</a>.
     * @return the newly decoded {@code String}
     * @throws UnsupportedEncodingException If character encoding needs to be consulted, but
     *                                      named character encoding is not supported
     * @implNote This implementation will throw an {@link IllegalArgumentException}
     * when illegal strings are encountered.
     * @see java.net.URLEncoder#encode(String, String)
     * @since 1.4
     */
    public static String decode(String s, String enc) throws UnsupportedEncodingException {
        if (enc.isEmpty()) {
            throw new UnsupportedEncodingException("URLDecoder: empty string enc parameter");
        }

        try {
            Charset charset = Charset.forName(enc);
            return decode(s, charset);
        } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
            throw new UnsupportedEncodingException(enc);
        }
    }

    /**
     * Decodes an {@code application/x-www-form-urlencoded} string using
     * a specific {@linkplain Charset Charset}.
     * The supplied charset is used to determine
     * what characters are represented by any consecutive sequences of the
     * form "<i>{@code %xy}</i>".
     * <p>
     * <em><strong>Note:</strong> The <a href=
     * "http://www.w3.org/TR/html40/appendix/notes.html#non-ascii-chars">
     * World Wide Web Consortium Recommendation</a> states that
     * UTF-8 should be used. Not doing so may introduce
     * incompatibilities.</em>
     *
     * @param s       the {@code String} to decode
     * @param charset the given charset
     * @return the newly decoded {@code String}
     * @throws NullPointerException     if {@code s} or {@code charset} is {@code null}
     * @throws IllegalArgumentException if the implementation encounters illegal
     *                                  characters
     * @implNote This implementation will throw an {@link IllegalArgumentException}
     * when illegal strings are encountered.
     * @see URLEncoder#encode(String, Charset)
     * @since 10
     */
    public static String decode(String s, Charset charset) {
        Objects.requireNonNull(charset, "Charset");
        boolean needToChange = false;
        int numChars = s.length();
        StringBuilder sb = new StringBuilder(numChars > 500 ? numChars / 2 : numChars);
        int i = 0;

        char c;
        byte[] bytes = null;
        while (i < numChars) {
            c = s.charAt(i);
            switch (c) {
                case '+':
                    sb.append(' ');
                    i++;
                    needToChange = true;
                    break;
                case '%':
                    /*
                     * Starting with this instance of %, process all
                     * consecutive substrings of the form %xy. Each
                     * substring %xy will yield a byte. Convert all
                     * consecutive  bytes obtained this way to whatever
                     * character(s) they represent in the provided
                     * encoding.
                     */

                    try {

                        // (numChars-i)/3 is an upper bound for the number
                        // of remaining bytes
                        if (bytes == null)
                            bytes = new byte[(numChars - i) / 3];
                        int pos = 0;

                        while (((i + 2) < numChars) &&
                                (c == '%')) {
                            int v = Integer.parseInt(s.substring(i + 1, i + 3), 16);
                            //int v = Integer.parseInt(s, i + 1, i + 3, 16);
                            if (v < 0)
                                throw new IllegalArgumentException(
                                        "URLDecoder: Illegal hex characters in escape "
                                                + "(%) pattern - negative value");
                            bytes[pos++] = (byte) v;
                            i += 3;
                            if (i < numChars)
                                c = s.charAt(i);
                        }

                        // A trailing, incomplete byte encoding such as
                        // "%x" will cause an exception to be thrown

                        if ((i < numChars) && (c == '%'))
                            throw new IllegalArgumentException(
                                    "URLDecoder: Incomplete trailing escape (%) pattern");

                        sb.append(new String(bytes, 0, pos, charset));
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "URLDecoder: Illegal hex characters in escape (%) pattern - "
                                        + e.getMessage());
                    }
                    needToChange = true;
                    break;
                default:
                    sb.append(c);
                    i++;
                    break;
            }
        }

        return (needToChange ? sb.toString() : s);
    }
}
