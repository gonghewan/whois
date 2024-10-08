package net.ripe.db.whois.update.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.ripe.db.whois.common.dao.RpslObjectUpdateDao;
import net.ripe.db.whois.common.dao.RpslObjectUpdateInfo;
import net.ripe.db.whois.common.rpsl.ObjectType;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.update.domain.Action;
import net.ripe.db.whois.update.domain.PreparedUpdate;
import net.ripe.db.whois.update.domain.UpdateContext;
import net.ripe.db.whois.update.domain.UpdateMessages;
import net.ripe.db.whois.update.handler.validator.BusinessRuleValidator;
import net.ripe.db.whois.update.sso.SsoTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
class UpdateObjectHandler {
    private final RpslObjectUpdateDao rpslObjectUpdateDao;
    private final SsoTranslator ssoTranslator;
    private final Map<Action, Map<ObjectType, List<BusinessRuleValidator>>> validatorsByActionAndType;
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateObjectHandler.class);

    @Autowired
    public UpdateObjectHandler(final RpslObjectUpdateDao rpslObjectUpdateDao,
                               final List<BusinessRuleValidator> businessRuleValidators,
                               final SsoTranslator ssoTranslator) {
        this.ssoTranslator = ssoTranslator;

        // Sort the business rules in some predictable order so they are processed for end-to-end error checking
       Collections.sort(businessRuleValidators, new Comparator<BusinessRuleValidator>(){
            public int compare(BusinessRuleValidator b1, BusinessRuleValidator b2) {
                return b1.getClass().getName().compareToIgnoreCase(b2.getClass().getName());
            }
        });
        this.rpslObjectUpdateDao = rpslObjectUpdateDao;

        validatorsByActionAndType = Maps.newEnumMap(Action.class);
        for (final Action action : Action.values()) {
            final Map<ObjectType, List<BusinessRuleValidator>> validatorsByType = Maps.newEnumMap(ObjectType.class);
            for (final ObjectType objectType : ObjectType.values()) {
                validatorsByType.put(objectType, Lists.<BusinessRuleValidator>newArrayList());
            }

            validatorsByActionAndType.put(action, validatorsByType);
        }

        for (final BusinessRuleValidator businessRuleValidator : businessRuleValidators) {
            final List<Action> actions = businessRuleValidator.getActions();
            for (final Action action : actions) {
                for (final ObjectType objectType : businessRuleValidator.getTypes()) {
                    validatorsByActionAndType.get(action).get(objectType).add(businessRuleValidator);
                }
            }
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void execute(final PreparedUpdate update, final UpdateContext updateContext) {
        LOGGER.info("[GWY LOG] entered into updateObjectHandler.execute");

        if (!updateContext.hasErrors(update)) {
            LOGGER.info("[GWY LOG] !updateContext.hasErrors");

            final RpslObjectUpdateInfo updateInfo;
            final RpslObject updatedObject = ssoTranslator.translateFromCacheAuthToUuid(updateContext, update.getUpdatedObject());
            switch (update.getAction()) {
                case CREATE:
                    LOGGER.info("[GWY LOG] CREATE");
                    updateInfo = rpslObjectUpdateDao.createObject(updatedObject);
                    updateContext.updateInfo(update, updateInfo);
                    break;
                case MODIFY:
                    LOGGER.info("[GWY LOG] MODIFY");
                    updateInfo = rpslObjectUpdateDao.updateObject(update.getReferenceObject().getObjectId(), updatedObject);
                    updateContext.updateInfo(update, updateInfo);
                    break;
                case DELETE:
                    LOGGER.info("[GWY LOG] DELETE");
                    final RpslObject object = update.getReferenceObject();
                    updateInfo = rpslObjectUpdateDao.deleteObject(object.getObjectId(), object.getKey().toString());
                    updateContext.updateInfo(update, updateInfo);
                    break;
                case NOOP:
                    LOGGER.info("[GWY LOG] NOOP");
                    updateContext.addMessage(update, UpdateMessages.updateIsIdentical());
                    break;
                default:
                    throw new IllegalStateException("Unhandled action: " + update.getAction());
            }
        }
    }

    public boolean validateBusinessRules(final PreparedUpdate update, final UpdateContext updateContext) {
        // TODO [AK] There must be a better way to set the status than to count errors
        LOGGER.info("[GWY LOG] entered into updateObjectHandler." + update.getKey());

        final int initialErrorCount = updateContext.getErrorCount(update);

        final Action action = update.getAction();
        final Map<ObjectType, List<BusinessRuleValidator>> validatorsByType = validatorsByActionAndType.get(action);

        final ObjectType type = update.getType();
        LOGGER.info("[GWY LOG] entered into updateObjectHandler. objecttype is: " + type.toString());
        for (final BusinessRuleValidator businessRuleValidator : validatorsByType.get(type)) {
            LOGGER.info("[GWY LOG] entered into updateObjectHandler. businessRuleValidator");
            businessRuleValidator.validate(update, updateContext);
        }

        LOGGER.info("[GWY LOG] entered into updateObjectHandler. getErrorCount is: " + updateContext.getErrorCount(update));
        return updateContext.getErrorCount(update) == initialErrorCount;
    }
}
