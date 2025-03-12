package net.ripe.db.whois.api.healthcheck;

import net.ripe.db.whois.common.HealthCheck;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.core.ResultSetExtractor;

import net.ripe.db.whois.common.domain.Timestamp;
import net.ripe.db.whois.common.DateTimeProvider;

import javax.sql.DataSource;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;
import java.util.HashMap;

@Component
public class DatabaseHealthCheck implements HealthCheck {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseHealthCheck.class);

    private final static String DB_HEALTH_CHECK_QUERY = "select object_id from last limit 1";
    private final static String USER_INFO_DUMP_QUERY = "select object_type, count(*) from last group by object_type";

    private final AtomicBoolean databaseHealthy = new AtomicBoolean(true);
    private final JdbcTemplate readTemplate;
    private final JdbcTemplate writeTemplate;

    @Autowired
    public DatabaseHealthCheck(@Qualifier("whoisSlaveDataSource") final DataSource readDataSource,
                              @Qualifier("whoisMasterDataSource") final DataSource writeDataSource) {
        this.readTemplate = new JdbcTemplate(readDataSource);
        this.writeTemplate = new JdbcTemplate(writeDataSource);
        readTemplate.setQueryTimeout(5);
        writeTemplate.setQueryTimeout(5);
    }

    @Override
    public boolean check() {
        return databaseHealthy.get();
    }

    @Scheduled(fixedDelay = 60 * 1_000)
    void updateStatus() {
        try {
            readTemplate.queryForObject(DB_HEALTH_CHECK_QUERY, Integer.class);
            writeTemplate.queryForObject(DB_HEALTH_CHECK_QUERY, Integer.class);
            databaseHealthy.set(true);
        } catch (DataAccessException e) {
            LOGGER.info("Database connection failed health check: {}", e.getMessage());
            databaseHealthy.set(false);
        }
    }

    @Scheduled(cron = "0 0 * * * ?") // 每天0点 dump 统计数据到statistics表
    public void dump_user_info() {
        LOGGER.info("[LOG GWY] schedule dump user info task start");
        Map<Integer, Integer> tmp_map = readTemplate.query(USER_INFO_DUMP_QUERY, new ResultSetExtractor<Map<Integer, Integer>>() {
            @Override
            public Map<Integer, Integer> extractData(ResultSet rs) throws SQLException, DataAccessException {
                Map<Integer, Integer> result = Maps.newHashMap();
                while (rs.next()) {
                    result.put(rs.getInt(1), rs.getInt(2));
                }
                return result;
            }
        });

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String currentTime = sdf.format(date);

        for (Map.Entry<Integer, Integer> entry : tmp_map.entrySet()) {
            Integer key = entry.getKey(); //object_type
            Integer value = entry.getValue(); //totalcount
            writeTemplate.update("insert into statistics (object_type, totalcount, timestamp) values (?,?,?)", key, value, currentTime);
            LOGGER.info("[LOG GWY] schedule dump user info task end");
        }
    }

}
