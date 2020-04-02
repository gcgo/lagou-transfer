package com.lagou.edu.factory;

import com.lagou.edu.annotations.Autowired;
import com.lagou.edu.annotations.Component;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

/**
 * @author 应癫
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactoryAnnotation {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String, Object> map = new HashMap<>();  // 存储对象
    private static Map<String, String> componentId = new HashMap<>();  // 存储component类名和id的对应关系

    static {
        // 任务一：读取解析xml，获取扫描的包，拿到所有@component的类，最后通过反射技术实例化对象并且存储待用（map集合）
        // 加载xml
        InputStream resourceAsStream = BeanFactoryAnnotation.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析xml
        SAXReader saxReader = new SAXReader();
        //保存解析出来的带@component注解的类
        List<Class> components = null;
        try {
            Document document = saxReader.read(resourceAsStream);
            Element rootElement = document.getRootElement();
            //寻找包扫描标签
            List<Element> beanList = rootElement.selectNodes("//component-scan");

            for (int i = 0; i < beanList.size(); i++) {
                Element element = beanList.get(i);
                String packageName = element.attributeValue("base-package");
                //该方法可以获得指定包下添加了指定注解的所有类
                components = BeanFactoryAnnotation.getClass4Annotation(packageName, Component.class);
                //遍历所有component，先装进map
                for (Class clazz : components) {
                    // 通过反射技术实例化对象
                    Object o = clazz.newInstance();  // 实例化之后的对象
                    Component annotation = (Component) clazz.getAnnotation(Component.class);
                    //id就是@Component()中的value属性
                    String id = annotation.value();
                    //处理id的情况
                    if (id.equals("")) {
                        // 存储到map中待用
                        map.put(clazz.getSimpleName(), o);
                        componentId.put(clazz.getName(), clazz.getSimpleName());
                    } else {
                        // 存储到map中待用
                        map.put(id, o);
                        componentId.put(clazz.getName(), id);
                    }
                }
            }

            //处理@AutoWired
            //需要特殊处理接口的情况
            for (Class component : components) {
                //先从map中找出对象
                String parentId = componentId.get(component.getName());
                //遍历属性
                Field[] fields = component.getDeclaredFields();
                for (Field field : fields) {
                    //如果属性添加了@Autowired注解，则需要注入
                    if (field.isAnnotationPresent(Autowired.class)) {
                        String name = null;
                        String className = null;
                        if (field.getType().isInterface()) {//如果该属性是一个接口类型
                            //遍历components列表，找它的实现类
                            for (Class aClass : components) {
                                //如果是同一接口，且不是自己
                                if (field.getType().isAssignableFrom(aClass) && !field.getType().equals(aClass)) {
                                    name = field.getType().getSimpleName();
                                    className = aClass.getName();
                                    break;
                                }
                            }
                        } else {
                            name = field.getType().getSimpleName();
                            className = field.getType().getName();
                        }
                        Object parentObject = map.get(parentId);
                        // 遍历父对象中的所有方法，找到"set" + name
                        Method[] methods = parentObject.getClass().getMethods();
                        for (Method method : methods) {
                            if (method.getName().equalsIgnoreCase("set" + name)) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                                String fieldId = componentId.get(className);
                                method.invoke(parentObject, map.get(fieldId));
                            }
                        }
                        //把处理之后的parentObject重新放到map中
                        map.put(parentId, parentObject);
                    }
                }
            }

        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

    }

    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static Object getBean(String id) {
        return map.get(id);
    }

    /**
     * 扫描指定包路径下所有包含指定注解的类
     * @param packageName 包名
     * @param apiClass 指定的注解
     * @return Set
     * */
    public static List<Class> getClass4Annotation(String packageName, Class<?> apiClass) {
        List<Class> classList = new ArrayList<>();
        // 是否循环迭代
        boolean recursive = true;
        // 获取包的名字 并进行替换
        String packageDirName = packageName.replace('.', '/');
        // 定义一个枚举的集合 并进行循环来处理这个目录下的things
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            // 循环迭代下去
            while (dirs.hasMoreElements()) {
                // 获取下一个元素
                URL url = dirs.nextElement();
                // 得到协议的名称
                String protocol = url.getProtocol();
                // 如果是以文件的形式保存在服务器上
                if ("file".equals(protocol)) {
                    // 获取包的物理路径
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    // 以文件的方式扫描整个包下的文件 并添加到集合中
                    File dir = new File(filePath);
                    List<File> fileList = new ArrayList<File>();
                    fetchFileList(dir, fileList);
                    for (File f : fileList) {
                        String fileName = f.getAbsolutePath();
                        if (fileName.endsWith(".class")) {
                            String noSuffixFileName = fileName.substring(8 + fileName.lastIndexOf("classes"), fileName.indexOf(".class"));
                            String filePackage = noSuffixFileName.replaceAll("\\\\", ".");
                            Class clazz = Class.forName(filePackage);
                            if (null != clazz.getAnnotation(apiClass)) {
                                classList.add(clazz);
                            }
                        }
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        // System.err.println(JsonUtil.toString(classSet));
        return classList;
    }

    /** * 查找所有的文件 * * @param dir 路径 * @param fileList 文件集合 */
    private static void fetchFileList(File dir, List<File> fileList) {
        if (dir.isDirectory()) {
            for (File f : Objects.requireNonNull(dir.listFiles())) {
                fetchFileList(f, fileList);
            }
        } else {
            fileList.add(dir);
        }
    }

}
