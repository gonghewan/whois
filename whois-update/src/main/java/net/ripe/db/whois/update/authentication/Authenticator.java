package net.ripe.db.whois.update.authentication;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.ripe.db.whois.common.Message;
import net.ripe.db.whois.common.Messages;
import net.ripe.db.whois.common.dao.UserDao;
import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.domain.IpRanges;
import net.ripe.db.whois.common.domain.Maintainers;
import net.ripe.db.whois.common.domain.User;
import net.ripe.db.whois.common.ip.IpInterval;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.update.authentication.strategy.AuthenticationFailedException;
import net.ripe.db.whois.update.authentication.strategy.AuthenticationStrategy;
import net.ripe.db.whois.update.domain.Origin;
import net.ripe.db.whois.update.domain.OverrideCredential;
import net.ripe.db.whois.update.domain.PasswordCredential;
import net.ripe.db.whois.update.domain.PreparedUpdate;
import net.ripe.db.whois.update.domain.UpdateContext;
import net.ripe.db.whois.update.domain.UpdateMessages;
import net.ripe.db.whois.update.domain.UpdateStatus;
import net.ripe.db.whois.update.log.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class Authenticator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Authenticator.class);

    private final IpRanges ipRanges;
    private final UserDao userDao;
    private final LoggerContext loggerContext;
    private final List<AuthenticationStrategy> authenticationStrategies;
    private final Map<CIString, Set<Principal>> principalsMap;

    @Autowired
    public Authenticator(final IpRanges ipRanges,
                         final UserDao userDao,
                         final Maintainers maintainers,
                         final LoggerContext loggerContext,
                         final AuthenticationStrategy[] authenticationStrategies) {
        this.ipRanges = ipRanges;
        this.userDao = userDao;
        this.loggerContext = loggerContext;
        Arrays.sort(authenticationStrategies);
        this.authenticationStrategies = Arrays.asList(authenticationStrategies);

        final Map<CIString, Set<Principal>> tempPrincipalsMap = Maps.newHashMap();
        addMaintainers(tempPrincipalsMap, maintainers.getEnduserMaintainers(), Principal.ENDUSER_MAINTAINER);
        addMaintainers(tempPrincipalsMap, maintainers.getLegacyMaintainers(), Principal.LEGACY_MAINTAINER);
        addMaintainers(tempPrincipalsMap, maintainers.getAllocMaintainers(), Principal.ALLOC_MAINTAINER);
        addMaintainers(tempPrincipalsMap, maintainers.getRsMaintainers(), Principal.RS_MAINTAINER);
        addMaintainers(tempPrincipalsMap, maintainers.getEnumMaintainers(), Principal.ENUM_MAINTAINER);
        addMaintainers(tempPrincipalsMap, maintainers.getDbmMaintainers(), Principal.DBM_MAINTAINER);
        this.principalsMap = Collections.unmodifiableMap(tempPrincipalsMap);
    }

    private static void addMaintainers(final Map<CIString, Set<Principal>> principalsMap, final Set<CIString> maintainers, final Principal principal) {
        for (final CIString maintainer : maintainers) {
            principalsMap.computeIfAbsent(maintainer, k -> Sets.newLinkedHashSet()).add(principal);
        }
    }

    public void authenticate(final Origin origin, final PreparedUpdate update, final UpdateContext updateContext) {
        final Subject subject;
        LOGGER.info("[GWY LOG] entered into authenticate");

        loggerContext.logCredentials(update.getUpdate());

        if (origin.isDefaultOverride()) {
            LOGGER.info("[GWY LOG] origin.isDefaultOverride");
            subject = new Subject(Principal.OVERRIDE_MAINTAINER);
        } else if (update.isOverride()) {
            LOGGER.info("[GWY LOG] origin.isOverride");
            subject = performOverrideAuthentication(origin, update, updateContext);
        } else {
            LOGGER.info("[GWY LOG] origin.else");
            subject = performAuthentication(origin, update, updateContext);
        }

        updateContext.subject(update, subject);
    }

    private Subject performOverrideAuthentication(final Origin origin, final PreparedUpdate update, final UpdateContext updateContext) {
        LOGGER.info("[GWY LOG] enter performOverrideAuthentication");
        final Set<OverrideCredential> overrideCredentials = update.getCredentials().ofType(OverrideCredential.class);
        final Set<Message> authenticationMessages = Sets.newLinkedHashSet();

        if (!origin.allowAdminOperations()) {
            LOGGER.info("[GWY LOG] !origin.allowAdminOperations");
            authenticationMessages.add(UpdateMessages.overrideNotAllowedForOrigin(origin));
        }

        if (overrideCredentials.size() != 1) {
            LOGGER.info("[GWY LOG] overrideCredentials.size() != 1");

            authenticationMessages.add(UpdateMessages.multipleOverridePasswords());
        }

        LOGGER.info("[GWY LOG] overrideCredentials[0]: " + overrideCredentials.iterator().next().toString());
        if (!authenticationMessages.isEmpty()) {
            LOGGER.info("[GWY LOG] authenticationMessages.isEmpty()");
            handleFailure(update, updateContext, authenticationMessages);
            return Subject.EMPTY;
        }

        final OverrideCredential overrideCredential = overrideCredentials.iterator().next();
        if (overrideCredential.getOverrideValues().isPresent()) {
            OverrideCredential.OverrideValues overrideValues = overrideCredential.getOverrideValues().get();
            final String username = overrideValues.getUsername();

            if (!isAllowedToUseOverride(origin, updateContext, username)) {
                LOGGER.info("[GWY LOG] user not equal @ripe.net");
                LOGGER.info("[GWY LOG] !isAllowedToUseOverride(origin, updateContext, username)");
                authenticationMessages.add(UpdateMessages.overrideOnlyAllowedByDbAdmins());
                handleFailure(update, updateContext, authenticationMessages);
                return Subject.EMPTY;
            }

            try {
                final User user = userDao.getOverrideUser(username);
                LOGGER.info("[GWY LOG] userDao.getOverrideUser(username): " + username);
                if (user.isValidPassword(overrideValues.getPassword()) && user.getObjectTypes().contains(update.getType())) {
                    LOGGER.info("[GWY LOG] user.isValidPassword");
                    updateContext.addMessage(update, UpdateMessages.overrideAuthenticationUsed());
                    return new Subject(Principal.OVERRIDE_MAINTAINER);
                }
            } catch (EmptyResultDataAccessException ignore) {
                loggerContext.logMessage(update, new Message(Messages.Type.INFO, "Unknown override user: %s", username));
            }
        }

        LOGGER.info("[GWY LOG] OverrideCredential failed");
        
        authenticationMessages.add(UpdateMessages.overrideAuthenticationFailed());
        handleFailure(update, updateContext, authenticationMessages);
        return Subject.EMPTY;
    }

    private boolean isAllowedToUseOverride(final Origin origin, final UpdateContext updateContext, final String overrideUsername) {
        if(ipRanges.isTrusted(IpInterval.parse(origin.getFrom()))) {
            LOGGER.info("[GWY LOG] ipRanges.isTrusted");
    
            return true;
        }

        if (updateContext.getUserSession() == null || updateContext.getUserSession().getUsername() == null || overrideUsername == null) {
            LOGGER.info("[GWY LOG] updateContext.getUserSession failed");
            
            return false;
        }

        return (updateContext.getUserSession().getUsername()).equals(overrideUsername.concat("@ripe.net"));
    }

    private Subject performAuthentication(final Origin origin, final PreparedUpdate update, final UpdateContext updateContext) {
        LOGGER.info("[GWY LOG] enter performAuthentication");
        final Set<Message> authenticationMessages = Sets.newLinkedHashSet();
        final Set<RpslObject> authenticatedObjects = Sets.newLinkedHashSet();

        final Set<String> passedAuthentications = new HashSet<>();
        final Set<String> failedAuthentications = new HashSet<>();

        if (update.getCredentials().ofType(PasswordCredential.class).size() > 20) {
            LOGGER.info("[GWY LOG] tooManyPasswordsSpecified");
            authenticationMessages.add(UpdateMessages.tooManyPasswordsSpecified());
        } else {
            LOGGER.info("[GWY LOG] will try inetnum/autnum/domain/mntby/route Authentication");
            for (final AuthenticationStrategy authenticationStrategy : authenticationStrategies) {
                LOGGER.info("[GWY LOG] authenticationStrategy name: " + authenticationStrategy.getName());

                if (authenticationStrategy.supports(update)) {
                    LOGGER.info("[GWY LOG] authenticationStrategy.supports(update)[has this atrribute]");

                    try {
                        authenticatedObjects.addAll(authenticationStrategy.authenticate(update, updateContext));
                        passedAuthentications.add(authenticationStrategy.getName());
                    } catch (AuthenticationFailedException e) {
                        authenticationMessages.addAll(e.getAuthenticationMessages());
                        failedAuthentications.add(authenticationStrategy.getName());
                    }
                }
            }
        }

        final Set<Principal> principals = Sets.newLinkedHashSet();
        for (final RpslObject authenticatedObject : authenticatedObjects) {
            LOGGER.info("[GWY LOG] authenticatedObject is: " + authenticatedObject.toString());
            principals.addAll(getPrincipals(authenticatedObject));
        }

        if (!principals.isEmpty() && !origin.isDefaultOverride()) {
            if (!origin.allowAdminOperations() || !ipRanges.isTrusted(IpInterval.parse(origin.getFrom()))) {
                LOGGER.info("ripeMntnerUpdatesOnlyAllowedFromWithinNetwork");
                authenticationMessages.add(UpdateMessages.ripeMntnerUpdatesOnlyAllowedFromWithinNetwork());
            }
        }

        final Subject subject = new Subject(principals, passedAuthentications, failedAuthentications);
        if (!authenticationMessages.isEmpty()) {
            LOGGER.info("Authenticatot handleFailure");
            handleFailure(update, updateContext, authenticationMessages);
        }

        return subject;
    }

    private Set<Principal> getPrincipals(final RpslObject authenticatedObject) {
        if (!authenticatedObject.getType().equals(ObjectType.MNTNER)) {
            return Collections.emptySet();
        }

        final Set<Principal> principals = principalsMap.get(authenticatedObject.getKey());
        if (principals == null) {
            return Collections.emptySet();
        }

        return principals;
    }

    private void handleFailure(final PreparedUpdate update, final UpdateContext updateContext, final Set<Message> authenticationMessages) {
        updateContext.status(update, UpdateStatus.FAILED_AUTHENTICATION);

        for (final Message message : authenticationMessages) {
            updateContext.addMessage(update, message);
            LOGGER.info("Authenticatot handleFailure: " + message.toString());

        }
    }
}
