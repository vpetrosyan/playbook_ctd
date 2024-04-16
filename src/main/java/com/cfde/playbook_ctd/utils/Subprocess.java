package com.cfde.playbook_ctd.utils;

import org.apache.log4j.Logger;
import java.io.BufferedReader;
import java.io.InputStreamReader;


public class Subprocess {
    static Logger logger = Logger.getLogger(Subprocess.class);
    private static boolean printCommandOut;

    static{
        printCommandOut = true;
        //printCommandOut = Boolean.parseBoolean(BCL2FastQConfiguration.getReference().getStringFromProperty(BCL2FastQConfiguration.getReference().getIgvConfigurationFile(),BCL2FastQConfiguration.PRINT_COMMANDS_TO_LOG));
    }

    public static void shellCall(String command){
        try {
            Process process = Runtime.getRuntime().exec(command);

            BufferedReader ersReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
            BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = ersReader.readLine()) != null) {
                if(printCommandOut) {
                    logger.info(line);
                }
            }
            ersReader.close();

            while ((line = outReader.readLine()) != null) {
                if(printCommandOut) {
                    logger.info(line);
                }
            }
            outReader.close();

            int exitCode = process.waitFor();

            if(printCommandOut) {
                logger.info("Executed: " + command + "\nExited with error code : " + exitCode);
            }
        } catch (Exception e) {
            logger.error(StackTracePrinter.printStackTrace(e));
        }
    }
}
