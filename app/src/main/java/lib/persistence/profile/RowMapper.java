package lib.persistence.profile;

import android.database.Cursor;

/**
 * Bir Cursor satırını bir nesneye dönüştürmek için kullanılan arayüz.
 *
 * @param <T> Dönüştürülecek nesnenin tipi.
 */
@FunctionalInterface
public interface RowMapper<T> {
    T mapRow(Cursor cursor) throws Exception;
}