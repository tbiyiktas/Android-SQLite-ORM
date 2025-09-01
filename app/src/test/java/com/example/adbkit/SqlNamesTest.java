package com.example.adbkit;

import org.junit.Assert;
import org.junit.Test;

import lib.persistence.SqlNames;

public class SqlNamesTest {

    @Test
    public void qId_simpleIdentifier_shouldBeQuoted() {
        Assert.assertEquals("`table`", SqlNames.qId("table"));
        Assert.assertEquals("`column_name`", SqlNames.qId("column_name"));
    }

    @Test
    public void qId_identifierWithSpacesOrFunctions_shouldReturnAsIs() {
        Assert.assertEquals("COUNT(*)", SqlNames.qId("COUNT(*)"));
        Assert.assertEquals("`my table`", SqlNames.qId("`my table`")); // Zaten quoted
        Assert.assertEquals("strftime('%Y', date_col)", SqlNames.qId("strftime('%Y', date_col)"));
    }

    @Test
    public void qCol_simpleColumn_shouldBeQuoted() {
        Assert.assertEquals("`title`", SqlNames.qCol("title"));
    }

    @Test
    public void qCol_tableDotColumn_shouldBeQuotedCorrectly() {
        Assert.assertEquals("`users`.`name`", SqlNames.qCol("users.name"));
    }

    @Test
    public void qCol_star_shouldReturnStar() {
        Assert.assertEquals("*", SqlNames.qCol("*"));
    }

    @Test
    public void safeId_validIdentifier_shouldReturnAsIs() {
        Assert.assertEquals("my_table", SqlNames.safeId("my_table"));
        Assert.assertEquals("IndexName1", SqlNames.safeId("IndexName1"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeId_invalidIdentifier_withSpace_shouldThrowException() {
        SqlNames.safeId("my table");
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeId_invalidIdentifier_startingWithNumber_shouldThrowException() {
        SqlNames.safeId("1table");
    }

    @Test(expected = IllegalArgumentException.class)
    public void safeId_invalidIdentifier_withSpecialChar_shouldThrowException() {
        SqlNames.safeId("table#");
    }
}
