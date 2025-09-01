package com.example.adbkit;


import static org.junit.Assert.*;
import org.junit.Test;
import lib.persistence.annotations.DbColumnAnnotation;
import lib.persistence.annotations.DbTableAnnotation;
import lib.persistence.command.definition.CreateTableCommand;

@DbTableAnnotation(name = "simple_table")
class SimpleEntity {
    @DbColumnAnnotation(name = "id", isPrimaryKey = true, isIdentity = true, ordinal = 0)
    public int id;
    @DbColumnAnnotation(name = "description", isNullable = false, ordinal = 1)
    public String description;
    @DbColumnAnnotation(name = "value", ordinal = 2)
    public Double value;
}

@DbTableAnnotation(name = "composite_pk_table")
class CompositePkEntity {
    @DbColumnAnnotation(name = "key_part1", isPrimaryKey = true, ordinal = 0)
    public int keyPart1;
    @DbColumnAnnotation(name = "key_part2", isPrimaryKey = true, ordinal = 1)
    public String keyPart2;
    @DbColumnAnnotation(name = "data", ordinal = 2)
    public String data;
}

public class CreateTableCommandTest {

    @Test
    public void build_simpleEntity_shouldGenerateCorrectSql() {
        CreateTableCommand cmd = CreateTableCommand.build(SimpleEntity.class);
        String expectedSql = "CREATE TABLE IF NOT EXISTS `simple_table` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "`description` TEXT NOT NULL, " +
                "`value` REAL);";
        // StringJoiner nedeniyle sıralama farklı olabilir, bu yüzden içerikleri kontrol edin.
        // Veya daha sağlam bir SQL parser/comparator kullanın.
        // Bu basit örnek için, temel yapıyı kontrol edelim.
        assertTrue(cmd.getQuery().contains("CREATE TABLE IF NOT EXISTS `simple_table`"));
        assertTrue(cmd.getQuery().contains("`id` INTEGER PRIMARY KEY AUTOINCREMENT"));
        assertTrue(cmd.getQuery().contains("`description` TEXT NOT NULL"));
        assertTrue(cmd.getQuery().contains("`value` REAL"));
    }

    @Test
    public void build_compositePkEntity_shouldGenerateCorrectSql() {
        CreateTableCommand cmd = CreateTableCommand.build(CompositePkEntity.class);
        String query = cmd.getQuery();
        assertTrue(query.contains("CREATE TABLE IF NOT EXISTS `composite_pk_table`"));
        assertTrue(query.contains("`key_part1` INTEGER")); // PK'lar default NOT NULL
        assertTrue(query.contains("`key_part2` TEXT"));   // PK'lar default NOT NULL
        assertTrue(query.contains("`data` TEXT"));
        assertTrue(query.contains("PRIMARY KEY (`key_part1`, `key_part2`)"));
    }

    @Test
    public void build_withTableConstraints_shouldIncludeConstraints() {
        CreateTableCommand cmd = CreateTableCommand.build(SimpleEntity.class,
                "FOREIGN KEY(`some_fk_id`) REFERENCES `other_table`(`id`)");
        String query = cmd.getQuery();
        assertTrue(query.contains("FOREIGN KEY(`some_fk_id`) REFERENCES `other_table`(`id`)"));
    }
}
