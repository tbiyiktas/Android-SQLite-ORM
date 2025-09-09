package com.example.adbkit.entities;

import lib.persistence.mapping.annotations.DbColumnAnnotation;
import lib.persistence.mapping.annotations.DbTableAnnotation;

@DbTableAnnotation
public class Person {
    @DbColumnAnnotation(ordinal = 1, isPrimaryKey = true, isIdentity = true, isNullable = false)
    public int id;
    @DbColumnAnnotation(ordinal = 2, isNullable = false)
    public String name;
    @DbColumnAnnotation(ordinal = 3, isNullable = false)
    public String surname;
}