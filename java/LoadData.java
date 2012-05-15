/*
Copyright (c) 2012 Morningstar, inc. All rights reserved.

 Redistribution and use of this software in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright notice, this list of
 conditions and the following disclaimer in the documentation and/or other materials provided
 with the distribution.
 * The name of Morningstar inc. may not be used to endorse or promote products derived from 
 this software without specific prior written permission of Morningstar inc.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
/**
 * Uploads data to a Morningstar Commodity Server using the WebServices REST API.
 * @Dependencies: the jersey-bundle and jsr311-api jar library files must be included in the java build path.
 * @author lfortes
 * @version 2012-04-05
 */

package webclient.examples;

import static javax.ws.rs.core.Response.Status.OK;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;

import javax.ws.rs.core.MediaType;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;

public class LoadData {
	
	//can also be specified as command line arguments
    private static String USERNAME = "joe@lim.com";
    private static String PASSWORD = "ILoveWS1";
    private static String BASE_URL = "http://ausqadev01.lim.com:9090";
    
    //Data upload URI
    private static final String DATA_LOAD_RSC_PATH = "rs/upload";

    private static final int HTTP_CODE_OK = OK.getStatusCode();
    
    //web client resource
    private WebResource webRsc;
 
    private long jobID;  
    
    /**
     * Constructor
     * Gets web client resource
     */
    public LoadData() {
		ClientConfig config = new DefaultClientConfig();
		Client client = Client.create(config);
        // Apply basic authentication.
        client.addFilter(new HTTPBasicAuthFilter(USERNAME, PASSWORD));
        // Define the base URL for the web resource.
        webRsc = client.resource(BASE_URL);
    }
    
    /**
     * usage
     * Required argument: xmlFileInput. 
     * Optional argument: output file name (XML response will be sent to file if given).
     * Optional argument: username.
     * Optional argument: password.
     * Optional argument: webserver.
     */
    private static void usage()
	{
    	System.out.println("Usage: " + LoadData.class.getName() +
    			" <xmlFileInput> [-out <XMLOutputFileName>] [-parser <parsername>]" +
    			" [-username <username>] [-password <password>] [-webserver <webserver>]");
    	System.out.println("Example: " + LoadData.class.getName() +	" /temp/yy.xml");
        System.exit(1);
	}    
    
	   /**
  * Send HTTP post request to server and returns XML String response
  * @param workload
  * @param parser
  * @return
  */
    String sendRequestAndGetServerResponse(File workload, String parser) throws UnsupportedEncodingException, ClientHandlerException, UniformInterfaceException, XMLStreamException, InterruptedException
    {
    	String response = null;
    	
        ClientResponse resp = webRsc.path(DATA_LOAD_RSC_PATH + "/").
    			queryParam("username", USERNAME).queryParam("parsername", parser).
    			accept(MediaType.TEXT_XML).type(MediaType.TEXT_XML).
    			post(ClientResponse.class, workload);
    	
    	
        if (HTTP_CODE_OK != resp.getStatus()) {
            // Present the error when something went wrong and bail out.
            throw new IllegalStateException("Initial request failed with response [" + resp + "]");
        }
        
        //Also check the response status attribute of the returned XML
        // a status code of less than 300 means job is still processing.
        response = resp.getEntity(String.class);
        while (getDataRequestResponseStatus(response) < 300) {
            // When the status indicates waiting-on-result, retry the request with ID.
            Thread.sleep(250);
            resp = webRsc.path(DATA_LOAD_RSC_PATH + "/jobreport/" + jobID).get(ClientResponse.class);
            if (HTTP_CODE_OK != resp.getStatus()) {
                throw new IllegalStateException("Error while polling for results [" + resp + "]");
            }
            response = resp.getEntity(String.class);
        }
        
        return response;       
    }    
    
	@SuppressWarnings("unchecked")
    private int getDataRequestResponseStatus(String response) throws XMLStreamException, UnsupportedEncodingException
    {
    	int status =0;
    	
    	//First create a new XMLInptFactory
    	XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    	//Setup a new eventReader
    	InputStream in = new ByteArrayInputStream(response.getBytes("UTF-8"));
    	XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
    	
    	//walk through XML document
    	while (eventReader.hasNext()) {
    		XMLEvent event = eventReader.nextEvent();
    		if (event.isStartElement()) {
    			StartElement startElement = event.asStartElement();
    			//If this is a response element, get status
    			if (startElement.asStartElement().getName().toString().endsWith("response")) {
					Iterator <Attribute> attributes = startElement.getAttributes();
    				while (attributes.hasNext()) {
    					Attribute attribute = attributes.next();
    					if (attribute.getName().toString().equals("intStatus"))
    						status = Integer.parseInt(attribute.getValue());
    					else if (attribute.getName().toString().equals("jobID"))
    						jobID = Integer.parseInt(attribute.getValue());
    					System.out.println(attribute.getName() + ": " + attribute.getValue());

    				}
    				break;
    			}
    			else if (startElement.asStartElement().getName().toString().endsWith("JobResultImpl")) {
    				//have final result!
    				status = 300;
    				break;
    			}

    		}
    	}
		return status;
    }

	/**
     * Parses LoadData XML response to console
     * @param response XML formated response
     */
    private static void parseLoadDataXMLResponseAndDisplayOnConsole(String response) throws UnsupportedEncodingException, XMLStreamException
    {
    	//First create a new XMLInptFactory
    	XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    	//Setup a new eventReader
    	InputStream in = new ByteArrayInputStream(response.getBytes("UTF-8"));
    	XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
    	
    	//walk through XML document
    	while (eventReader.hasNext()) {
    		XMLEvent event = eventReader.nextEvent();
    		if (event.isStartElement()) {
    			StartElement startElement = event.asStartElement();
    			if (startElement.asStartElement().getName().toString().endsWith("message")) {
    				event = eventReader.nextEvent();
    				System.out.println("\n" + event.asCharacters().getData());
    			}
    		}
    	}
    }    
    
    /**
     * prints string XML response to given file
     * @param fileName user specified output file
     * @param response server XML response
     * @return
     */
	private static void printXMLResponseToFile(String fileName, String response)
	{
		if (!fileName.contains(".xml") || !fileName.contains(".XML"))
			fileName.concat(".xml");
	    File f = new File(fileName);
		if(f.exists()) {
			f.delete();
		}
		PrintWriter pw;
		try {
			pw = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
			pw.println(response);
			pw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Done. output written to " + fileName);
	}        
    
    
    /**
	 * @param args
     * @throws XMLStreamException 
     * @throws UnsupportedEncodingException 
	 */
	public static void main(String[] args)  throws UnsupportedEncodingException, XMLStreamException, ClientHandlerException, UniformInterfaceException, InterruptedException  {
        if (args.length < 1) {
        	usage();
        }

        //user input arguments
	    String xmlFileInput;
	    String fileName = null;
	    String parser = "DefaultParser";
        
	    xmlFileInput = args[0];
	    for (int i = 1; i < args.length; i++) {
	    	if (args[i].equals("-out")) {
	    		i++;
	    		fileName = args[i];
	    	}
	    	else if (args[i].equals("-username")) {
	    		i++;
	    		USERNAME = args[i];
	    	}
	    	else if (args[i].equals("-password")) {
	    		i++;
	    		PASSWORD = args[i];
	    	}
	    	else if (args[i].equals("-webserver")) {
	    		i++;
	    		BASE_URL = args[i];
	    	}
	    	else if (args[i].equals("-parser")) {
	    		i++;
	    		parser = args[i];
	    	}
	    }
	    
	    LoadData c = new LoadData();
	    String response = null;

	    response = c.sendRequestAndGetServerResponse(new File(xmlFileInput), parser);
	    
	    if (fileName != null)
	    	//XML output file specified.
	    	printXMLResponseToFile(fileName, response);
	    else
	    	//Parse and display on console
		    parseLoadDataXMLResponseAndDisplayOnConsole(response);
	}
	    
}
