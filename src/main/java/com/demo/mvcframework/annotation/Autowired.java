package com.demo.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author qiudong
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Autowired {
    String value() default "";
}
