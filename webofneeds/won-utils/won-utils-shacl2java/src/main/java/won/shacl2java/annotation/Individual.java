package won.shacl2java.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD })
public @interface Individual {
    /**
     * The URI(s) of the individual(s).
     * 
     * @return
     */
    String[] value() default "";
}
