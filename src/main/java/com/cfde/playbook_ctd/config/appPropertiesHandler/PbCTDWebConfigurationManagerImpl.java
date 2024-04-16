package com.cfde.playbook_ctd.config.appPropertiesHandler;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.util.HashMap;

@Component
public class PbCTDWebConfigurationManagerImpl implements PbCTDWebConfigurationManager {

    private static Logger logger = Logger.getLogger(PbCTDWebConfigurationManagerImpl.class);
    private static HashMap<String, String> propertiesMap;

    /*
    @Value("${in_property_name_1}")
    private String name_1;

    @Value("${in_property_name_2}")
    private String name_2;
    */

    @PostConstruct
    private HashMap<String, String> initializePropertieMap(){
        if(this.propertiesMap == null) {
            loadPropertieMap();
        }
        return this.propertiesMap;
    }

    public void loadPropertieMap(){
        this.propertiesMap = new HashMap<String, String>();
        /*
        this.propertiesMap.put(Constants.NAME_1,name_1);
        this.propertiesMap.put(Constants.NAME_2,name_2);
        */
        if(this.propertiesMap.size() > 0){
            logger.info("Initialized app's config custom properties!");
        }
    }

    @Override
    public String getPropertyByNameAsString(String propertyName){
        if(propertiesMap == null || propertiesMap.isEmpty()){
            return null;
        }
        return propertiesMap.get(propertyName);
    }

    @Override
    public Integer getPropertyByNameAsInt(String propertyName){
        if(propertiesMap == null || propertiesMap.isEmpty()){
            return null;
        }
        return Integer.parseInt(propertiesMap.get(propertyName));
    }

    @Override
    public String[] getPropertyByNameAsArray(String propertyName){
        if(propertiesMap == null || propertiesMap.isEmpty()){
            return null;
        }
        return (propertiesMap.get(propertyName)).split(",");
    }
}
