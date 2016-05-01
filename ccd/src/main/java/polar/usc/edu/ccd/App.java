package polar.usc.edu.ccd;

import java.io.IOException;
import java.util.ArrayList;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class App 
{
    public void feedsolr( String json )
    {
    	ObjectMapper m = new ObjectMapper();
    	String title;
		JsonNode data = null;
		try {
			data = m.readTree(json);
		} catch (JsonProcessingException e) {
			System.out.println("JsonProcessingException");
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			System.out.println("IOException");
		}
		
		ObjectNode final_doc = m.createObjectNode();
		//Copy all common and simple ones:
		final_doc.put("url", data.get("request").asText());
		title = data.get("request").asText();
		
		if(data.get("id")==null || (data.get("id")!=null && data.get("id").asText().isEmpty() ) ){
			final_doc.put("id", assignShorturl(title));
		}
		else {
			final_doc.put("id", data.get("id").asText());
		}
    			
		if(data.get("Content-Type")!= null)
			final_doc.put("Content-Type", data.get("Content-Type").asText());
		else{
			System.out.println("");
		}
		
		if(data.get("text_tagratio")!= null && !data.get("text_tagratio").asText().isEmpty())
			final_doc.put("text_tagratio", data.get("text_tagratio").asText());
    			
		if(data.get("NER_UNITS") != null && data.get("NER_UNITS").size() >0)
			final_doc.putPOJO("ner_units", (ArrayNode)data.get("NER_UNITS"));
		
		if(data.get("NER_NAMES") != null && data.get("NER_NAMES").size() >0)
			final_doc.putPOJO("ner_names", (ArrayNode)data.get("NER_NAMES"));
		
		//evaluation keys
		if(data.get("lang") != null)
			final_doc.putPOJO("lang", data.get("lang").asText());
		
//		if(data.get("request") != null)
//			final_doc.putPOJO("request", data.get("request").asText());
		if(data.get("server") != null)
			final_doc.putPOJO("server", data.get("server").asText());
		if(data.get("status") != null)
			final_doc.putPOJO("status", data.get("status").asText());
		if(data.get("file_size") != null)
			final_doc.putPOJO("file_size", data.get("file_size").asText());
		if(data.get("parser_details") != null && ((ArrayNode)data.get("parser_details")).size() >0)
			final_doc.putPOJO("parser_details", (ArrayNode)data.get("parser_details"));
		if(data.get("sweet") != null && ((ArrayNode)data.get("sweet")).size() >0)
			final_doc.putPOJO("sweet", (ArrayNode)data.get("sweet"));
		
		
		if(data.get("Geographic_LATITUDE") != null && data.get("Geographic_LONGITUDE") != null){
			final_doc.put("geotopic_latlon", data.get("Geographic_LATITUDE").asText() + "," + data.get("Geographic_LONGITUDE").asText());
			final_doc.put("Geographic_LATITUDE", data.get("Geographic_LATITUDE").asText());
			final_doc.put("Geographic_LONGITUDE", data.get("Geographic_LONGITUDE").asText());
		}
			
		if(data.get("Geographic_NAME") != null) {
			final_doc.put("geotopic_location", data.get("Geographic_NAME").asText());
			final_doc.put("Geographic_NAME", data.get("Geographic_NAME").asText());
		}
		
		ArrayList<String> locations = new ArrayList<String>();
		ArrayList<String> locations_latlon = new ArrayList<String>();
		for(int i =1; i<10; i++){
			if(data.get("Optional_LATITUDE" + i) != null && data.get("Optional_LONGITUDE" + i) != null && data.get("Optional_NAME"+i) != null) { 
				locations_latlon.add(data.get("Optional_LATITUDE" +i).asText() + "," + data.get("Optional_LONGITUDE" + i).asText());
				locations.add(data.get("Optional_NAME"+i).asText());
			}
			else {
				break;
			}
			
		}

		if(locations.size() >0) {
			final_doc.putPOJO("locations", locations );
			final_doc.putPOJO("locations_latlon", locations_latlon );
		}
    			
		if(data.get("grobid:header_TEIJSONSource") != null)
			final_doc.put("grobid:header_TEIJSONSource", data.get("grobid:header_TEIJSONSource").asText());	
		if(data.get("meta:author")!= null)
			final_doc.put("meta:author", data.get("meta:author").asText());
		if(data.get("grobid:header_Authors")!= null)
			final_doc.put("grobid:header_Authors", data.get("grobid:header_Authors").asText());
		if(data.get("grobid:header_Affiliation")!= null)
			final_doc.put("grobid:header_Affiliation", data.get("grobid:header_Affiliation").asText());
		if(data.get("grobid:header_Title")!= null)
			final_doc.put("grobid:header_Title", data.get("grobid:header_Title").asText());
		
		//common to pdf, xml, xhtml
		if(data.get("xmpTPg:NPages")!= null)
			final_doc.put("xmpTPg:NPages", data.get("xmpTPg:NPages").asText());
		if(data.get("xmp:CreatorTool")!= null)
			final_doc.put("xmp:CreatorTool", data.get("xmp:CreatorTool").asText());
		if(data.get("dc:title")!= null)
			final_doc.put("dc:title", data.get("dc:title").asText());
		if(data.get("Content-Encoding")!= null)
			final_doc.put("Content-Encoding", data.get("Content-Encoding").asText());
    			
		String body ="";
		try {
			final_doc.put("json_size", m.writeValueAsBytes(final_doc).length);
			body = m.writeValueAsString(final_doc);
			
			body = "{\"add\": { \"doc\":" + body + "}}";
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		if(!body.isEmpty()) {
			executeShellCommand(body);
		}
    		
    }
   
	private String assignShorturl(String title)  {
		System.out.println("generate id wait 10s");
		try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		// TODO Auto-generated method stub
    	Response response = WebClient
                .create( "http://polar.usc.edu/yourls-api.php?action=shorturl&title="+ title +"&format=json&url=" + title)
                .accept(MediaType.APPLICATION_JSON)
                .get();
    	
    	String resp = response.readEntity(String.class);
        ObjectMapper mymapper = new ObjectMapper();
        ObjectNode yourls=null;
        
        if(!resp.startsWith("<")) {
			try {
				yourls = (ObjectNode) mymapper.readTree(resp);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
				System.out.println("JsonProcessingException feedsolr");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.out.println("IOException feedsolr");
			}
		
			return yourls.get("shorturl").asText();
        } else {
        	System.out.println("feedsolr wait 7s");
        	try {
    			Thread.sleep(7000);
    		} catch (InterruptedException e1) {
    			// TODO Auto-generated catch block
    			e1.printStackTrace();
    		}
        	return assignShorturl(title);
        }
	}
	
	private void executeShellCommand(String doc){		
			Response response = WebClient
	                .create( "http://localhost:8983/solr/collection2/update?wt=json&commit=true&overwrite=true")
	                .type(MediaType.APPLICATION_JSON)
	                .post(doc);
			String r = response.readEntity(String.class);
	}

}
