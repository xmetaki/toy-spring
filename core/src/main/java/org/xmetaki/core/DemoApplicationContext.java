package org.xmetaki.core;

import org.xmetaki.core.annotations.Component;
import org.xmetaki.core.annotations.ComponentScan;
import org.xmetaki.core.annotations.Scope;
import org.xmetaki.core.config.BeanDefinition;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class DemoApplicationContext {
    private Class configClass;

    // BeanDefinition集合
    private ConcurrentHashMap<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<String, BeanDefinition>();

    // 单例池
    private ConcurrentHashMap<String, Object> singletonPool = new ConcurrentHashMap<>();

    public DemoApplicationContext(Class configClass) throws Exception{
        this.configClass = configClass;

        // scan
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan scan = (ComponentScan)configClass.getAnnotation(ComponentScan.class);
            String scanPath = scan.value(); // 获取包名
            scanPath = scanPath.replace(".", "/");

            ClassLoader classLoader = DemoApplicationContext.class.getClassLoader();
            // 获取扫描包的绝对路径
            String filePath = classLoader.getResource(scanPath).getFile();

            File file = new File(filePath);
            if (file.isDirectory()) {
                File[] files = file.listFiles();
                // 获取到扫描包一级路径下所有的文件
                for(File f: files) {
                    String absolutePath = f.getAbsolutePath();

                    if (absolutePath.endsWith(".class")) {
                        // 判断当前类是否是一个Bean
                        // 首先需要把对象反射出来，这个前提又需要拿到对象所属的类
                        String className = fPath2PPath(absolutePath);
                        Class<?> clazz = classLoader.loadClass(className);

                        if (clazz.isAnnotationPresent(Component.class)) {
                            //创建BeanDefinition 放置Spring管理对象的元信息
                            BeanDefinition beanDefinition = new BeanDefinition();
                            if (clazz.isAnnotationPresent(Scope.class)) {
                                Scope scope = clazz.getAnnotation(Scope.class);
                                beanDefinition.setScope(scope.value());
                            } else {
                                // 没有注解默认单例
                                beanDefinition.setScope("singleton");
                            }

                            beanDefinition.setType(clazz);
                            // 统一放置BeanDefinition, 然后最后根据BeanDefinition的信息统一进行创建对象
                            beanDefinitionMap.put(getBeanName(clazz), beanDefinition);
                        }
                    }
                }
            }

        }

        createBeanBatch();
    }

    /*文件路径转换为包路径 (简陋的实现)*/
    private String fPath2PPath(String fpath) {
        return fpath.substring(fpath.indexOf("org"), fpath.indexOf(".class"))
                .replace("\\", ".");
    }

    /*获取BeanName*/
    private String getBeanName(Class clazz) {
        String beanName = "";
        if (clazz.isAnnotationPresent(Component.class)) {
            Component component = (Component) clazz.getAnnotation(Component.class);
            beanName = component.value();
        }
        if (beanName == null || beanName.isEmpty()) {
            beanName = clazz.getSimpleName();
        }
        return beanName;
    }

    private void createBeanBatch() {
        for(String beanName: beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if ("singleton".equals(beanDefinition.getScope())) {
                //创建单例Bean
               Object obj = createBean(beanName, beanDefinition);
               // 存放到单例池
               singletonPool.put(beanName, obj);
            }
        }
    }
    //创建单例bean
    private Object createBean(String beanName, BeanDefinition beanDefinition){
        Class clazz = beanDefinition.getType();
        // TODO: 处理没有无参构造方法的情况
        try {
            Object instance = clazz.getConstructor().newInstance();
            return instance;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    /*获取bean*/
    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null) {
            throw new NullPointerException();
        } else {
            String scope = beanDefinition.getScope();
            if ("prototype".equals(scope)) {
                //如果是多例Bean
                return createBean(beanName, beanDefinition);
            } else {
                // 否则为单例Bean
                Object bean = singletonPool.get(beanName);
                if (bean == null) {
                    Object singletonBean = createBean(beanName, beanDefinition);
                    singletonPool.put(beanName, singletonBean);
                }
                return bean;
            }
        }
    }
}
