package lib.persistence.mapping.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import lib.persistence.mapping.converters.TypeConverter;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DbConverterAnnotation {
    Class<? extends TypeConverter<?, ?>> converter();
}
