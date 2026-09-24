package com.vectorium.core.svg;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.vectorium.core.model.VectorDocument;
public final class SvgCache {
    public static final int MAX_ENTRIES=32;
    private final AtomicReference<Entry[]> entries=new AtomicReference<Entry[]>(new Entry[MAX_ENTRIES]);
    private final AtomicInteger nextSlot=new AtomicInteger();
    public SvgCache() {
    }
    public String get(VectorDocument document) {
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
    public void put(VectorDocument document, String svg) {
        validateDocument(document);
        if (svg==null) {
            throw new IllegalArgumentException("svg must not be null");
        }
        while (true) {
            Entry[] current=entries.get();
            Entry[] next=current.clone();
            int existing=find(current, document.getDocumentId());
            if (existing>=0) {
                next[existing]=new Entry(document, svg);
            } else {
                int slot=findEmpty(current);
                if (slot<0) {
                    slot=nextSlot.getAndIncrement()&(MAX_ENTRIES-1);
                }
                next[slot]=new Entry(document, svg);
            }
            if (entries.compareAndSet(current, next)) {
                return;
            }
        }
    }
    public boolean invalidate(VectorDocument document) {
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
            next[index]=null;
            if (entries.compareAndSet(current, next)) {
                return true;
            }
        }
    }
    public boolean invalidate(String documentId) {
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
            next[index]=null;
            if (entries.compareAndSet(current, next)) {
                return true;
            }
        }
    }
    public void clear() {
        entries.set(new Entry[MAX_ENTRIES]);
        nextSlot.set(0);
    }
    public int size() {
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
        private Entry(VectorDocument document, String svg) {
            this.document=document;
            this.svg=svg;
        }
    }
}
