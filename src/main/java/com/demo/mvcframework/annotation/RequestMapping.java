package com.demo.mvcframework.annotation;

import java.lang.annotation.*;

/**
 * @author qiudong
 */
@Documented
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequestMapping {
    String value() default "";
}
