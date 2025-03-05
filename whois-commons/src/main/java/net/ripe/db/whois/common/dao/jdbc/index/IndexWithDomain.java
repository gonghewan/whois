package net.ripe.db.whois.common.dao.jdbc.index;


import net.ripe.db.whois.common.dao.RpslObjectInfo;
import net.ripe.db.whois.common.dao.jdbc.domain.RpslObjectInfoResultSetExtractor;
import net.ripe.db.whois.common.domain.CIString;
import net.ripe.db.whois.common.ip.Ipv4Resource;
import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslObject;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

class IndexWithDomain extends IndexStrategyWithSingleLookupTable {
    public IndexWithDomain(final AttributeType attributeType) {
        super(attributeType, "domain");
    }

    @Override
    public int addToIndex(final JdbcTemplate jdbcTemplate, final RpslObjectInfo objectInfo, final RpslObject object, final String value) {
        final CIString domainAttribute = object.getValueOrNullForAttribute(AttributeType.DOMAIN);
        final String domain = domainAttribute == null ? "" : domainAttribute.toString();
        // GRS sources might not have netname
        final CIString netnameAttribute = object.getValueOrNullForAttribute(AttributeType.NETNAME);
        final String netname = netnameAttribute == null ? "" : netnameAttribute.toString();

        return jdbcTemplate.update(
                "INSERT INTO domain (object_id, domain, netname) VALUES (?, ?, ?)",
                objectInfo.getObjectId(),
                domain,
                netname);
    }

    @Override
    public List<RpslObjectInfo> findInIndex(final JdbcTemplate jdbcTemplate, final String value) {
        return jdbcTemplate.query(" " +
                "SELECT l.object_id, l.object_type, l.pkey " +
                "  FROM domain " +
                "  LEFT JOIN last l ON l.object_id = domain.object_id " +
                "  WHERE domain = ? " +
                "  AND l.sequence_id != 0 ",
                new RpslObjectInfoResultSetExtractor(),
                value);
    }
}
