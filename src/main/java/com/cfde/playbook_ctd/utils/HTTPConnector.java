package com.cfde.playbook_ctd.utils;

import com.cfde.playbook_ctd.utils.constants.Constants;
import org.apache.log4j.Logger;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.zip.GZIPInputStream;

public class HTTPConnector {

    private static Logger logger = Logger.getLogger(HTTPConnector.class);

    private URL url;
    private HttpURLConnection con = null;
    private BufferedReader inputBR;
    private int httpTimeout = 3000;
    private String inputLine;
    private String responseStr;

    public String sendHttpRequest(String urlString, String method, String parametares, HashMap<String,String> httpProperties){
        if(urlString == null){
            return null;
        }

        byte[] postData = null;
        if(method.equals(Constants.HTTP_POST)){
            postData = parametares.getBytes(StandardCharsets.UTF_8);
        }else if(method.equals(Constants.HTTP_GET) && parametares != null && !parametares.isEmpty()){
            urlString = urlString+"?"+parametares;
        }

        try{
            url = new URL(urlString);
            con = (HttpURLConnection)url.openConnection();
            con.setRequestMethod(method);
            con.setConnectTimeout(httpTimeout);
            //con.setReadTimeout(httpTimeout+15000);
            if(httpProperties != null && httpProperties.size() > 0){
                setAdditionalRequestProperties(con, httpProperties);
            }
            con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
            con.setRequestProperty("Accept-Encoding", "gzip, deflate, sdch, compress, identity");
            con.setRequestProperty("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");

            if(method.equals(Constants.HTTP_POST)){
                configurePostRequaest(con);
            }

            if(method.equals(Constants.HTTP_POST) && postData != null){
                responseStr =  executePostMethod(con, postData);
            }else if(method.equals(Constants.HTTP_GET)){
                //process input stream directly
                responseStr = processInputStream(con);
            }

        }catch(Exception e){
            String stackTraceStr = StackTracePrinter.printStackTrace(e);
            logger.error(stackTraceStr);
            System.out.println(stackTraceStr);
        }finally{
            closeHttpConnection();
            return responseStr;
        }
    }

    private void setAdditionalRequestProperties(HttpURLConnection con, HashMap<String,String> httpProperties){
        Iterator<String> iter = httpProperties.keySet().iterator();
        while(iter.hasNext()){
            String httpProperty = iter.next();
            String properyValue = httpProperties.get(httpProperty);
            con.setRequestProperty(httpProperty, properyValue);
        }
    }

    private void configurePostRequaest(HttpURLConnection con) throws Exception{
        con.setDoOutput(true);
        //con.setUseCaches(false);
        //con.setInstanceFollowRedirects(false);
    }

    private String executePostMethod(HttpURLConnection con, byte[] postData) throws Exception{
        //write post data first
        con.getOutputStream().write(postData);
        return processInputStream(con);
    }

    private String processInputStream(HttpURLConnection con) throws Exception{
        if(con.getContentEncoding() != null &&
                con.getContentEncoding().equalsIgnoreCase("gzip")){
            inputBR = new BufferedReader(
                    new InputStreamReader(
                            new GZIPInputStream(con.getInputStream())));
        }else{
            inputBR = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
        }

        int responseCode = con.getResponseCode();

        if(responseCode == 200){
            return getResponseAsString(inputBR, con);
        }else{
            logger.error("Received HTTP code: "+responseCode);
            return null;
        }
    }

    private String getResponseAsString(BufferedReader inputBR, HttpURLConnection con) throws Exception{
        StringBuffer response = new StringBuffer();
        while((inputLine = inputBR.readLine()) != null){
            response.append(inputLine);
        }
        return response.toString();
    }

    private void closeHttpConnection(){
        try{
            inputBR.close();
        }catch(Exception e){}
        if(con != null){
            con.disconnect();
        }
    }
}
