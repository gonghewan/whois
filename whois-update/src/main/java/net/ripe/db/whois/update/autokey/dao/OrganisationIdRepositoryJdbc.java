package net.ripe.db.whois.update.autokey.dao;

import net.ripe.db.whois.common.collect.CollectionHelper;
import net.ripe.db.whois.update.domain.OrganisationId;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.EmptyResultDataAccessException;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Repository
@Transactional(propagation = Propagation.MANDATORY)
class OrganisationIdRepositoryJdbc implements OrganisationIdRepository {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganisationIdRepositoryJdbc.class);
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public OrganisationIdRepositoryJdbc(@Qualifier("sourceAwareDataSource") final DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final class OrganisationIdRange {
        private Integer rangeId;
        private int rangeEnd;

        private OrganisationIdRange(final Integer rangeId, final int rangeEnd) {
            this.rangeId = rangeId;
            this.rangeEnd = rangeEnd;
        }
    }

    @Override
    public boolean claimSpecified(final OrganisationId autoKey) {
        final OrganisationIdRange organisationIdRange = getOrganisationIdRange(autoKey.getSpace(), autoKey.getSuffix());

        if (organisationIdRange == null) {
            createRange(autoKey.getSpace(), autoKey.getSuffix(), autoKey.getIndex());
            updateRange(1, autoKey.getIndex());
            return true;
        }

        if (organisationIdRange.rangeEnd >= autoKey.getIndex()) {
            return false;
        }

        updateRange(organisationIdRange.rangeId, autoKey.getIndex());
        return true;
    }

    @Override
    public OrganisationId claimNextAvailableIndex(final String space, final String suffix) {
        // space: "CERNET ORG" -> "CO", suffix: "TEST"
        final OrganisationIdRange organisationIdRange = getOrganisationIdRange(space, suffix);

        int availableIndex;
        if (organisationIdRange == null) {
            availableIndex = 1;
            try{
                availableIndex = jdbcTemplate.queryForObject("SELECT max(range_end) FROM organisation_id", Integer.class) + 1;
            }catch (Exception e) {
                LOGGER.info("enter claimNextAvailableIndex  table is null");
            }
            createRange(space, suffix, availableIndex);
            updateRange(1, availableIndex);
        } else {
            availableIndex = organisationIdRange.rangeEnd + 1;
            updateRange(organisationIdRange.rangeId, availableIndex);
        }

        return new OrganisationId(space, availableIndex, suffix);
    }

    private OrganisationIdRange getOrganisationIdRange(final String space, final String suffix) {
        return CollectionHelper.uniqueResult(jdbcTemplate.query("" +
                "select range_id, range_end " +
                "  from organisation_id " +
                "  where space = ? and source = ? ",
                new RowMapper<OrganisationIdRange>() {
                    @Override
                    public OrganisationIdRange mapRow(final ResultSet rs, final int rowNum) throws SQLException {
                        return new OrganisationIdRange(rs.getInt("range_id"), rs.getInt("range_end"));
                    }
                }, space, getSuffixForSql(suffix)));
    }

    void createRange(final String space, final String suffix, final int end) {
        LOGGER.info("enter createRange end is " + end);
        jdbcTemplate.update("" +
                "insert into organisation_id(range_end, space, source) " +
                "  values(?, ?, ?)",
                end, space, getSuffixForSql(suffix));
    }

    void updateRange(final int rangeId, final int end) {
        LOGGER.info("enter updateRange end is " + end);
        jdbcTemplate.update(
                "update organisation_id set range_end = ?",
                end);
    }

    private String getSuffixForSql(final String suffix) {
        return StringUtils.isEmpty(suffix) ? "" : "-" + suffix;
    }
}
