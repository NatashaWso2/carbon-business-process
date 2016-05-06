/**
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.bpmn.core.internal;


import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngines;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.task.Task;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jndi.JNDIContextManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.bpmn.core.ActivitiEngineBuilder;
import org.wso2.carbon.bpmn.core.BPMNConstants;
import org.wso2.carbon.bpmn.core.BPMNEngineService;
import org.wso2.carbon.bpmn.core.BPMNServerHolder;
import org.wso2.carbon.bpmn.core.db.DataSourceHandler;
import org.wso2.carbon.bpmn.core.deployment.BPMNDeployer;
import org.wso2.carbon.datasource.core.api.DataSourceManagementService;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.datasource.core.exception.DataSourceException;
import org.wso2.carbon.deployment.engine.Artifact;
import org.wso2.carbon.deployment.engine.ArtifactType;
import org.wso2.carbon.security.caas.user.core.service.RealmService;

import javax.naming.Context;
import javax.naming.NamingException;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * BPMN Service Component.
 */

@Component(
        name = "org.wso2.carbon.bpmn.core.internal.BPMNServiceComponent",
        service = BPMNEngineService.class,
        immediate = true)

public class BPMNServiceComponent {

    private static final Logger log = LoggerFactory.getLogger(BPMNServiceComponent.class);
    private DataSourceService datasourceService;
    private DataSourceManagementService datasourceManagementService;
    private JNDIContextManager jndiContextManager;
    private BundleContext bundleContext;

  //  Set CarbonRealmService
    @Reference(
            name = "org.wso2.carbon.security.CarbonRealmServiceImpl",
            service = RealmService.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterCarbonRealm"
    )
    public void registerCarbonRealm(RealmService carbonRealmService) {
        log.info("register CarbonRealmService...");
        IdentityDataHolder.getInstance().registerCarbonRealmService(carbonRealmService);
    }

    public void unregisterCarbonRealm(RealmService carbonRealmService) {
        log.info("Unregister CarbonRealmService...");
    }

    @Reference(
            name = "org.wso2.carbon.datasource.jndi.JNDIContextManager",
            service = JNDIContextManager.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unRegisterJNDIContext")

    public void registerJNDIContext(JNDIContextManager contextManager) {
        log.info("register JNDI Context");
        this.jndiContextManager = contextManager;
    }

    public void unRegisterJNDIContext(JNDIContextManager contextManager) {
        log.info("Unregister JNDI Context");
    }

    @Reference(
            name = "org.wso2.carbon.datasource.core.api.DataSourceService",
            service = DataSourceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unRegisterDataSourceService")
    public void registerDataSourceService(DataSourceService datasource) {
        log.info("register Datasource service");
        this.datasourceService = datasource;
    }

    public void unRegisterDataSourceService(DataSourceService datasource) {
        log.info("unregister datasource service");
    }

    @Reference(
            name = "org.wso2.carbon.datasource.core.api.DataSourceManagementService",
            service = DataSourceManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unRegisterDataSourceManagementService")

    public void registerDataSourceManagementService(
            DataSourceManagementService datasourceMgtService) {
        log.info("register Datasource Management service");
        this.datasourceManagementService = datasourceMgtService;
    }

    public void unRegisterDataSourceManagementService(DataSourceManagementService datasource) {
        log.info("unregister datasource service");
    }

    @Activate
    protected void activate(ComponentContext ctxt) {
        log.info("BPMN core component activator...");
        try {
            this.bundleContext = ctxt.getBundleContext();
            registerJNDIContextForActiviti();
            BPMNServerHolder holder = BPMNServerHolder.getInstance();
            ActivitiEngineBuilder.getInstance();
            holder.setEngine(ActivitiEngineBuilder.getInstance().buildEngine());
            BPMNEngineServiceImpl bpmnEngineService = new BPMNEngineServiceImpl();
            bpmnEngineService
                    .setProcessEngine(ActivitiEngineBuilder.getInstance().getProcessEngine());
            bpmnEngineService.setCarbonRealmService(IdentityDataHolder.getInstance().getCarbonRealmService());
            bundleContext
                    .registerService(BPMNEngineService.class.getName(), bpmnEngineService, null);
            // Create metadata table for deployments
            DataSourceHandler dataSourceHandler = new DataSourceHandler();
            dataSourceHandler
                    .initDataSource(ActivitiEngineBuilder.getInstance().getDataSourceJndiName());
            dataSourceHandler.closeDataSource();

            BPMNDeployer customDeployer = new BPMNDeployer();
            customDeployer.init();
            File ab = new File("/home/natasha/Documents/SoapInvoker.bar");
            Artifact artifact = new Artifact(ab);
            ArtifactType artifactType = new ArtifactType<>("bar");
            artifact.setKey("SoapInvoker.bar");
            artifact.setType(artifactType);
            customDeployer.deploy(artifact);
            log.info("Artifact Deployed");

            ProcessEngine eng = bpmnEngineService.getProcessEngine();
            RuntimeService runtimeService = eng.getRuntimeService();

            Map<String, Object> taskVariables = new HashMap<>();
            taskVariables.put("serviceURL", "http://10.100.4.192:9763/services/HelloService?wsdl");
            taskVariables.put("method", "hello");
            taskVariables.put("input", "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:unit=\"http://ode/bpel/unit-test.wsdl\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <unit:hello>\n" +
                    "         <TestPart>Hello</TestPart>\n" +
                    "      </unit:hello>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>");
            runtimeService.startProcessInstanceByKey("myProcess", taskVariables);

            log.info("Process Instance started");

            TaskService taskService = eng.getTaskService();
            List<Task> tasks = taskService.createTaskQuery().processDefinitionKey("myProcess").list();
            if (tasks != null && tasks.size() > 0) {
                Task task = tasks.get(0);
                System.out.println(task.getName());
            }

            // Calling the HelloWorld Soap Service
          /*  BPMNDeployer customDeployer = new BPMNDeployer();
            customDeployer.init();
            File ab = new File ("/home/natasha/Documents/soapService.bar");
            Artifact artifact = new Artifact(ab);
            ArtifactType artifactType = new ArtifactType<>("bar");
            artifact.setKey("soapService.bar");
            artifact.setType(artifactType);
            customDeployer.deploy(artifact);
            log.info("Artifact Deployed");

            ProcessEngine eng = bpmnEngineService.getProcessEngine();
            RuntimeService runtimeService = eng.getRuntimeService();
            runtimeService.startProcessInstanceByKey("myProcess");
            log.info("Process Instance started");

            TaskService taskService = eng.getTaskService();
            List<Task> tasks = taskService.createTaskQuery().processDefinitionKey("myProcess").list();

            Task task = tasks.get(0);

            log.info("1st User Task -- >"  +task.getName());
            Map<String, Object> taskVariables = new HashMap<>();
            taskVariables.put("wsdl", "http://10.100.4.192:9763/services/HelloService?wsdl");
            taskVariables.put("operation", "hello");
            taskVariables.put("request", "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:unit=\"http://ode/bpel/unit-test.wsdl\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <unit:hello>\n" +
                    "         <TestPart>Hello</TestPart>\n" +
                    "      </unit:hello>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>");
            //taskVariables.put("responseType", "helloResponse.TestPart");

            taskService.complete(task.getId(), taskVariables);
            log.info("1st User Task --> COMPLETED");

            tasks = taskService.createTaskQuery().processDefinitionKey("myProcess").list();
            Task task2 = tasks.get(0);
            log.info("2nd User Task -- >"  +task2.getName());
            log.info("2nd User Task Desc -- > "+task2.getDescription());
            taskService.complete(task2.getId());
            log.info("2nd User Task --> COMPLETED");*/


            // Calling the HelloWorld Soap Service
            /*BPMNDeployer customDeployer = new BPMNDeployer();
            customDeployer.init();
            File ab = new File ("/home/natasha/Documents/webService.bar");
            Artifact artifact = new Artifact(ab);
            ArtifactType artifactType = new ArtifactType<>("bar");
            artifact.setKey("webService.bar");
            artifact.setType(artifactType);
            log.info("Atrifact Created");
            customDeployer.deploy(artifact);
            log.info("Artifact Deployed");

            ProcessEngine eng = bpmnEngineService.getProcessEngine();
            RuntimeService runtimeService = eng.getRuntimeService();
            //ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("processId");
            log.info("created");
            runtimeService.startProcessInstanceByKey("webServiceInvocation");
            //runtimeService.startProcessInstanceById("processId");
            log.info("started");*/


            //Calling the AdderProcess SOAP Service

           /* BPMNDeployer customDeployer = new BPMNDeployer();
            customDeployer.init();
            File ab = new File ("/home/natasha/Documents/activitiWebServiceTest.bar");
            Artifact artifact = new Artifact(ab);
            ArtifactType artifactType = new ArtifactType<>("bar");
            artifact.setKey("activitiWebServiceTest.bar");
            artifact.setType(artifactType);
            log.info("Atrifact Created");
            customDeployer.deploy(artifact);
            log.info("Artifact Deployed");

            ProcessEngine eng = bpmnEngineService.getProcessEngine();
            RuntimeService runtimeService = eng.getRuntimeService();
            log.info("created");
            runtimeService.startProcessInstanceByKey("testWebServiceInvocation");
            log.info("started");*/

            /*Gets Deployed. But when starting the activitiWebServiceTest.bar --->
            * ERROR {org.wso2.carbon.bpmn.core.internal.BPMNServiceComponent} - Error initializing bpmn component java.lang.NullPointerException java.lang.NullPointerException
            at org.activiti.engine.impl.bpmn.data.ItemDefinition.createInstance(ItemDefinition.java:44)
            at org.activiti.engine.impl.bpmn.webservice.MessageDefinition.createInstance(MessageDefinition.java:37)
            at org.activiti.engine.impl.bpmn.behavior.WebServiceActivityBehavior.execute(WebServiceActivityBehavior.java:68)
            at org.activiti.engine.impl.pvm.runtime.AtomicOperationActivityExecute.execute(AtomicOperationActivityExecute.java:60)
            at org.activiti.engine.impl.interceptor.CommandContext.performOperation(CommandContext.java:97)
            at org.activiti.engine.impl.persistence.entity.ExecutionEntity.performOperationSync(ExecutionEntity.java:633)
            at org.activiti.engine.impl.persistence.entity.ExecutionEntity.performOperation(ExecutionEntity.java:628)
            at org.activiti.engine.impl.pvm.runtime.AtomicOperationTransitionNotifyListenerStart.eventNotificationsCompleted(AtomicOperationTransitionNotifyListenerStart.java:52)
               */


        } catch (Throwable t) {
            log.error("Error initializing bpmn component " + t, t);
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctxt) {
        log.info("Stopping the BPMN core component...");
        ProcessEngines.destroy();
    }

    private void registerJNDIContextForActiviti() throws DataSourceException, NamingException {
        //DataSourceMetadata activitiDB = datasourceManagementService.getDataSource(BPMNConstants.BPMN_DB_NAME);
        //JNDIConfig jndiConfig = activitiDB.getJndiConfig();
        Context context = jndiContextManager.newInitialContext();

        Context subcontext = context.createSubcontext("java:comp/jdbc");
        subcontext.bind(BPMNConstants.BPMN_DB_CONTEXT_NAME,
                datasourceService.getDataSource(BPMNConstants.BPMN_DB_NAME));
    }

}

