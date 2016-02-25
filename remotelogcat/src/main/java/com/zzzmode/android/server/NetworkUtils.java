package com.zzzmode.android.server;

import java.net.*;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by zl on 16/1/26.
 */
public class NetworkUtils {

    public static int prefixLengthToNetmaskInt(int prefixLength)
            throws IllegalArgumentException {
        if (prefixLength < 0 || prefixLength > 32) {
            throw new IllegalArgumentException("Invalid prefix length (0 <= prefix <= 32)");
        }
        int value = 0xffffffff << (32 - prefixLength);
        return Integer.reverseBytes(value);
    }

    public static int netmaskIntToPrefixLength(int netmask) {
        return Integer.bitCount(netmask);
    }

    public static int inetAddressToInt(Inet4Address inetAddr)
            throws IllegalArgumentException {
        byte [] addr = inetAddr.getAddress();
        return ((addr[3] & 0xff) << 24) | ((addr[2] & 0xff) << 16) |
                ((addr[1] & 0xff) << 8) | (addr[0] & 0xff);
    }


    public static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = { (byte)(0xff & hostAddress),
                (byte)(0xff & (hostAddress >> 8)),
                (byte)(0xff & (hostAddress >> 16)),
                (byte)(0xff & (hostAddress >> 24)) };
        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int[] intToArray(int i) {
        return new int[]{i & 255, i >> 8 & 255, i >> 16 & 255, i >> 24 & 255};
    }


    public static InetAddress getLocalHostLANAddress() {
        try {
            InetAddress candidateAddress = null;
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();

                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (inetAddr instanceof Inet4Address && !inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            return inetAddr;
                        }
                        else if (candidateAddress == null) {
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            return InetAddress.getLocalHost();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }



    public static int getNetworkPrefixLength(InetAddress address){
        try {
            final NetworkInterface networkInterface = NetworkInterface.getByInetAddress(address);
            final List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
            for (InterfaceAddress addr:interfaceAddresses){
                int len=addr.getNetworkPrefixLength();
                if(len > 0 && len < 32){
                    return len;
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return 0;
    }
}