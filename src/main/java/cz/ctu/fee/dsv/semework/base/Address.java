package cz.ctu.fee.dsv.semework.base;

import java.io.Serializable;

// aby byla posilatelna skrz RMI, musi bit serializable
// musi byt comparable, protze je zaroven pouzita jako idcko
public class Address implements Comparable<Address>, Serializable {
    public String hostname;
    public Integer port;


    public Address () {
        this("127.0.0.1", 2010);
    }


    public Address (String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }


    public Address (Address addr) {
        this(addr.hostname, addr.port);
    }


    @Override
    public String toString() {
        return("Addr[host:'"+hostname+"', port:'"+port+"']");
    }


    @Override
    public int compareTo(Address address) {
        int retval = 0;
        if ((retval = hostname.compareTo(address.hostname)) == 0 ) {
            if ((retval = port.compareTo(address.port)) == 0 ) {
                return 0;
            }
            else
                return retval;
        }
        else
            return retval;
    }
}