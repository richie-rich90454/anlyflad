package com.vectorium.core.stage;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.vectorium.core.model.VectorDocument;
public final class BoundedPipelineMemoizer implements PipelineMemoizer {
    public static final int MAX_ENTRIES=16;
    private final AtomicReference<Entry[]> entries=new AtomicReference<Entry[]>(new Entry[MAX_ENTRIES]);
    private final AtomicInteger nextSlot=new AtomicInteger();
    public BoundedPipelineMemoizer() {
    }
    public VectorDocument get(VectorDocument input, long configHash) {
        if (input==null) {
            return null;
        }
        Entry[] current=entries.get();
        for (int index=0;index<current.length;index++) {
            Entry entry=current[index];
            if (entry!=null&&entry.input==input&&entry.configHash==configHash) {
                return entry.output;
            }
        }
        return null;
    }
    public void put(VectorDocument input, long configHash, VectorDocument output) {
        if (input==null||output==null) {
            throw new IllegalArgumentException("memoizer documents must not be null");
        }
        while (true) {
            Entry[] current=entries.get();
            Entry[] next=current.clone();
            int existing=-1;
            for (int index=0;index<current.length;index++) {
                Entry entry=current[index];
                if (entry!=null&&entry.input==input&&entry.configHash==configHash) {
                    existing=index;
                    break;
                }
            }
            if (existing>=0) {
                next[existing]=new Entry(input, configHash, output);
            } else {
                int slot=findEmpty(current);
                if (slot<0) {
                    slot=nextSlot.getAndIncrement()&(MAX_ENTRIES-1);
                }
                next[slot]=new Entry(input, configHash, output);
            }
            if (entries.compareAndSet(current, next)) {
                return;
            }
        }
    }
    public void clear() {
        entries.set(new Entry[MAX_ENTRIES]);
        nextSlot.set(0);
    }
    public int size() {
        Entry[] current=entries.get();
        int size=0;
        for (int index=0;index<current.length;index++) {
            if (current[index]!=null) {
                size++;
            }
        }
        return size;
    }
    private int findEmpty(Entry[] current) {
        for (int index=0;index<current.length;index++) {
            if (current[index]==null) {
                return index;
            }
        }
        return -1;
    }
    private static final class Entry {
        private final VectorDocument input;
        private final long configHash;
        private final VectorDocument output;
        private Entry(VectorDocument input, long configHash, VectorDocument output) {
            this.input=input;
            this.configHash=configHash;
            this.output=output;
        }
    }
}
