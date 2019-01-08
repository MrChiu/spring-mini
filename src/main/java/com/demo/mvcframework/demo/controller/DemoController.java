package com.demo.mvcframework.demo.controller;

import com.demo.mvcframework.annotation.Controller;
import com.demo.mvcframework.annotation.RequestMapping;
import com.demo.mvcframework.annotation.Autowired;
import com.demo.mvcframework.annotation.RequestParam;
import com.demo.mvcframework.demo.service.DemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author qiudong
 */
@Controller
@RequestMapping("web")
public class DemoController {

    @Autowired()
    DemoService demoService;

    @RequestMapping("query.json")
    public void query(HttpServletRequest request, HttpServletResponse response ,
                      @RequestParam("name") String name){
        String result = demoService.get(name);
        try {
            response.getWriter().write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
