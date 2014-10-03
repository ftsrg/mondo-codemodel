package hu.bme.mit.codemodel.jamoppdiscoverer.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PackageName {

    protected static final Pattern PACKAGENAME_PATTERN = Pattern.compile("^(.*)\\..*?$");

    public static String of(String FQN) {
        Matcher matcher = PACKAGENAME_PATTERN.matcher(FQN);
        if (matcher.matches()) {
            return matcher.group(1);
        }

        return FQN;
    }
}
