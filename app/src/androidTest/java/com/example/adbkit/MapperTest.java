package com.example.adbkit;


import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Test;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lib.persistence.annotations.DbColumnAnnotation;
import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.profile.DbColumn;
import lib.persistence.profile.DbDataType;
import lib.persistence.profile.Mapper;

import android.content.ContentValues; // AndroidX Test veya Robolectric gerekebilir ya da mock'layın

// Test edilecek entity'ler
@DbTableAnnotation(name = "test_entities")
class TestEntity {
    @DbColumnAnnotation(ordinal = 1, name = "entity_id", isPrimaryKey = true, isIdentity = true)
    public int id;

    @DbColumnAnnotation(ordinal = 2, name = "name_col")
    public String name;

    @DbColumnAnnotation(ordinal = 3, name = "value_col")
    public double value;

    @DbColumnAnnotation(ordinal = 4, name = "is_active_col")
    public boolean isActive;

    @DbColumnAnnotation(ordinal = 5, name = "created_date_col")
    public LocalDate createdDate;

    @DbColumnAnnotation(ordinal = 6, name = "updated_at_col")
    public LocalDateTime updatedAt;

    // Anotasyonsuz alan (dikkate alınmamalı)
    public String transientField;

    @DbColumnAnnotation(ordinal = 0) // Ordinal'ı düşük olan önce gelmeli
    public String firstField;
}

@DbTableAnnotation(name = "no_pk_entities")
class NoPkEntity {
    @DbColumnAnnotation(name = "data")
    public String data;
}

@DbTableAnnotation() // Name eksik -> exception beklenir
class MissingTableNameEntity {
    @DbColumnAnnotation() public int id;
}


public class MapperTest {

    @Test
    public void getTableName_shouldReturnCorrectName() {
        Assert.assertEquals("test_entities", Mapper.getTableName(TestEntity.class));
    }

    @Test(expected = IllegalStateException.class)
    public void getTableName_missingAnnotationName_shouldThrowException() {
        Mapper.getTableName(MissingTableNameEntity.class);
    }

    @Test
    public void classToDbColumns_shouldReturnCorrectColumnsInOrder() {
        ArrayList<DbColumn> columns = Mapper.classToDbColumns(TestEntity.class);

        assertNotNull(columns);
        assertEquals(7, columns.size()); // 7 anotasyonlu alan

        // Ordinal'a göre sıralama kontrolü
        Assert.assertEquals("firstField", columns.get(0).getFieldName());
        Assert.assertEquals("firstField", columns.get(0).getColumnName()); // Varsayılan isimlendirme (eğer name verilmemişse)
        // veya anotasyondaki 'name'

        Assert.assertEquals("id", columns.get(1).getFieldName());
        Assert.assertEquals("entity_id", columns.get(1).getColumnName());
        assertTrue(columns.get(1).isPrimaryKey());
        assertTrue(columns.get(1).isIdentity());

        Assert.assertEquals("name", columns.get(2).getFieldName());
        Assert.assertEquals("name_col", columns.get(2).getColumnName());
        Assert.assertEquals(DbDataType.TEXT, columns.get(2).getDataType());

        Assert.assertEquals("value", columns.get(3).getFieldName());
        Assert.assertEquals(DbDataType.REAL, columns.get(3).getDataType());

        Assert.assertEquals("isActive", columns.get(4).getFieldName());
        Assert.assertEquals(DbDataType.INTEGER, columns.get(4).getDataType());

        Assert.assertEquals("createdDate", columns.get(5).getFieldName());
        Assert.assertEquals(DbDataType.TEXT, columns.get(5).getDataType());

        Assert.assertEquals("updatedAt", columns.get(6).getFieldName());
        Assert.assertEquals(DbDataType.TEXT, columns.get(6).getDataType());
    }

    @Test
    public void objectToContentValues_shouldMapCorrectly() {
        TestEntity entity = new TestEntity();
        entity.name = "Test Name";
        entity.value = 123.45;
        entity.isActive = true;
        entity.createdDate = LocalDate.of(2023, 1, 10);
        entity.updatedAt = LocalDateTime.of(2023, 1, 10, 14, 30);
        entity.firstField = "Initial";
        // entity.id identity olduğu için ContentValues'e eklenmemeli

        // ContentValues mock'lanabilir veya AndroidX test ile kullanılabilir
        // Bu örnekte direkt kullanıyoruz, çalışması için test ortamında Android sınıflarının olması gerekir.
        // Daha saf unit test için ContentValues'i mock'layın.
        ContentValues cv = Mapper.objectToContentValues(entity);

        assertFalse(cv.containsKey("entity_id"));              // Identity alan atlanmalı
        assertEquals("Initial", cv.getAsString("firstField")); // name verilmediyse field adı = kolon adı
        assertEquals("Test Name", cv.getAsString("name_col"));
        assertEquals(123.45, cv.getAsDouble("value_col"), 0.001);
        assertEquals(1, (int) cv.getAsInteger("is_active_col")); // true -> 1
        assertEquals("2023-01-10", cv.getAsString("created_date_col"));
        assertEquals("2023-01-10T14:30:00", cv.getAsString("updated_at_col"));
    }

    @Test
    public void getPrimaryKeyColumns_shouldReturnPkColumns() {
        List<DbColumn> pks = Mapper.getPrimaryKeyColumns(TestEntity.class);
        assertEquals(1, pks.size());
        Assert.assertEquals("entity_id", pks.get(0).getColumnName());
    }

    @Test
    public void getPrimaryKeyColumns_noPk_shouldReturnEmptyList() {
        List<DbColumn> pks = Mapper.getPrimaryKeyColumns(NoPkEntity.class);
        assertTrue(pks.isEmpty());
    }

    @Test
    public void getColumnByName_byColumnName_shouldReturnColumn() {
        DbColumn col = Mapper.getColumnByName(TestEntity.class, "name_col");
        assertNotNull(col);
        Assert.assertEquals("name_col", col.getColumnName());
    }

    @Test
    public void getColumnByName_byFieldName_shouldReturnColumn() {
        DbColumn col = Mapper.getColumnByName(TestEntity.class, "createdDate");
        assertNotNull(col);
        Assert.assertEquals("created_date_col", col.getColumnName());
        Assert.assertEquals("createdDate", col.getFieldName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void getColumnByName_notFound_shouldThrowException() {
        Mapper.getColumnByName(TestEntity.class, "non_existent_column");
    }

    // cursorToObject testi enstrümantasyon testlerinde daha anlamlı olur,
    // çünkü gerçek bir Cursor nesnesi gerektirir.
    // Ancak mock bir Cursor ile de birim testi yazılabilir.
}
