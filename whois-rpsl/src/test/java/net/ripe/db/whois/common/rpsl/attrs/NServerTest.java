package net.ripe.db.whois.common.rpsl.attrs;

import net.ripe.db.whois.common.ip.Ipv4Resource;
import net.ripe.db.whois.common.ip.Ipv6Resource;
import org.junit.jupiter.api.Test;

import static net.ripe.db.whois.common.domain.CIString.ciString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class NServerTest {
    @Test
    public void empty() {
        assertThrows(AttributeParseException.class, () -> {
            NServer.parse("");
        });
    }

    @Test
    public void hostname_invalid() {
        assertThrows(AttributeParseException.class, () -> {
            NServer.parse("$");
        });
    }

    // @Test
    // public void hostname_only() {
    //     final NServer nServer = NServer.parse("dns.comcor.ru");
    //     assertThat(nServer.getHostname(), is(ciString("dns.comcor.ru")));
    //     assertThat(nServer.getIpInterval(), is(nullValue()));
    //     assertThat(nServer.getGlue(), is(nullValue()));
    //     assertThat(nServer.toString(), is("dns.comcor.ru"));
    // }

    @Test
    public void hostname_trailing_dot() {
        final NServer nServer = NServer.parse("dns.comcor.ru.");
        assertThat(nServer.getHostname(), is(ciString("dns.comcor.ru")));
        //assertThat(nServer.getIpInterval(), is(nullValue()));
        //assertThat(nServer.getGlue(), is(nullValue()));
        assertThat(nServer.toString(), is("dns.comcor.ru"));
    }

    // @Test
    // public void hostname_and_ipv4() {
    //     final NServer nServer = NServer.parse("dns.comcor.ru 194.0.0.0");
    //     assertThat(nServer.getHostname(), is(ciString("dns.comcor.ru")));
    //     assertThat((Ipv4Resource) nServer.getIpInterval(), is(Ipv4Resource.parse("194.0.0.0")));
    //     assertThat(nServer.getGlue(), is("194.0.0.0"));
    //     assertThat(nServer.toString(), is("dns.comcor.ru 194.0.0.0"));
    // }

    // @Test
    // public void hostname_and_ipv4_range_24() {
    //     assertThrows(AttributeParseException.class, () -> {
    //         NServer.parse("dns.comcor.ru 194.0.0.0/24");
    //     });
    // }

    // @Test
    // public void hostname_and_ipv4_range_32() {
    //     final NServer nServer = NServer.parse("dns.comcor.ru 194.0.0.0/32");
    //     assertThat(nServer.getHostname(), is(ciString("dns.comcor.ru")));
    //     assertThat((Ipv4Resource) nServer.getIpInterval(), is(Ipv4Resource.parse("194.0.0.0")));
    //     assertThat(nServer.getGlue(), is("194.0.0.0"));
    //     assertThat(nServer.toString(), is("dns.comcor.ru 194.0.0.0"));
    // }

    // @Test
    // public void hostname_trailing_dot_and_ipv4() {
    //     final NServer nServer = NServer.parse("dns.comcor.ru. 194.0.0.0");
    //     assertThat(nServer.getHostname(), is(ciString("dns.comcor.ru")));
    //     assertThat((Ipv4Resource) nServer.getIpInterval(), is(Ipv4Resource.parse("194.0.0.0")));
    //     assertThat(nServer.getGlue(), is("194.0.0.0"));
    //     assertThat(nServer.toString(), is("dns.comcor.ru 194.0.0.0"));
    // }

    // @Test
    // public void hostname_and_ipv4_list() {
    //     assertThrows(AttributeParseException.class, () -> {
    //         NServer.parse("dns.comcor.ru 194.0.0.0 194.0.0.0");
    //     });
    // }

    // @Test
    // public void ipv4_only() {
    //     final NServer nServer = NServer.parse("194.0.0.0");
    //     assertThat(nServer.getHostname(), is(ciString("194.0.0.0")));
    //     assertThat(nServer.getIpInterval(), is(nullValue()));
    //     assertThat(nServer.getGlue(), is(nullValue()));
    //     assertThat(nServer.toString(), is("194.0.0.0"));
    // }

    // @Test
    // public void hostname_and_ipv6() {
    //     final NServer nServer = NServer.parse("dns.comcor.ru f::1");
    //     assertThat(nServer.getHostname(), is(ciString("dns.comcor.ru")));
    //     assertThat((Ipv6Resource) nServer.getIpInterval(), is(Ipv6Resource.parse("f::1")));
    //     assertThat(nServer.getGlue(), is("f::1"));
    //     assertThat(nServer.toString(), is("dns.comcor.ru f::1"));
    // }

    // @Test
    // public void hostname_and_invalid_ip() {
    //     assertThrows(AttributeParseException.class, () -> {
    //         NServer.parse("dns.comcor.ru dns.comcor.ru");
    //     });
    // }
}
