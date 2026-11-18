package blockrenderer6343.client.utils;

import it.unimi.dsi.fastutil.longs.AbstractLongSet;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

public class RevisionedLongSet extends AbstractLongSet {

    private final LongOpenHashSet values = new LongOpenHashSet();
    private long revision;

    public long getRevision() {
        return revision;
    }

    @Override
    public boolean add(long value) {
        if (!values.add(value)) return false;
        revision++;
        return true;
    }

    @Override
    public boolean remove(long value) {
        if (!values.remove(value)) return false;
        revision++;
        return true;
    }

    @Override
    public boolean contains(long value) {
        return values.contains(value);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public void clear() {
        if (values.isEmpty()) return;
        values.clear();
        revision++;
    }

    @Override
    public LongIterator iterator() {
        LongIterator iterator = values.iterator();
        return new LongIterator() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public long nextLong() {
                return iterator.nextLong();
            }

            @Override
            public void remove() {
                iterator.remove();
                revision++;
            }
        };
    }

    public void trim() {
        values.trim();
    }
}
