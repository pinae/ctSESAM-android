package de.pinyto.passwordgenerator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to extract domains from urls.
 */
public class DomainExtractor {
    public String extract(String url) {
        Pattern pattern = Pattern.compile("(?:https?://)?(\\w+\\.)+(\\w+).*");
        Matcher matcher = pattern.matcher(url);
        if (matcher.matches() && matcher.groupCount() >= 2) {
            return matcher.group(matcher.groupCount()-1) + matcher.group(matcher.groupCount());
        } else {
            return url;
        }
    }
}
