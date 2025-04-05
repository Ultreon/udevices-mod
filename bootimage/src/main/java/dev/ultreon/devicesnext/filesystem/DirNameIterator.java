package dev.ultreon.devicesnext.filesystem;

import java.util.Iterator;

class DirNameIterator implements Iterator<String> {
    private final FSNode[] list;
    private int index;

    public DirNameIterator(FSNode[] list) {
        this.list = list;
    }

    @Override
    public boolean hasNext() {
        return index < list.length;
    }

    @Override
    public String next() {
        return list[index++].getName();
    }
}
