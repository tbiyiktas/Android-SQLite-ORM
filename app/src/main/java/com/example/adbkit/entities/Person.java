package com.example.adbkit.entities;

import lib.persistence.annotations.DbTableAnnotation;

@DbTableAnnotation
public class Person {
    public int id;
    public String name;
    public String surname;
}