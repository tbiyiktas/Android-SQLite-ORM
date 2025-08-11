package lib.persistence.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DbColumnAnnotation {
    String name() default "";
    boolean isPrimaryKey() default false;
    boolean isIdentity() default false;
    boolean isNullable() default true;
    int ordinal() default 1010;
}