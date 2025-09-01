package lib.persistence.domain.entities;

import lib.persistence.annotations.DbColumnAnnotation;
import lib.persistence.annotations.DbTableAnnotation;

@DbTableAnnotation(name = "todos")
public class Todo {
    @DbColumnAnnotation(ordinal = 1,isPrimaryKey = true, isIdentity = true)
    public int id;
    @DbColumnAnnotation(ordinal = 2)
    public int userId;
    @DbColumnAnnotation(ordinal = 3,isNullable = false)
    public String title;
    @DbColumnAnnotation(ordinal = 4)
    public boolean completed;

    @Override
    public String toString() {
        return "Todo{" +
                "userId=" + userId +
                ", id=" + id +
                ", title='" + title + '\'' +
                ", completed=" + completed +
                '}';
    }
}