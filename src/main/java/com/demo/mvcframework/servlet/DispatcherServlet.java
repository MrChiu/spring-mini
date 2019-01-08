package com.demo.mvcframework.servlet;

import com.demo.mvcframework.annotation.*;

import javax.servlet.*;
import javax.servlet.annotation.WebInitParam;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author qiudong
 */
@WebServlet(name = "dispatcherServlet", urlPatterns = "/*", loadOnStartup = 1,
        initParams = {@WebInitParam(name = "base-package", value = "com.demo.mvcframework")})
public class DispatcherServlet extends HttpServlet {

    //扫描的基包
    private String basePackage = "";
    //基包下的带包路经的类名
    private List<String> classNames = new ArrayList<>();
    //IOC容器
    private Map<String, Object> iocMap = new HashMap<>();
    private List<Handler> handlermapping = new ArrayList<>();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        basePackage = servletConfig.getInitParameter("base-package");
        //2.扫描到所有相关的类
        scanPackage(basePackage);
        //3.扫描@Controller/@Service，拿到对应的名称，并实例化它们修饰的类，加入到ioc容器
        instance();
        //4.扫描@Autowired，完成Spring IOC注入
        ioc();
        //5.扫描@RequestMapping，完成URL到某一个Controller的某一个方法上的映射关系。反射
        handleMapping();
        //handlemapping 用handlemapping 保存关系
        //等待请求，反射将结果返回
    }

    /**
     * URL，我们需要提取出来，映射到Controller的Method上
     */
    private void handleMapping() {
        if(iocMap.isEmpty()){
            return;
        }
        for(Map.Entry<String,Object> entry :iocMap.entrySet()){
            Class clazz = entry.getValue().getClass();
            if(clazz.isAnnotationPresent(Controller.class)){
                RequestMapping requestMapping = (RequestMapping) clazz.getAnnotation(RequestMapping.class);
                String baseUrl = requestMapping.value();
                Method[] methods = clazz.getMethods();
                for(Method method : methods){
                    if(method.isAnnotationPresent(RequestMapping.class)){
                        String mapping = method.getAnnotation(RequestMapping.class).value();
                        mapping ="/" + baseUrl +"/"+ mapping.replaceAll("/+","/");
                        Pattern p = Pattern.compile(mapping);
                        try {
                            handlermapping.add(new Handler(entry.getValue(), method, p));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    /**
     * Spring IOC注入
     */
    private void ioc() {
        if (iocMap.isEmpty()) {
            return;
        }
        //开始注入
        for (Map.Entry<String, Object> entry : iocMap.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowired.class)) {
                    //如果有值
                    Autowired autowired = field.getAnnotation(Autowired.class);
                    //获取值时请注意去掉两端空格
                    String beanName = autowired.value().trim();
                    //如果没值
                    if ("".equals(beanName)) {
                        beanName = field.getType().getName().trim();
                    }
                    field.setAccessible(true);
                    try {
                        field.set(entry.getValue(), iocMap.get(beanName) );
                        System.out.println(field.toString());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * 从这里你可以看出，我们完成了被注解标注的类的实例化，以及和注解名称的映射。
     */
    private void instance() {
        if (classNames.isEmpty()) {
            return;
        }
        try {
            for (String classname : classNames) {
                Class<?> clazz = Class.forName(classname);
                //初始化需要实例化的类
                if (clazz.isAnnotationPresent(Controller.class)) {
                    //如果自定义了id，就使用id，如果没有，默认首字母小写
                    Controller controller = clazz.getAnnotation(Controller.class);
                    String name = controller.value();
                    if (!"".equals(name)) {
                        iocMap.put(name, clazz.newInstance());
                    } else {
                        String beanName = lowerFirst(clazz.getSimpleName());
                        iocMap.put(beanName, clazz.newInstance());
                    }
                } else if (clazz.isAnnotationPresent(Service.class)) {
                    //无接口实现
                    Service service = clazz.getAnnotation(Service.class);
                    String name = service.value();
                    if (!"".equals(name)) {
                        iocMap.put(name, clazz.newInstance());
                    } else {
                        String beanName = clazz.getName();
                        iocMap.put(beanName, clazz.newInstance());
                    }
                    //如果有接口实现怎么办
                    Class<?>[] classes = clazz.getInterfaces();
                    for (Class cla : classes) {
                        iocMap.put(cla.getName(), cla.newInstance());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 首字母小写
     * @param simpleName
     * @return
     */
    private String lowerFirst(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    /**
     * 扫描基包
     * 注意，基包是X.Y.Z的形式，而URL是X/Y/Z的形式，需要转换。
     * @param basePackage
     */
    private void scanPackage(String basePackage) {
        //为了得到基包下面的URL路径需要对basePackage做转换：间.换装为/
        URL url = this.getClass().getClassLoader().getResource("/" + basePackage.replaceAll("\\.", "/"));
        if(url != null){
            File dir = new File(url.getFile());
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    //递归查找目录
                    scanPackage(basePackage + "." + file.getName());
                } else {
                    //将找到的类名保存下来，去掉尾部的.class后缀
                    classNames.add(basePackage + "." + file.getName().replace(".class", ""));
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doDispatch(request,response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doDispatch(request,response);
    }

    private void doDispatch(HttpServletRequest request, HttpServletResponse response) {
        String uri = request.getRequestURI();
        System.out.println("uri:"+uri);
        String context = request.getContextPath();
        uri = uri.replace(context,"").replaceAll("/+","/");
        Handler handler = getHandler(uri);
        //没有匹配到就返回错误
        if(handler==null){
            try {
                response.getWriter().write("404");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{//不等于null就执行
            Class<?>[] paramTypes = handler.method.getParameterTypes();
            Object[] paramValues = new Object[paramTypes.length];
            Map<String, String[]> map = request.getParameterMap();

            for(Map.Entry<String,String[]> entry : map.entrySet()){
                String value = Arrays.toString(entry.getValue()).replaceAll("\\[|\\]","").replaceAll(",\\s",",");
                if(handler.paramIndexMapping.containsKey(entry.getKey())){
                    int index = handler.paramIndexMapping.get(entry.getKey());
                    paramValues[index] = covert(paramTypes[index],value);
                }
                int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
                paramValues[reqIndex] = request;
                int resIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
                paramValues[resIndex] = response;
            }
            try {
                handler.method.invoke(handler.controller, paramValues);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    private Object covert(Class<?> paramType, String value) {
        if(Integer.class == paramType){
            return Integer.valueOf(value);
        }
        return value;
    }

    /**
     * 根据uri返回Handler
     * @param uri
     * @return
     */
    private Handler getHandler(String uri) {
        for (Handler handler : handlermapping) {
            Matcher matcher = handler.pattern.matcher(uri);
            if (matcher.matches()) {
                return handler;
            }
        }
        return null;
    }

    /**
     * handlermapping和controller的关系
     */
    private class Handler{
        private Object controller;//方法的对象
        private Method method;//方法
        private Pattern pattern;//正则表达式
        private Map<String, Integer> paramIndexMapping;//方法参数顺序

        private Handler(Object controller, Method method, Pattern pattern) {
            this.controller = controller;
            this.method = method;
            this.pattern = pattern;
            paramIndexMapping = new HashMap<>();
            putParamIndexMapping(method);
        }
        //获取加了注解的方法参数
        private void putParamIndexMapping(Method method) {
            Parameter[] parameters = method.getParameters();
            Annotation[][] annotations = method.getParameterAnnotations();
            for(int i = 0; i < annotations.length; i++){
               for(Annotation a: annotations[i]) {
                   if(a instanceof RequestParam){
                        String requestParam = ((RequestParam) a).value();
                        //如果定义了值
                        if(!"".equals(requestParam.trim())){
                            paramIndexMapping.put(requestParam, i);
                        }else {
                            //如果没有定义值
                            paramIndexMapping.put(parameters[i].getName(), i);
                        }
                   }
               }
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];
                if(type == HttpServletRequest.class || type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(), i);
                }
            }
        }
    }
}
