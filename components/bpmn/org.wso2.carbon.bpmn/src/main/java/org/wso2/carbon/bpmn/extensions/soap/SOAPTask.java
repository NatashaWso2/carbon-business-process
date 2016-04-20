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

/*import com.jayway.jsonpath.JsonPath;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.ClientImpl;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory;
import org.apache.cxf.service.model.ServiceInfo;
import org.json.XML;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.bpmn.core.internal.BusInstanceHolder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
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
import javax.xml.ws.soap.SOAPBinding;*/


/**
 * SOAP client using CXF.
 */
public class SOAPTask {
   /* private static final Logger log = LoggerFactory.getLogger(SOAPTask.class);
    public void executeSOAPClient() throws Exception {
        try {
            String inputMsg = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\"" +
                    " xmlns:add=\"http://wso2.org/wso2con/2011/sample/adder\">\n" +
                    "   <soapenv:Header/>\n" +
                    "   <soapenv:Body>\n" +
                    "      <add:AdderProcessRequest>\n" +
                    "         <add:a>100</add:a>\n" +
                    "         <add:b>24</add:b>\n" +
                    "      </add:AdderProcessRequest>\n" +
                    "   </soapenv:Body>\n" +
                    "</soapenv:Envelope>";

            String responseMessageType = null;
            Bus bus = BusInstanceHolder.getInstance().getBusInstance();
            JaxWsDynamicClientFactory factory = JaxWsDynamicClientFactory.newInstance(bus);
            URL wsdlURL = new URL("http://10.100.4.192:9763/services/AdderProcessService?wsdl");

            Client client = factory.createClient(wsdlURL.toExternalForm());
            ClientImpl clientImpl = (ClientImpl) client;

            Endpoint endpoint = clientImpl.getEndpoint();
            ServiceInfo serviceInfo = endpoint.getService().getServiceInfos().get(0);

            String targetNS = serviceInfo.getTargetNamespace();
            String portN = endpoint.getEndpointInfo().getName().getLocalPart();

            String operation = "process";
            String wsdlEndpointUrl = endpoint.getEndpointInfo().getAddress();

            QName operationName = new QName(targetNS, operation);
            QName portName = new QName(targetNS, portN);

            String responseStr = null;
            try {

                Service svc = Service.create(operationName);
                svc.addPort(portName, SOAPBinding.SOAP11HTTP_BINDING, wsdlEndpointUrl);


                Dispatch<Source> dispatch = svc.createDispatch(portName, Source.class, Service.Mode.MESSAGE);

                ByteArrayInputStream bais = new ByteArrayInputStream(inputMsg.getBytes());
                Source input = new StreamSource(bais);

                Source response = dispatch.invoke(input);


                StreamResult result = new StreamResult(new ByteArrayOutputStream());
                Transformer trans = TransformerFactory.newInstance().newTransformer();
                trans.transform(response, result);
                ByteArrayOutputStream baos = (ByteArrayOutputStream) result.getOutputStream();

                responseStr = new String(baos.toByteArray());
                String str = removeXmlStringNamespaceAndPreamble(responseStr);
                String jsonStr = XML.toJSONObject(str).toString();
                responseMessageType = "AdderProcessResponse";
                String responseBody = JsonPath.read(jsonStr, "Envelope.Body."
                        + responseMessageType + ".result").toString();
                log.error(responseBody);

            } catch (TransformerConfigurationException e) {
               log.error(e.toString());
            } catch (TransformerFactoryConfigurationError e) {
                log.error(e.toString());
            } catch (TransformerException e) {
                log.error(e.toString());
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public static String removeXmlStringNamespaceAndPreamble(String xmlString) {
        return xmlString.replaceAll("(<\\?[^<]*\\?>)?", "").*//* remove preamble *//*
                replaceAll("xmlns.*?(\"|\').*?(\"|\')", "") *//* remove xmlns declaration *//*
                .replaceAll("(<)(\\w+:)(.*?>)", "$1$3") *//* remove opening tag prefix *//*
                .replaceAll("(</)(\\w+:)(.*?>)", "$1$3"); *//* remove closing tags prefix *//*
    }
*/

}
