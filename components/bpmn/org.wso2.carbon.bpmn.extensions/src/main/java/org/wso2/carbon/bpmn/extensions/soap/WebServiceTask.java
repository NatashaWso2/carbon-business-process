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
 * Provides SOAP service invocation support within BPMN processes. It invokes the SOAP service/operation specified
 * by "operation" of the given "wsdl".
 * <p/>
 * Input payload/request body can be provided using the "request" parameter. The entire SOAP request message
 * has to be provided.
 * <p/>
 * <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:unit="http://ode/bpel/unit-test.wsdl">
 * " +
 * "   <soapenv:Header/>\n" +
 * "   <soapenv:Body>\n" +
 * "      <unit:hello>\n" +
 * "         <TestPart>Hello</TestPart>\n" +
 * "      </unit:hello>\n" +
 * "   </soapenv:Body>\n" +
 * "</soapenv:Envelope>
 * <p/>
 * Output received from the SOAP service will be assigned to a process variable (as raw content)
 * or parts of the output can be mapped to different process variables.
 * The response type i.e. the name of the output to be assigned/mapped to the response variable of the SOAP response message
 * has to be provided in "responseType".
 * <p/>
 * helloResponse.TestPart
 * <p/>
 * If a failure occurs in WebService task, a BPMN error with error code "WebServiceInvokeError" will
 * be thrown. BPMN process can catch this error using an Error Boundary Event associated
 * with the SOAP service task.
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

        try {

            String endpointURL = serviceURL.getValue(execution).toString();
            String methodName = method.getValue(execution).toString();
            String request = input.getValue(execution).toString();

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
                String outVarName = outputVariable.getValue(execution).toString();
                execution.setVariable(outVarName, responseStr);
                System.out.println(responseStr);

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
                    " Cause for the exception : "  +e.getMessage();
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
