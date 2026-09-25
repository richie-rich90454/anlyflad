package com.vectorium.core.svg;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import com.vectorium.core.model.VectorDocument;
public final class SvgCache {
    public static final int MAX_ENTRIES=32;
    public static final int MAX_ENTRY_BYTES=8*1024*1024;
    public static final int MAX_TOTAL_BYTES=64*1024*1024;
    private final AtomicReference<Entry[]> entries=new AtomicReference<Entry[]>(new Entry[MAX_ENTRIES]);
    private final AtomicInteger nextSlot=new AtomicInteger();
    private final AtomicLong totalBytes=new AtomicLong();
    public SvgCache() {
    }
    public synchronized String get(VectorDocument document) {
        validateDocument(document);
        Entry[] current=entries.get();
        for (int index=0;index<current.length;index++) {
            Entry entry=current[index];
            if (entry!=null&&entry.document==document) {
                return entry.svg;
            }
        }
        String svg=SvgWriter.write(document);
        put(document, svg);
        return svg;
    }
    public synchronized void put(VectorDocument document, String svg) {
        validateDocument(document);
        if (svg==null) {
            throw new IllegalArgumentException("svg must not be null");
        }
        int byteLength=SvgWriter.utf8Length(svg);
        if (byteLength>MAX_ENTRY_BYTES) {
            invalidate(document.getDocumentId());
            return;
        }
        while (true) {
            Entry[] current=entries.get();
            Entry[] next=current.clone();
            int existing=find(current, document.getDocumentId());
            long removed=existing>=0?current[existing].byteLength:0L;
            int[] victims=new int[MAX_ENTRIES];
            int victimCount=0;
            long projected=totalBytes.get()-removed+byteLength;
            for (int index=0;index<current.length&&projected>MAX_TOTAL_BYTES;index++) {
                if (current[index]!=null&&index!=existing) {
                    victims[victimCount]=index;
                    victimCount++;
                    projected-=current[index].byteLength;
                }
            }
            if (projected>MAX_TOTAL_BYTES) {
                return;
            }
            int slot;
            if (existing>=0) {
                slot=existing;
            } else if (victimCount>0) {
                slot=victims[0];
            } else {
                slot=findEmpty(current);
                if (slot<0) {
                    slot=nextSlot.getAndIncrement()&(MAX_ENTRIES-1);
                    removed+=current[slot].byteLength;
                }
            }
            for (int index=0;index<victimCount;index++) {
                removed+=current[victims[index]].byteLength;
                next[victims[index]]=null;
            }
            next[slot]=new Entry(document, svg, byteLength);
            if (entries.compareAndSet(current, next)) {
                totalBytes.addAndGet(byteLength-removed);
                return;
            }
        }
    }
    public synchronized boolean invalidate(VectorDocument document) {
        validateDocument(document);
        while (true) {
            Entry[] current=entries.get();
            int index=-1;
            for (int candidate=0;candidate<current.length;candidate++) {
                Entry entry=current[candidate];
                if (entry!=null&&entry.document==document) {
                    index=candidate;
                    break;
                }
            }
            if (index<0) {
                return false;
            }
            Entry[] next=current.clone();
            Entry removed=next[index];
            next[index]=null;
            if (entries.compareAndSet(current, next)) {
                totalBytes.addAndGet(-removed.byteLength);
                return true;
            }
        }
    }
    public synchronized boolean invalidate(String documentId) {
        if (documentId==null||documentId.trim().isEmpty()) {
            throw new IllegalArgumentException("documentId must not be blank");
        }
        while (true) {
            Entry[] current=entries.get();
            int index=find(current, documentId);
            if (index<0) {
                return false;
            }
            Entry[] next=current.clone();
            Entry removed=next[index];
            next[index]=null;
            if (entries.compareAndSet(current, next)) {
                totalBytes.addAndGet(-removed.byteLength);
                return true;
            }
        }
    }
    public synchronized void clear() {
        entries.set(new Entry[MAX_ENTRIES]);
        nextSlot.set(0);
        totalBytes.set(0L);
    }
    public synchronized int size() {
        Entry[] current=entries.get();
        int count=0;
        for (int index=0;index<current.length;index++) {
            if (current[index]!=null) {
                count++;
            }
        }
        return count;
    }
    public int capacity() {
        return MAX_ENTRIES;
    }
    private int find(Entry[] current, String documentId) {
        for (int index=0;index<current.length;index++) {
            Entry entry=current[index];
            if (entry!=null&&entry.document.getDocumentId().equals(documentId)) {
                return index;
            }
        }
        return -1;
    }
    private int findEmpty(Entry[] current) {
        for (int index=0;index<current.length;index++) {
            if (current[index]==null) {
                return index;
            }
        }
        return -1;
    }
    private static void validateDocument(VectorDocument document) {
        if (document==null) {
            throw new IllegalArgumentException("document must not be null");
        }
    }
    private static final class Entry {
        private final VectorDocument document;
        private final String svg;
        private final int byteLength;
        private Entry(VectorDocument document, String svg, int byteLength) {
            this.document=document;
            this.svg=svg;
            this.byteLength=byteLength;
        }
    }
}
