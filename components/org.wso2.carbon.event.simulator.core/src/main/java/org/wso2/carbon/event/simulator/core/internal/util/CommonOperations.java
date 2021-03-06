package org.wso2.carbon.event.simulator.core.internal.util;

import org.json.JSONObject;

/**
 * CommonOperations class is used to perform functions common to all validations
 */
public class CommonOperations {

    /**
     * checkAvailability() performs the following checks on the the json object and key provided
     * 1. has
     * 2. isNull
     * 3. isEmpty
     *
     * @param configuration JSON object containing configuration
     * @param key           name of key
     * @return true if checks are successful, else false
     */
    public static boolean checkAvailability(JSONObject configuration, String key) {
        return configuration.has(key)
                && !configuration.isNull(key)
                && !configuration.getString(key).isEmpty();
    }

    /**
     * checkAvailabilityOfArray() performs the following checks on the the json object and key provided.
     * This method is used for key's that contains json array values.
     * 1. has
     * 2. isNull
     * 3. isEmpty
     *
     * @param configuration JSON object containing configuration
     * @param key           name of key
     * @return true if checks are successful, else false
     */
    public static boolean checkAvailabilityOfArray(JSONObject configuration, String key) {
        return configuration.has(key)
                && !configuration.isNull(key)
                && configuration.getJSONArray(key).length() > 0;
    }

}
