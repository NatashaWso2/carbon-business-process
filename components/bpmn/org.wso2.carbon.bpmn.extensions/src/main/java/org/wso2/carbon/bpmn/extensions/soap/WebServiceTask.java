/**
 *  Copyright (c) 2015 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.bpmn.extensions.soap;

import org.activiti.engine.delegate.BpmnError;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.FixedValue;
import org.activiti.engine.impl.el.JuelExpression;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.service.model.ServiceInfo;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Dispatch;
import javax.xml.ws.Service;
import javax.xml.ws.soap.SOAPBinding;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

/**
 Provides SOAP service invocation support within BPMN processes.
 The WebService task which is implemented as a BPMN extension to the Service task invokes the SOAP web service given by
 “serviceURL” parameter. "serviceURL" parameter can be used to give a URL of a SOAP service endpoint.
 It invokes the operation of the invoked web service given by the “method” parameter.
 The input/request payload can be provided using the "input" parameter.
 Output received  from the SOAP service invocation will be assigned to a process variable (as raw content) or parts of the output can be mapped to different process variables.
 The parameters that should be specified by the user :
 - serviceURL : URL of a SOAP service endpoint
 - method : method to be invoked
 - input : request payload
 - outputVariable : process variable to save the response
 The response of the SOAP service invocation can be retrieved from the variable specified above in the succession steps of the workflow. The class name of the Soap client implemented as an extension is given as the class
 name to the Service task i.e.  "org.wso2.carbon.bpmn.extensions.soap.WebServiceTask".
 Given below is an example on how the parameters are specified in the WebService task:
     <serviceTask id="servicetask1" name="Service Task" activiti:class="org.wso2.carbon.bpmn.extensions.soap.WebServiceTask">
     <extensionElements>
     <activiti:field name="serviceURL">
     <activiti:expression><![CDATA[${serviceURL}]]></activiti:expression>
     </activiti:field>
     <activiti:field name="method">
     <activiti:expression><![CDATA[${method}]]></activiti:expression>
     </activiti:field>
     <activiti:field name="input">
     <activiti:expression><![CDATA[${input}]]></activiti:expression>
     </activiti:field>
     <activiti:field name="outputVariable">
     <activiti:string><![CDATA[outputVariable]]></activiti:string>
     </activiti:field>
     </extensionElements>
     </serviceTask>
 From the parameters specified by the user, the “serviceURL”, “method” and “input” are given as JuelExpressions i.e. the values for those variables are passed as expressions since they can have dynamic values.
     <activiti:field name="serviceURL">
     <activiti:expression><![CDATA[${serviceURL}]]></activiti:expression>
     </activiti:field>
 “outputVariable” is a fixed value since it contains the response of the SOAP service invocation.
 */
public class WebServiceTask implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(WebServiceTask.class);
    private static final String SOAP_INVOKE_ERROR = "WebServiceInvokeError";
    private JuelExpression serviceURL;
    private JuelExpression method;
    private JuelExpression input;
    private FixedValue outputVariable;

    @Override
    public void execute(DelegateExecution execution) {

        if (log.isDebugEnabled()) {
            log.debug("Executing WebServiceTask " + serviceURL.getValue(execution).toString() + " - " + method.getValue(execution).toString());
        }

        String endpointURL = null;
        String methodName = null;
        String request = null;

        try {
            if (serviceURL != null) {
                endpointURL = serviceURL.getValue(execution).toString();
            } else {
                String urlNotFoundErrorMsg = "Service URL is not provided. serviceURL must be provided.";
                throw new BPMNSOAPException(urlNotFoundErrorMsg);
            }
            if (method != null) {
                methodName = method.getValue(execution).toString();
            } else {
                String methodNotFoundErrorMsg = "Method name that should be invoked is not provided. method must be provided.";
                throw new BPMNSOAPException(methodNotFoundErrorMsg);
            }
            if (input != null) {
                request = input.getValue(execution).toString();
            } else {
                String inputNotFoundErrorMsg = "Service URL is not provided. serviceURL must be provided.";
                throw new BPMNSOAPException(inputNotFoundErrorMsg);
            }
            /**
             *  Create the JaxWsDynamicClientFactory to walk through the CXF service model
             *  i.e. wsdl
             */
            JaxWsDynamicClientFactory factory = BPMNSOAPDynamicClient.getInstance();
            URL wsdlURL = new URL(endpointURL);
            Client client = factory.createClient(wsdlURL.toExternalForm());
            ClientImpl clientImpl = (ClientImpl) client;
            Endpoint endpoint = clientImpl.getEndpoint();
            ServiceInfo serviceInfo = endpoint.getService().getServiceInfos().get(0);

            String targetNS = serviceInfo.getTargetNamespace();
            String portN = endpoint.getEndpointInfo().getName().getLocalPart();
            String wsdlEndpointUrl = endpoint.getEndpointInfo().getAddress();
            QName operationName = new QName(targetNS, methodName);
            QName portName = new QName(targetNS, portN);

            try {
                /**
                 *  Define the service.
                 */

                Service svc = Service.create(operationName);
                svc.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, wsdlEndpointUrl);

                /**
                 *  Create the dynamic invocation object from this service.
                 */
                Dispatch<Source> dispatch = svc.createDispatch(portName, Source.class, Service.Mode.MESSAGE);
                ByteArrayInputStream bais = new ByteArrayInputStream(request.getBytes());
                Source input = new StreamSource(bais);

                /**
                 *  Invoking WebService
                 */
                Source response = dispatch.invoke(input);

                /**
                 *  Process the response.
                 */
                StreamResult result = new StreamResult(new ByteArrayOutputStream());
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.transform(response, result);
                ByteArrayOutputStream baos = (ByteArrayOutputStream) result.getOutputStream();

                /**
                 *  Write out the response content to string.
                 */
                String responseStr = new String(baos.toByteArray());

                if (outputVariable != null) {
                    String outVarName = outputVariable.getValue(execution).toString();
                    execution.setVariable(outVarName, responseStr);
                    System.out.println("XML -- >  " + responseStr);
                } else {
                    String outputNotFoundErrorMsg = "Output variable is not provided. outputVariable must be provided to save " +
                            "the response.";
                    throw new BPMNSOAPException(outputNotFoundErrorMsg);
                }

            } catch (TransformerConfigurationException e) {
                String transfomerConfigEx = "Configuration error";
                throw new BPMNSOAPException(transfomerConfigEx);
            } catch (TransformerFactoryConfigurationError e) {
                String transfomerFactoryConfigEx = "Exception when configuring the Transformer Factories. " +
                        "Class of a transformation factory specified in the system properties cannot be found or instantiated";
                throw new BPMNSOAPException(transfomerFactoryConfigEx);
            } catch (TransformerException e) {
                String transformerEx = "Exception during the transformation process";
                throw new BPMNSOAPException(transformerEx);
            }

        } catch (Exception e) {
            String msg = "Exception occured when walking through the CXF service model." +
                    " Cause for the exception : " + e.getMessage();
            log.error(msg, e);

        }
    }

    public void setServiceURL(JuelExpression serviceURL) {
        this.serviceURL = serviceURL;
    }

    public void setMethod(JuelExpression method) {
        this.method = method;
    }

    public void setInput(JuelExpression input) {
        this.input = input;
    }

    public void setOutputVariable(FixedValue outputVariable) {
        this.outputVariable = outputVariable;
    }
}
