package net.ripe.db.whois.update.domain;

import org.apache.commons.lang.Validate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OrganisationId extends AutoKey {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganisationId.class);

    public OrganisationId(final String space, final int index, final String suffix) {
        super(space, index, suffix);
        Validate.notNull(space, "space cannot be null");
        Validate.isTrue(index > 0, "index must be greater than 0");
        Validate.notNull(suffix, "suffix cannot be null");
    }

    @Override
    public String toString() {
        String actualIndex = String.format("%06d", getIndex());
        //LOGGER.info("enter OrganisationId.toString() is " + getSpace().toUpperCase() + actualIndex);
        return new StringBuilder()
                .append(getSpace().toUpperCase())
                .append(actualIndex)
                .toString();
    }
}
