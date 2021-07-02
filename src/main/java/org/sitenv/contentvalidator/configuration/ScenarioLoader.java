package org.sitenv.contentvalidator.configuration;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.sitenv.contentvalidator.model.CCDARefModel;
import org.sitenv.contentvalidator.parsers.CCDAParser;
import org.springframework.beans.factory.InitializingBean;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

/**
 * Created by Brian on 8/15/2016.
 */
public class ScenarioLoader implements InitializingBean {
    private String scenarioFilePath;
    private CCDAParser ccdaParser;
    private HashMap<String, CCDARefModel> refModelHashMap = new HashMap<>();
    
	private static Logger log = Logger.getLogger(ScenarioLoader.class.getName());

    @Override
    public void afterPropertiesSet() throws Exception {
        loadScenarioFiles(scenarioFilePath, ccdaParser);
    }

    public void setScenarioFilePath(String scenarioFilePath) {
        this.scenarioFilePath = scenarioFilePath;
    }

    public void setCcdaParser(CCDAParser ccdaParser) {
        this.ccdaParser = ccdaParser;
    }

    public HashMap<String, CCDARefModel> getRefModelHashMap() {
        return refModelHashMap;
    }

    public CCDARefModel getRefModel(String scenario){
       return refModelHashMap.get(scenario);
    }

    private void loadScenarioFiles(String scenarioFilePath, CCDAParser ccdaParser) throws IOException{
        System.out.println("LOADING SCENARIO FILES AT " + scenarioFilePath);
        File dir = new File(scenarioFilePath);
        if (dir.isFile()) {
            throw new IOException("Directory to Load is a file and not a directory");
        } else {
            File[] list = dir.listFiles();
            for (File file : list) {
                if (!file.isDirectory() && !file.isHidden()) {
                	
                	log.info("Parsing File : " + file.getName());
                    byte[] encoded = Files.readAllBytes(Paths.get(file.toURI()));
                    CCDARefModel m = ccdaParser.parse(new String(encoded, "UTF-8"), true);
                    String modelName = FilenameUtils.getBaseName(file.getName());
                    refModelHashMap.put(modelName, m);
                }
            }
        }
    }
}
