package com.hha.smarttracker.service;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;


public class StringifyHelper {

    private StringifyHelper() {
    }

    public static String toJson(Object obj) {
        return ToStringBuilder.reflectionToString(obj, ToStringStyle.JSON_STYLE);
    }

}
