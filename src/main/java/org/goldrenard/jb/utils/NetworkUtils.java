/*
 * This file is part of Program JB.
 *
 * Program JB is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * Program JB is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with Program JB. If not, see <http://www.gnu.org/licenses/>.
 */
package org.goldrenard.jb.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Enumeration;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkUtils {

    private static final Logger log = LoggerFactory.getLogger(NetworkUtils.class);

    public static String localIPAddress() {
        try {
            for (final Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                final NetworkInterface intf = en.nextElement();
                for (final Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    final InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        String ipAddress = inetAddress.getHostAddress();
                        final int p = ipAddress.indexOf("%");
                        if (p > 0) {
                            ipAddress = ipAddress.substring(0, p);
                        }
                        if (log.isTraceEnabled()) {
                            log.trace("localIPAddress: {}", ipAddress);
                        }
                        return ipAddress;
                    }
                }
            }
        } catch (final SocketException e) {
            log.warn("Could not detect IP Address", e);
        }
        return "127.0.0.1";
    }

    public static String responseContent(final String url) throws Exception {
        final HttpClient client = new DefaultHttpClient();
        final HttpGet request = new HttpGet();
        request.setURI(new URI(url));
        final InputStream is = client.execute(request).getEntity().getContent();
        final BufferedReader inb = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder("");
        String line;
        final String NL = System.getProperty("line.separator");
        while ((line = inb.readLine()) != null) {
            sb.append(line).append(NL);
        }
        inb.close();
        return sb.toString();
    }

    public static String responseContentUri(final URI uri) throws Exception {
        final HttpClient client = new DefaultHttpClient();
        final HttpPost request = new HttpPost();
        request.setURI(uri);
        final InputStream is = client.execute(request).getEntity().getContent();
        final BufferedReader inb = new BufferedReader(new InputStreamReader(is));
        final StringBuilder sb = new StringBuilder("");
        String line;
        final String NL = System.getProperty("line.separator");
        while ((line = inb.readLine()) != null) {
            sb.append(line).append(NL);
        }
        inb.close();
        return sb.toString();
    }

    public static String spec(final String host, final String botid, final String custid, final String input) {
        if (log.isDebugEnabled()) {
            log.debug("Network spec --> custId = {}", custid);
        }
        String spec = "";
        try {
            if ("0".equals(custid)) { // get custid on first transaction with Pandorabots
            	spec = String.format("%s?botid=%s&input=%s",
            	                        "http://" + host + "/pandora/talk-xml",
            	                        botid,
            	                        URLEncoder.encode(input, "UTF-8"));
            } else { // get custid on first transaction with Pandorabots
            	spec =                 // re-use custid on each subsequent interaction
            	                    String.format("%s?botid=%s&custid=%s&input=%s",
            	                            "http://" + host + "/pandora/talk-xml",
            	                            botid,
            	                            custid,
            	                            URLEncoder.encode(input, "UTF-8"));
            }
        } catch (final Exception e) {
            log.error("spec failed", e);
        }
        if (log.isDebugEnabled()) {
            log.debug("Network spec result --> {}", spec);
        }
        return spec;
    }
}
