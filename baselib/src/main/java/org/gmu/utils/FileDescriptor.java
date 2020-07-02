package org.gmu.utils;

/**
 * User: ttg
 * Date: 25/01/13
 * Time: 8:58
 * To change this template use File | Settings | File Templates.
 */
public class FileDescriptor {
    public String name;
    public Long ts, size;

    public FileDescriptor(String data[]) {
        this.name = data[0];
        this.ts = new Long(data[1]);
        this.size = new Long(data[2]);
    }

    public FileDescriptor(String name, Long ts, Long size) {
        this.name = name;
        this.ts = ts;
        this.size = size;
    }
}