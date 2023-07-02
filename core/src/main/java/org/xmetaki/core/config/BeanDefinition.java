package org.xmetaki.core.config;

import lombok.Data;

@Data
public class BeanDefinition {
    private Class type;

    private String scope;
}
