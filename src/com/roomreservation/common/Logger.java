package com.roomreservation.common;

import com.roomreservation.protobuf.protos.ResponseObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {

    /**
     * Initializes logger file
     * @param logFilePath Logger file path
     * @throws IOException Exception
     */
    public static void initializeLog(String logFilePath) throws IOException {
        if (new File(logFilePath).createNewFile()) {
            FileWriter fileWriter = new FileWriter(logFilePath, false);
            fileWriter.append("Datetime,Message,RequestType,RequestParameters,Status").append("\n");
            fileWriter.close();
        }
    }

    /**
     * Adds entry in log file
     * @param logFilePath Logger file path
     * @throws IOException Exception
     */
    public static void log(String logFilePath, ResponseObject responseObject) {
        try {
            FileWriter fileWriter = new FileWriter(logFilePath, true);
            fileWriter.append(toString(responseObject)).append("\n");
            fileWriter.close();
        } catch (IOException ignored) {}
    }

    /**
     * Converts Corba RMIResponse object to an appropriate string for the logger
     * @param responseObject ResponseObject object
     * @return
     */
    private static String toString(ResponseObject responseObject){
        return responseObject.getDateTime() + "," + responseObject.getMessage() + "," + responseObject.getRequestType() + "," + responseObject.getRequestParameters() + "," + responseObject.getStatus();
    }
}
