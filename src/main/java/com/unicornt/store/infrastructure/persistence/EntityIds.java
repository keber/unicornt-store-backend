package com.unicornt.store.infrastructure.persistence;

/**
 * Narrows a domain identifier (modelled uniformly as {@code long}) to the
 * {@code int} width of the catalog primary keys: {@code products},
 * {@code categories} and {@code product_types} are all {@code INTEGER} in the
 * schema, as are the {@code product_id} foreign keys.
 *
 * <p>A value outside the {@code int} range cannot match any row, so callers
 * treat {@link #toInt} returning {@code null} as a miss rather than letting a
 * silent {@code (int)} truncation collide with an unrelated row — e.g.
 * {@code 4_294_967_297L} would otherwise truncate to {@code 1}.</p>
 */
public final class EntityIds {

    private EntityIds() {
    }

    /** The id as an {@code int}, or {@code null} when it does not fit. */
    public static Integer toInt(long id) {
        return (id < Integer.MIN_VALUE || id > Integer.MAX_VALUE) ? null : (int) id;
    }
}
