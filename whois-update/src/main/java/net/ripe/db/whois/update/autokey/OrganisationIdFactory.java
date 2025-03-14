package net.ripe.db.whois.update.autokey;

import net.ripe.db.whois.common.rpsl.AttributeType;
import net.ripe.db.whois.common.rpsl.RpslObject;
import net.ripe.db.whois.common.rpsl.ValidationMessages;
import net.ripe.db.whois.update.autokey.dao.OrganisationIdRepository;
import net.ripe.db.whois.update.domain.OrganisationId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.google.common.collect.Maps;
import java.util.Map;

@Component
public class OrganisationIdFactory extends AbstractAutoKeyFactory<OrganisationId> {
    private static final Map<String, String> PROVINCE_CODE = Maps.newHashMap();
    // use blank in "BJ"->"B J": AbstractAutoKeyFactory.java generateForName()->getSpace(), use blank split and generate space, when less than 2 part, add "A"
    static {
            PROVINCE_CODE.put("BJ","B J");
            PROVINCE_CODE.put("TJ","B J");
            PROVINCE_CODE.put("HE","B J");
            PROVINCE_CODE.put("SX","B J");
            PROVINCE_CODE.put("NM","B J");
            PROVINCE_CODE.put("LN","S Y");
            PROVINCE_CODE.put("JL","S Y");
            PROVINCE_CODE.put("HL","S Y");
            PROVINCE_CODE.put("SH","S H");
            PROVINCE_CODE.put("JS","N J");
            PROVINCE_CODE.put("ZJ","S H");
            PROVINCE_CODE.put("AH","N J");
            PROVINCE_CODE.put("FJ","S H");
            PROVINCE_CODE.put("JX","S H");
            PROVINCE_CODE.put("SD","N J");
            PROVINCE_CODE.put("HA","W H");
            PROVINCE_CODE.put("HB","W H");
            PROVINCE_CODE.put("HN","W H");
            PROVINCE_CODE.put("GD","G Z");
            PROVINCE_CODE.put("GX","G Z");
            PROVINCE_CODE.put("HI","G Z");
            PROVINCE_CODE.put("CQ","C D");
            PROVINCE_CODE.put("SC","C D");
            PROVINCE_CODE.put("GZ","C D");
            PROVINCE_CODE.put("YN","C D");
            PROVINCE_CODE.put("XZ","C D");
            PROVINCE_CODE.put("SN","X A");
            PROVINCE_CODE.put("GS","X A");
            PROVINCE_CODE.put("QH","X A");
            PROVINCE_CODE.put("NX","X A");
            PROVINCE_CODE.put("XJ","X A");
            PROVINCE_CODE.put("TW","T W");
            PROVINCE_CODE.put("HK","H K");
            PROVINCE_CODE.put("MO","M O");
    }

    @Autowired
    public OrganisationIdFactory(final OrganisationIdRepository organisationIdRepository) {
        super(organisationIdRepository);
    }

    @Override
    public AttributeType getAttributeType() {
        return AttributeType.ORGANISATION;
    }

    @Override
    public OrganisationId generate(final String keyPlaceHolder, final RpslObject object) {
        try {
            return generateForName(keyPlaceHolder, PROVINCE_CODE.get(object.getValueForAttribute(AttributeType.PROVINCE).toString()));
        } catch (IllegalArgumentException e){
            return generateForName(keyPlaceHolder, "C N");
        }
        
    }

    @Override
    public OrganisationId claim(final String key) throws ClaimException {
        throw new ClaimException(ValidationMessages.syntaxError(key, "must be AUTO-nnn for create"));
    }
}
