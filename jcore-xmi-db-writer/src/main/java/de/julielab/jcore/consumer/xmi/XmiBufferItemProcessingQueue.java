package de.julielab.jcore.consumer.xmi;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;

public class XmiBufferItemProcessingQueue extends ArrayList<XmiBufferItem> {
private CountDownLatch syncLatch;
    private XmiBufferItemProcessingQueue(int initialCapacity) {
        super(initialCapacity);
    }

    private XmiBufferItemProcessingQueue() {
    }

    public XmiBufferItemProcessingQueue(@NotNull Collection<? extends XmiBufferItem> c) {
        super(c);
        syncLatch = new CountDownLatch(c.size());
    }

}
