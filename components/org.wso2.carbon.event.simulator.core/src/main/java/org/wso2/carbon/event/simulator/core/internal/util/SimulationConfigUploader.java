package org.wso2.carbon.event.simulator.core.internal.util;

import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.wso2.carbon.event.simulator.core.exception.FileAlreadyExistsException;
import org.wso2.carbon.event.simulator.core.exception.FileOperationsException;
import org.wso2.carbon.event.simulator.core.exception.InvalidConfigException;

import static org.wso2.carbon.event.simulator.core.internal.util.CommonOperations.checkAvailability;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class is responsible for saving, modifying and deleting the simulation configurations
 */
public class SimulationConfigUploader {
    private static final Logger log = Logger.getLogger(SimulationConfigUploader.class);
    private static final SimulationConfigUploader configUploader =
            new SimulationConfigUploader();

    private SimulationConfigUploader() {
    }

    /**
     * getConfigUploader() returns Singleton configUploader object
     *
     * @return configUploader
     */
    public static SimulationConfigUploader getConfigUploader() {
        return configUploader;
    }

    /**
     * Method to upload a simulation configuration.
     *
     * @param simulationConfig simulation configuration being uploaded
     * @param destination      destination where the simulation configuration must be stored
     * @throws FileOperationsException if an IOException occurs while copying uploaded stream to
     *                                 'destination' directory
     */
    public void uploadSimulationConfig(String simulationConfig, String destination) throws FileOperationsException,
            InvalidConfigException, FileAlreadyExistsException {
        String simulationName = getSimulationName(simulationConfig);
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(destination, (simulationName + "." +
                EventSimulatorConstants.SIMULATION_FILE_EXTENSION)))) {
            writer.write(simulationConfig);
            if (log.isDebugEnabled()) {
                log.debug("Successfully uploaded simulation configuration '" + simulationName + "'.");
            }
        } catch (java.nio.file.FileAlreadyExistsException e) {
            /**
             * since the deployer takes about 15 seconds to create a simulator for an uploaded simulation config, 2
             * consecutive requests upload the same csv file will result in java.nio.file.FileAlreadyExistsException
             */
            log.error("A simulation already exists under the name " + "'" + simulationName + "'", e);
            throw new FileAlreadyExistsException("A simulation already exists under the name "
                    + "'" + simulationName + "'", e);
        } catch (IOException e) {
            log.error("Error occurred while copying the file '" + simulationName + "'.", e);
            throw new FileOperationsException("Error occurred while copying the file '" + simulationName +
                    "'. ", e);
        }
    }

    /**
     * Method to delete an uploaded simulation configuration.
     *
     * @param simulationName name of simulation to be deleted
     * @param destination    location where the simulation is saved
     * @return true if simulation configuration was deleted, else return false
     * @throws FileOperationsException if an IOException occurs while deleting file
     */
    public boolean deleteSimulationConfig(String simulationName, String destination) throws FileOperationsException {
        try {
            if (log.isDebugEnabled()) {
                log.debug("Delete simulation configuration '" + simulationName + "'.");
            }
            return Files.deleteIfExists(Paths.get(destination, simulationName + "." +
                    EventSimulatorConstants.SIMULATION_FILE_EXTENSION));
        } catch (IOException e) {
            log.error("Error occurred while deleting the simulation configuration '" + simulationName +
                    "'.", e);
            throw new FileOperationsException("Error occurred while deleting the simulation configuration '" +
                    simulationName + "'.'", e);
        }
    }

    /**
     * getSimulationConfig() is used to retrieve an uploaded simulation configuration
     *
     * @param simulationName name of simulation to be retrieved
     * @param destination    where the simulation configuration is stored
     * @return simulation configuration
     * @throws FileOperationsException is an error occurs when reading simulation configuration file
     */
    public String getSimulationConfig(String simulationName, String destination) throws FileOperationsException {
        try {
            return new String(Files.readAllBytes(Paths.get(destination, (simulationName + "." +
                    EventSimulatorConstants.SIMULATION_FILE_EXTENSION))),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error occurred while reading the simulation configuration '" +
                    simulationName + "'.", e);
            throw new FileOperationsException("Error occurred while reading the simulation configuration '" +
                    simulationName + "'.", e);
        }
    }

    /**
     * getSimulationName() is used to retrieve the simulation name of a simulation configuration
     *
     * @param simulationConfig simulation configuration
     * @return simulation configuration name
     * @throws InvalidConfigException if the simulation configuration doesnt contain a simulation name
     */
    public String getSimulationName(String simulationConfig) throws InvalidConfigException {
        try {
            JSONObject configuration = new JSONObject(simulationConfig);
            if (configuration.has(EventSimulatorConstants.EVENT_SIMULATION_PROPERTIES)
                    && !configuration.isNull(EventSimulatorConstants.EVENT_SIMULATION_PROPERTIES)) {
                if (checkAvailability(configuration.getJSONObject(EventSimulatorConstants.EVENT_SIMULATION_PROPERTIES),
                        EventSimulatorConstants.EVENT_SIMULATION_NAME)) {
                    return configuration.getJSONObject(EventSimulatorConstants.EVENT_SIMULATION_PROPERTIES)
                            .getString(EventSimulatorConstants.EVENT_SIMULATION_NAME);
                } else {
                    throw new InvalidConfigException("Simulation name is required for event simulation. Invalid " +
                            "simulation configuration provided : " + configuration.toString());
                }
            } else {
                throw new InvalidConfigException("Simulation properties are required for event simulation. Invalid " +
                        "simulation configuration provided : " + configuration.toString());
            }
        } catch (JSONException e) {
            throw new InvalidConfigException("Invalid simulation configuration provided : " + simulationConfig, e);
        }
    }
}
