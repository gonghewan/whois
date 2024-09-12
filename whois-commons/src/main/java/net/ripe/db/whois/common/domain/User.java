package net.ripe.db.whois.common.domain;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.ripe.db.whois.common.rpsl.ObjectType;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.ripe.db.whois.common.domain.CIString.ciString;

public final class User {
    private final CIString username;
    private final String hashedPassword;
    private final Set<ObjectType> objectTypes;
    private static final Logger LOGGER = LoggerFactory.getLogger(User.class);

    private User(final CIString username, final String hashedPassword, final Set<ObjectType> objectTypes) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.objectTypes = Collections.unmodifiableSet(objectTypes);
    }

    public CIString getUsername() {
        return username;
    }

    public String getHashedPassword() {
        LOGGER.info("[GWY LOG] user get hashedpassword:" + hashedPassword);

        return hashedPassword;
    }

    public Set<ObjectType> getObjectTypes() {
        return objectTypes;
    }

    public boolean isValidPassword(final String password) {
        LOGGER.info("[GWY LOG] user isValidPassword:" + password);
        return getHash(password).equals(hashedPassword);
    }

    public static User createWithPlainTextPassword(final String username, final String password, final ObjectType... objectTypes) {
        LOGGER.info("[GWY LOG] user createWithPlainTextPassword: username:" + username);
        LOGGER.info("[GWY LOG] user createWithPlainTextPassword: password:" + password);
        LOGGER.info("[GWY LOG] user createWithPlainTextPassword: hashpassword:" + getHash(password));

        return new User(ciString(username), getHash(password), Sets.newEnumSet(Lists.newArrayList(objectTypes), ObjectType.class));
    }

    public static User createWithHashedPassword(final String username, final String hashedPassword, final Iterable<ObjectType> objectTypes) {
        LOGGER.info("[GWY LOG] user createWithHashedPassword: username:" + username);
        LOGGER.info("[GWY LOG] user createWithHashedPassword: hashedPassword:" + hashedPassword);

        return new User(ciString(username), hashedPassword, Sets.newEnumSet(objectTypes, ObjectType.class));
    }

    private static String getHash(final String text) {
        LOGGER.info("[GWY LOG] user gethashofPassword:" + DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8)));
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }
}
