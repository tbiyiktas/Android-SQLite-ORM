package lib.persistence.domain.entities;

import lib.persistence.annotations.DbTableAnnotation;

@DbTableAnnotation
public class Todo {
    public int id;
    public int userId;
    public String title;
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