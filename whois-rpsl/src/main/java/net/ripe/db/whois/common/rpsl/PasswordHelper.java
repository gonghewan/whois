package net.ripe.db.whois.common.rpsl;

import org.apache.commons.codec.digest.Md5Crypt;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PasswordHelper {
    private static final Pattern MD5_PATTERN = Pattern.compile("(?i)MD5-PW ((\\$1\\$.{1,8})\\$.{22})");
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordHelper.class);

    public static boolean authenticateMd5Passwords(final String authValue, final String... passwords) {
        return authenticateMd5Passwords(authValue, Arrays.asList(passwords));
    }

    public static boolean authenticateMd5Passwords(final String authValue, final Iterable<String> passwords) {
        LOGGER.info("[GWY LOG] entered into PasswordHelper.authenticateMd5Passwords");
        
        final Matcher matcher = MD5_PATTERN.matcher(authValue);
        if (matcher.matches()) {
            final String known = matcher.group(1);
            final String salt = matcher.group(2);
            LOGGER.info("[GWY LOG] PasswordHelper.authenticateMd5Passwords, known is " + known + " salt is " + salt);

            for (String password : passwords) {
                final String offered = Md5Crypt.md5Crypt(password.getBytes(), salt);
                LOGGER.info("[GWY LOG] PasswordHelper.authenticateMd5Passwords create md5 password by known salt and offered plain-text password" + offered);

                if (known.equals(offered)) {
                    LOGGER.info("[GWY LOG] PasswordHelper.authenticateMd5Passwords vaild equal");
                    return true;
                }
            }
        }
        return false;
    }

    public static final String hashMd5Password(final String cleantextPassword) {
        return Md5Crypt.md5Crypt(cleantextPassword.getBytes());
    }
}
