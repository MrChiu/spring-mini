package com.demo.mvcframework.demo.service;

import com.demo.mvcframework.annotation.Service;

/**
 * @author qiudong
 */
@Service
public class DemoService {

    public String get(String name) {
        System.out.println(name);
        return name;
    }

}
