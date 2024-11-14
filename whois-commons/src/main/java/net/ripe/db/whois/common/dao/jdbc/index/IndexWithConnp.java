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

class IndexWithConnp extends IndexStrategyWithSingleLookupTable {
    public IndexWithConnp(final AttributeType attributeType) {
        super(attributeType, "connp");
    }

    @Override
    public int addToIndex(final JdbcTemplate jdbcTemplate, final RpslObjectInfo objectInfo, final RpslObject object, final String value) {
        final int object_id = objectInfo.getObjectId();
        final String query = String.format("INSERT INTO %s (object_id, name) VALUES (?, ?)", lookupTableName);
        return jdbcTemplate.update(query, object_id, value);
    }

    @Override
    public List<RpslObjectInfo> findInIndex(final JdbcTemplate jdbcTemplate, final String value) {
        return jdbcTemplate.query("SELECT l.object_id, l.object_type, l.pkey " + 
                                  "FROM last l JOIN connp c ON l.object_id = c.object_id " + 
                                  "WHERE c.name = ? AND l.sequence_id != 0",
                new RpslObjectInfoResultSetExtractor(),
                value);
    }
}
