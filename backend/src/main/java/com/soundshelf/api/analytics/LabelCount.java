package com.soundshelf.api.analytics;

/** Projection for every "group by one dimension, count rows" analytics query. */
public interface LabelCount {
    String getLabel();

    long getTotal();
}
