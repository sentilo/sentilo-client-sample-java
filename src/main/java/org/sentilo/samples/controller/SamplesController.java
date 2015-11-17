/*
 * Sentilo
 *  
 * Original version 1.4 Copyright (C) 2013 Institut Municipal d’Informàtica, 
 * Ajuntament de Barcelona.
 * Modified by Opentrends adding support for multitenant deployments and SaaS. 
 * Modifications on version 1.5 Copyright (C) 2015 Opentrends Solucions i Sistemes, S.L.
 * 
 *   
 * This program is licensed and may be used, modified and redistributed under the
 * terms  of the European Public License (EUPL), either version 1.1 or (at your 
 * option) any later version as soon as they are approved by the European 
 * Commission.
 *   
 * Alternatively, you may redistribute and/or modify this program under the terms
 * of the GNU Lesser General Public License as published by the Free Software 
 * Foundation; either  version 3 of the License, or (at your option) any later 
 * version. 
 *   
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. 
 *   
 * See the licenses for the specific language governing permissions, limitations 
 * and more details.
 *   
 * You should have received a copy of the EUPL1.1 and the LGPLv3 licenses along 
 * with this program; if not, you may find them at: 
 *   
 *   https://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *   http://www.gnu.org/licenses/ 
 *   and 
 *   https://www.gnu.org/licenses/lgpl.txt
 */
package org.sentilo.samples.controller;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;

import org.codehaus.jackson.map.ObjectMapper;
import org.sentilo.common.domain.AuthorizedProvider;
import org.sentilo.common.domain.CatalogComponent;
import org.sentilo.common.domain.CatalogSensor;
import org.sentilo.platform.client.core.PlatformTemplate;
import org.sentilo.platform.client.core.domain.CatalogInputMessage;
import org.sentilo.platform.client.core.domain.CatalogOutputMessage;
import org.sentilo.platform.client.core.domain.DataInputMessage;
import org.sentilo.platform.client.core.domain.Observation;
import org.sentilo.platform.client.core.domain.SensorObservations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SamplesController
 * 
 * Executes a basic Sentilo Java Client Platform example that connects to the server and publishes some data to a sample sensor.
 * In this case we're getting info from the system with the runtime properties object
 * 
 */
@Controller
public class SamplesController {

  private final Logger logger = LoggerFactory.getLogger(SamplesController.class);

  private static final String VIEW_SAMPLES_RESPONSE = "samples";

  @Autowired
  private PlatformTemplate platformTemplate;

  @Resource
  private Properties samplesProperties;

  @RequestMapping(value = {"/", "/home"})
  public String runSamples(final Model model) {

    // All this data must be created in the Catalog Application before start this
    // sample execution. At least the application identity token id and the provider id must be
    // declared in system twice
    String restClientIdentityKey = samplesProperties.getProperty("rest.client.identityKey");
    String providerId = samplesProperties.getProperty("rest.client.provider");

    // For this example we have created a generic component with a status sensor that accepts text
    // type observations, only for test purpose
    String componentId = samplesProperties.getProperty("rest.client.component");
    String sensorId = samplesProperties.getProperty("rest.client.sensor");

    logger.info("Starting samples execution...");

    String observationsValue = null;
    String errorMessage = null;

    try {
      // Get some system data from runtime
      Runtime runtime = Runtime.getRuntime();
      NumberFormat format = NumberFormat.getInstance();
      StringBuilder sb = new StringBuilder();
      long maxMemory = runtime.maxMemory();
      long allocatedMemory = runtime.totalMemory();
      long freeMemory = runtime.freeMemory();

      sb.append("free memory: " + format.format(freeMemory / 1024) + "<br/>");
      sb.append("allocated memory: " + format.format(allocatedMemory / 1024) + "<br/>");
      sb.append("max memory: " + format.format(maxMemory / 1024) + "<br/>");
      sb.append("total free memory: " + format.format((freeMemory + (maxMemory - allocatedMemory)) / 1024) + "<br/>");

      // In this case, we're getting CPU status in text mode
      observationsValue = sb.toString();

      logger.info("Observations values: " + observationsValue);

      // Create the sample sensor, only if it doesn't exists in the catalog
      createSensorIfNotExists(restClientIdentityKey, providerId, componentId, sensorId);

      // Publish observations to the sample sensor
      sendObservations(restClientIdentityKey, providerId, componentId, sensorId, observationsValue);
    } catch (Exception e) {
      logger.error("Error publishing sensor observations: " + e.getMessage(), e);
      errorMessage = e.getMessage();
    }

    logger.info("Samples execution ended!");

    model.addAttribute("restClientIdentityKey", restClientIdentityKey);
    model.addAttribute("providerId", providerId);
    model.addAttribute("componentId", componentId);
    model.addAttribute("sensorId", sensorId);
    model.addAttribute("observations", observationsValue);

    ObjectMapper mapper = new ObjectMapper();

    try {
      if (errorMessage != null && errorMessage.length() > 0) {
        Object json = mapper.readValue(errorMessage, Object.class);
        model.addAttribute("errorMsg", mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
      } else {
        model.addAttribute("successMsg", "Observations sended successfully");
      }
    } catch (Exception e) {
      logger.error("Error parsing JSON: {}", e.getMessage(), e);
      errorMessage += (errorMessage.length() > 0) ? "<br/>" : "" + e.getMessage();
      model.addAttribute("errorMsg", errorMessage);
    }

    return VIEW_SAMPLES_RESPONSE;
  }

  /**
   * Retrieve catalog information about the sample provider. If the component and/or sensor doesn't
   * exists, then let create they
   * 
   * @param identityToken Samples Application identity token for manage the rest connections
   * @param providerId Samples provider id
   * @param componentId Samples component id
   * @param sensorId Samples sensor id
   * @return {@link CatalogOutputMessage} object with provider's catalog data
   */
  private CatalogOutputMessage createSensorIfNotExists(String identityToken, String providerId, String componentId, String sensorId) {
    List<String> sensorsIdList = new ArrayList<String>();
    sensorsIdList.add(sensorId);

    // Create a CatalogInputMessage object for retrieve server data
    CatalogInputMessage getSensorsInputMsg = new CatalogInputMessage();
    getSensorsInputMsg.setProviderId(providerId);
    getSensorsInputMsg.setIdentityToken(identityToken);
    getSensorsInputMsg.setComponents(createComponentsList(componentId));
    getSensorsInputMsg.setSensors(createSensorsList(providerId, componentId, sensorsIdList));

    // Obtain the sensors list from provider within a CatalogOutputMessage response object type
    CatalogOutputMessage getSensorsOutputMsg = platformTemplate.getCatalogOps().getSensors(getSensorsInputMsg);

    // Search for the sensor in the list
    boolean existsSensor = false;
    if (getSensorsOutputMsg.getProviders() != null && !getSensorsOutputMsg.getProviders().isEmpty()) {
      for (AuthorizedProvider provider : getSensorsOutputMsg.getProviders()) {
        if (provider.getSensors() != null && !provider.getSensors().isEmpty()) {
          for (CatalogSensor sensor : provider.getSensors()) {
            logger.debug("Retrieved sensor: " + sensor.getComponent() + " - " + sensor.getSensor());
            existsSensor |= sensorId.equals(sensor.getSensor());
            if (existsSensor) {
              break;
            }
          }
        }
      }
    }

    // If the sensor doesn't exists in the retrieved list, we must create it before publish the
    // observations
    if (!existsSensor) {
      // Create a CatalogInputMessage object for retrieve server data
      CatalogInputMessage registerSensorsInputMsg = new CatalogInputMessage(providerId);
      registerSensorsInputMsg.setIdentityToken(identityToken);
      registerSensorsInputMsg.setComponents(createComponentsList(componentId));
      registerSensorsInputMsg.setSensors(createSensorsList(providerId, componentId, sensorsIdList));

      // Register the new sensor in the server
      platformTemplate.getCatalogOps().registerSensors(registerSensorsInputMsg);
    }

    return getSensorsOutputMsg;
  }

  /**
   * Publish some observations from a sensor
   * 
   * @param identityToken Samples Application identity token for manage the rest connections
   * @param providerId Samples provider id
   * @param componentId Samples component id
   * @param sensorId Samples sensor id
   * @param value Observations value, in our case, a String type
   */
  private void sendObservations(String identityToken, String providerId, String componentId, String sensorId, String value) {
    List<String> sensorsIdList = new ArrayList<String>();
    sensorsIdList.add(sensorId);
    createSensorsList(providerId, componentId, sensorsIdList);

    List<Observation> observations = new ArrayList<Observation>();
    Observation observation = new Observation(value, new Date());
    observations.add(observation);

    SensorObservations sensorObservations = new SensorObservations(sensorId);
    sensorObservations.setObservations(observations);

    DataInputMessage dataInputMessage = new DataInputMessage(providerId, sensorId);
    dataInputMessage.setIdentityToken(identityToken);
    dataInputMessage.setSensorObservations(sensorObservations);

    platformTemplate.getDataOps().sendObservations(dataInputMessage);
  }

  /**
   * Create a component list
   * 
   * @param componentId Component identifier
   * @return A {@link CatalogComponent} list
   */
  private List<CatalogComponent> createComponentsList(String componentId) {
    List<CatalogComponent> catalogComponentList = new ArrayList<CatalogComponent>();
    CatalogComponent catalogComponent = new CatalogComponent();
    catalogComponent.setComponent(componentId);
    catalogComponent.setComponentType(samplesProperties.getProperty("rest.client.component.type"));
    catalogComponent.setLocation(samplesProperties.getProperty("rest.client.component.location"));
    catalogComponentList.add(catalogComponent);
    return catalogComponentList;
  }

  /**
   * Create a sensor list
   * 
   * @param componentId The Sample Component Id
   * @param sensorsIdList A list with the sensor ids to create
   * @return A {@link CatalogSensor} list
   */
  private List<CatalogSensor> createSensorsList(String providerId, String componentId, List<String> sensorsIdList) {
    List<CatalogSensor> catalogSensorsList = new ArrayList<CatalogSensor>();
    for (String sensorId : sensorsIdList) {
      CatalogSensor catalogSensor = new CatalogSensor();
      catalogSensor.setComponent(componentId);
      catalogSensor.setSensor(sensorId);
      catalogSensor.setProvider(providerId);
      catalogSensor.setType(samplesProperties.getProperty("rest.client.sensor.type"));
      catalogSensor.setDataType(samplesProperties.getProperty("rest.client.sensor.dataType"));
      catalogSensor.setLocation(samplesProperties.getProperty("rest.client.sensor.location"));
      catalogSensorsList.add(catalogSensor);
    }
    return catalogSensorsList;
  }
}
