package polar.usc.edu.ccd;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import co.nstant.in.cbor.CborDecoder;
import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import polar.usc.edu.ccd.TTR;
import polar.usc.edu.ccd.ExecuteShellCommand;
import polar.usc.edu.ccd.Parsers;

public class Extract {

	private ObjectNode data; // Json object used for each file
	File body;
	File nltk;
	File geot;
	String request="";
	String server = "";
	String status = "";
	int text_size = 0;
	int meta_size = 0;
	HashSet<String> parser_names;
	ArrayList<String> sweet_entities;
	Parsers parsers_detail;
	ObjectMapper mapper;
	ArrayNode yourls;
	boolean empty=true;
	long file_size = 0;
	FileWriter yourls_left;
	long index_size = 0;
	App solr;
	int id=0;
	private Extract(){
		body = new File("body");
		nltk = new File("nltk.txt");
		geot = new File("geot.geot");
		mapper = new ObjectMapper();
    	solr = new App();
    	sweet_entities = new ArrayList<String>();
    	yourls_left=null;
    	yourls = mapper.createArrayNode();
	}
	public static void main(String[] args) {
		
		if(args.length !=0 ){
			System.out.println("Please enter path to input and output directory");
		}
		else{
//			String inputDir = args[0];
			String inputDir = "/Users/manali/599/ass3/application_pdf1/";
			if(!(new File(inputDir)).exists()){
				System.out.println("Input Directory path invalid");
				System.exit(0);
			}
			
	    	
			Extract  e = new Extract();
			e.extract_commoncrawl(inputDir);
		}
		
	}
	
	private void extract_commoncrawl(String inputDir) {
		// TODO Auto-generated method stub
		File dir = new File(inputDir);
		int count = 0;
		File sweet  = new File("sweet_entities_flat.json");
		
		try {
			ArrayNode sweetjson = (ArrayNode) mapper.readTree(sweet);
			for(int i = 0; i<sweetjson.size(); i++){
				String x = sweetjson.get(i).asText();
				sweet_entities.add(x.replaceAll("(\\p{Ll})(\\p{Lu})","$1 $2"));
			}
		} catch (JsonProcessingException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
        for (File f : dir.listFiles()) {
        	System.out.println(f.getName());
        	parser_names = new HashSet<String>();
        	parsers_detail = new Parsers();
        	data = null;
            // Ignore .DS_STORE files and children directory if any (ideally shouldn't be there)
            if(f.getName().contains(".DS_Store") || f.isDirectory()){
                continue;
            }
            
            parseCBOR(f);
            
            if(!empty){
            	
            	
            	try {
    				yourls_left= new FileWriter("yourls_left.json");
    			} catch (IOException e1) {
    				// TODO Auto-generated catch block
    				e1.printStackTrace();
    			}
//            	runTika();
            	runGrobid();
//            	if(data.get("Content-Type") != null && data.get("Content-Type").asText().equals("application/pdf")){
//            		runGrobid();
//            	}
            	file_size = f.length();
            	
            	data.put("request", request);
            	data.put("server", server);
            	data.put("status", status);
            	text_size = data.get("X-TIKA:content").asText().getBytes().length;
				if(data.get("X-Parsed-By").isArray()) {
					try {
						ArrayNode a = (ArrayNode)data.get("X-Parsed-By");
						for(int x = 0; x<a.size(); x++){
							String name = a.get(x).asText();
							meta_size = mapper.writeValueAsString(data).getBytes().length;
							if(!parser_names.contains(name)){
								parser_names.add(name);
								parsers_detail.add(name + "," + text_size + "," + meta_size);
							}
						}
						
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				else {
					String name = data.get("X-Parsed-By").asText();
					try {
						meta_size = mapper.writeValueAsString(data).getBytes().length;
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(!parser_names.contains(name)) {
						parser_names.add(name);
						parsers_detail.add(name + "," + text_size + "," + meta_size);
					}
				}
				
            	
            	
            	// Run Tag Ratio Algorithm
                String xhtml="";
                String ttr = "";
                if (data.get("X-TIKA:content") != null)
                {
                	
                    xhtml = data.get("X-TIKA:content").asText();
                    // Add text extracted by ttr
                    ttr = TTR.runTTR(xhtml);
                    //data.put("text_tagratio",ttr);
                    
                    FileWriter writer=null;
                    try {
                    	
                    	if(data.get("Content-Encoding") != null && !data.get("Content-Encoding").asText().equals("UTF-8")){
                    		byte b[] = ttr.getBytes();
                    		if(data.get("Content-Encoding").asText().equals("windows-1252")) {
                    			b = ttr.getBytes("Windows-1252");
                    		}
                    		else {
                    			b = ttr.getBytes(data.get("Content-Encoding").asText());
                    		}
                    		
                    		ttr = new String(b, Charset.forName("UTF-8"));
                    	}
    	                writer = new FileWriter(nltk);
    	                writer.write(ttr);
    	                writer.close();
    	                writer = new FileWriter(geot);
    	                writer.write(ttr);
    					writer.close();
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
                    sweet_ontologies(ttr);	 
                    runCurlLanguage();
                    runCurlNLTK();		
//					runTikaOpenNLP();
//					runTikaCoreNLP();
//					runTikaGrobidQuantities(ttr);
					
                    
					int nermeta_size =0;
					if(data.get("NER_UNITS") != null || data.get("NER_NAMES") != null)
					try {
						nermeta_size = mapper.writeValueAsString(data).getBytes().length - meta_size;
						parser_names.add("org.apache.tika.parser.ner.NamedEntityParser");
		                parser_names.add("org.apache.tika.parser.CompositeParser");
		                parsers_detail.add("org.apache.tika.parser.ner.NamedEntityParser," + text_size + "," + nermeta_size);
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					runGeoTopic();
				
					try {
		                String z = mapper.writeValueAsString(parsers_detail);
		                try {
							data.putPOJO("parser_details", (ArrayNode)mapper.readTree(z).get("parser"));
						} catch (IOException e) {
							e.printStackTrace();
						}
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
					
//					data.putPOJO("Parser_details", (ArrayNode)parsers_detail);
					data.put("file_size", file_size);
					
					assignYourl();
                }
                try {
					solr.feedsolr(mapper.writeValueAsString(data));
					
				} catch (JsonProcessingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
            
        }
        
        
        try {
        	if(yourls.size() > 0)
			mapper.writerWithDefaultPrettyPrinter().writeValue(yourls_left, yourls);
		} catch (JsonGenerationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void sweet_ontologies(String ttr) {
		ArrayList<String> output = new ArrayList<String>();
		for(int i=0 ; i<sweet_entities.size(); i++){
			if(StringUtils.containsIgnoreCase(ttr, sweet_entities.get(i))) {
				output.add(sweet_entities.get(i));
			}
		}
		if(output.size() > 0)
			data.putPOJO("sweet", output);
	}
	
	private void assignYourl() {
	
		String url = "http://polar.usc.edu/yourls-api.php?action=shorturl&format=json&url=" + data.get("request").asText() + "&title=" + data.get("request").asText() + "&keyword=p" + (id);
		yourls.add(url);
		data.put("id", "http://polar.usc.edu/p"+id++);
//	    	Response response = WebClient
//	                .create( "http://polar.usc.edu/yourls-api.php?action=shorturl&title=" + data.get("request").asText() + "&format=json&url=" + data.get("request").asText())
//	                .accept(MediaType.APPLICATION_JSON)
//	                .get();
//	    	String resp = response.readEntity(String.class);
//	    	if(resp.contains("<!DOCTYPE html>")){
//	    		return;
//	    	}
//	        ObjectNode yourls=null;
//			
//			try {
//				yourls = (ObjectNode) mapper.readTree(resp);
//				data.put("id", yourls.get("shorturl").asText());
//			} catch (JsonProcessingException e) {
//				System.out.println("JsonProcessingException Extract");
//			} catch (IOException e) {
//				System.out.println("IOException extract");
//			}
	}
	
	private void runTika() { 
		try {
			ContentDisposition cd = new ContentDisposition("attachment; filename=\"" + body.getName() + "\"");
	        Attachment att;
			att = new Attachment("input", new FileInputStream(body), cd);
			MultipartBody body = new MultipartBody(att);
	        Response response = WebClient
	                 .create( "http://localhost:9996/rmeta/form")
	                 .accept(MediaType.APPLICATION_JSON).type(MediaType.MULTIPART_FORM_DATA)
	                 .post(body);

	         String resp = response.readEntity(String.class);
	         if(resp.equals("")){
	         	return;
	         }
	         try {
				data = (ObjectNode) mapper.readTree(resp).get(0);
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
        
	}
	
	private void parseCBOR(File f) {
		// TODO Auto-generated method stub
		InputStream targetStream;
		try {
			targetStream = new FileInputStream(f);
			byte[] encodedBytes = IOUtils.toByteArray(targetStream);
			ByteArrayInputStream bais = new ByteArrayInputStream(encodedBytes );
			List<DataItem> dataItems = new CborDecoder(bais).decode();
			ObjectMapper m = new ObjectMapper(); 
			String x = "";
			for(DataItem dataItem : dataItems) {
			    // process data item
				
				x = dataItem.toString();
				
			}
			
			JsonNode j = m.readTree(x);
			String body_content = j.get("response").get("body").asText();
			request = j.get("url").asText();
			server = j.get("response").get("server").get("hostname").asText();
			
			if(body != null && !body_content.isEmpty()){
				empty = false;
				Pattern pattern = Pattern.compile("(Moved Permanently)");
        		Matcher matcher = pattern.matcher(body_content);
        		if(matcher.find()) {
        			status = "301";
        		}
        		else{
        			status = "200";
        		}
			    FileWriter writer=null;
                try {
	                writer = new FileWriter(body);
	                writer.write(body_content);
	                writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else{
				status = "0";
			}
			
			
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (CborException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private  void runCurlNLTK(){
    	
		String command = "curl -v -X POST -d @"+ nltk.getName() +" http://localhost:8888/nltk";
    	ExecuteShellCommand exe = new ExecuteShellCommand(command);
    	try {
			JsonNode jnode = mapper.readTree(exe.output.toString());
		 
			if((ArrayNode)jnode.get("names")!=null && ((ArrayNode)jnode.get("names")).size() > 0){
				data.putPOJO("NER_NAMES", (ArrayNode)jnode.get("names"));
			}
			if((ArrayNode)jnode.get("units")!=null && ((ArrayNode)jnode.get("units")).size() > 0){
				data.putPOJO("NER_UNITS", (ArrayNode)jnode.get("units"));
			}
			
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
	
	private  void runCurlLanguage(){
    	
		String command = "curl -X PUT -d @"+ nltk.getName() +" http://localhost:9996/language/stream";
    	ExecuteShellCommand exe = new ExecuteShellCommand(command);
    	if(exe.output.toString() !=null){
    		data.put("lang", exe.output.toString());
    	}

    }
		
	@SuppressWarnings("unused")
	private void runTikaCoreNLP() {
		try {
			ContentDisposition cd = new ContentDisposition("attachment; filename=\"" + body.getName() + "\"");
	        Attachment att;
			att = new Attachment("input", new FileInputStream(body), cd);
			MultipartBody body = new MultipartBody(att);
	        Response response = WebClient
	                 .create( "http://localhost:9995/rmeta/form")
	                 .accept(MediaType.APPLICATION_JSON).type(MediaType.MULTIPART_FORM_DATA)
	                 .post(body);

	         String resp = response.readEntity(String.class);
	         if(resp.equals("")){
	         	return;
	         }
	         try {
	 			JsonNode jnode = mapper.readTree(resp).get(0);
	 		
	 			 
	 			if((ArrayNode)jnode.get("NER_LOCATION")!=null && !jnode.get("NER_LOCATION").asText().equals("[]")){
	 				data.putPOJO("NER_LOCATION_CORENLP", (ArrayNode)jnode.get("NER_LOCATION"));
	 			}
	 			if((ArrayNode)jnode.get("NER_PERSON")!=null && !jnode.get("NER_PERSON").asText().equals("[]")){
	 				data.putPOJO("NER_PERSON_CORENLP", (ArrayNode)jnode.get("NER_PERSON"));
	 			}
	 			if((ArrayNode)jnode.get("NER_ORGANIZATION")!=null && !jnode.get("NER_ORGANIZATION").asText().equals("[]")){
	 				data.putPOJO("NER_ORGANIZATION_CORENLP", (ArrayNode)jnode.get("NER_ORGANIZATION"));
	 			}
	 			if((ArrayNode)jnode.get("NER_DATE")!=null && !jnode.get("NER_DATE").asText().equals("[]")){
	 				data.putPOJO("NER_DATE_CORENLP", (ArrayNode)jnode.get("NER_DATE"));
	 			}
	 			
	 			
	 		} catch (JsonProcessingException e) {
	 			// TODO Auto-generated catch block
	 			e.printStackTrace();
	 		} catch (IOException e) {
	 			// TODO Auto-generated catch block
	 			e.printStackTrace();
	 		}
	        
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	@SuppressWarnings("unused")
	private void runTikaGrobidQuantities(String body) {
//		String command = "curl -G --data-urlencode \"text=i lost one minute.\" cloud.science-miner.com/quantity/processQuantityText";
//    	ExecuteShellCommand exe = new ExecuteShellCommand(command);
//    	 Response response = WebClient
//                 .create( "http://www.cloud.science-miner.com/quantity/processQuantityText")
//                 .accept(MediaType.APPLICATION_JSON)
//                 .query("text", "i lost one minute.")
//                 .get();
//
//    	 String resp = response.readEntity(String.class);
//    	try {
//			JsonNode jnode = mapper.readTree(exe.output.toString());
//		
//			 
//			if((ArrayNode)jnode.get("names")!=null && !jnode.get("names").asText().equals("[]")){
//				data.putPOJO("NER_NAMES", (ArrayNode)jnode.get("names"));
//			}
//			if((ArrayNode)jnode.get("units")!=null && !jnode.get("units").asText().equals("[]")){
//				data.putPOJO("NER_UNITS", (ArrayNode)jnode.get("units"));
//			}
//			
//		} catch (JsonProcessingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
	}
	@SuppressWarnings("unused")
	private void runTikaOpenNLP() {
		try {
			ContentDisposition cd = new ContentDisposition("attachment; filename=\"" + body.getName() + "\"");
	        Attachment att;
			att = new Attachment("input", new FileInputStream(body), cd);
			MultipartBody body = new MultipartBody(att);
	        Response response = WebClient
	                 .create( "http://localhost:9997/rmeta/form")
	                 .accept(MediaType.APPLICATION_JSON).type(MediaType.MULTIPART_FORM_DATA)
	                 .post(body);

	         String resp = response.readEntity(String.class);
	         if(resp.equals("")){
	         	return;
	         }
	         try {
	 			JsonNode jnode = mapper.readTree(resp).get(0);
	 		
	 			 
	 			if((ArrayNode)jnode.get("NER_LOCATION")!=null && !jnode.get("NER_LOCATION").asText().equals("[]")){
	 				data.putPOJO("NER_LOCATION", (ArrayNode)jnode.get("NER_LOCATION"));
	 			}
	 			if((ArrayNode)jnode.get("NER_PERSON")!=null && !jnode.get("NER_PERSON").asText().equals("[]")){
	 				data.putPOJO("NER_PERSON", (ArrayNode)jnode.get("NER_PERSON"));
	 			}
	 			if((ArrayNode)jnode.get("NER_ORGANIZATION")!=null && !jnode.get("NER_ORGANIZATION").asText().equals("[]")){
	 				data.putPOJO("NER_ORGANIZATION", (ArrayNode)jnode.get("NER_ORGANIZATION"));
	 			}
	 			if((ArrayNode)jnode.get("NER_PERCENT")!=null && !jnode.get("NER_PERCENT").asText().equals("[]")){
	 				data.putPOJO("NER_PERCENT", (ArrayNode)jnode.get("NER_PERCENT"));
	 			}
	 			
	 			
	 		} catch (JsonProcessingException e) {
	 			// TODO Auto-generated catch block
	 			e.printStackTrace();
	 		} catch (IOException e) {
	 			// TODO Auto-generated catch block
	 			e.printStackTrace();
	 		}
	        
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	private void runGrobid() {
        try {
        	
            
            ContentDisposition cd = new ContentDisposition("attachment; filename=\"" + body.getName() + "\"");
            Attachment att = new Attachment("input", new FileInputStream(body), cd);
            MultipartBody body = new MultipartBody(att);
            Response response = WebClient
                    .create( "http://localhost:9997/rmeta/form")
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.MULTIPART_FORM_DATA)
                    .post(body);

            String resp = response.readEntity(String.class);
            if(resp.equals("")){
            	return;
            }
            mapper = new ObjectMapper();
            data = (ObjectNode) mapper.readTree(resp).get(0);            
				
			
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("runGrobid: IOException occurred.");
        }
    }
	
	private void runGeoTopic(){

        try {
            // Send rest request to Tika
            ContentDisposition cd = new ContentDisposition("attachment; filename=\"" + geot.getName() + "\"");
            Attachment att = new Attachment("input", new FileInputStream(geot), cd);
            MultipartBody body = new MultipartBody(att);
            Response response = WebClient
                    .create( "http://localhost:9998/rmeta/form")
                    .accept(MediaType.APPLICATION_JSON).type(MediaType.MULTIPART_FORM_DATA)
                    .post(body);
            String resp = response.readEntity(String.class);
            if(!resp.equals("")) {
	            ObjectMapper geotMapper = new ObjectMapper();
	            JsonNode geotData = geotMapper.readTree(resp).get(0);
	            
	            Iterator<Entry<String, JsonNode>> itParams = geotData.fields();
	            while (itParams.hasNext()) {
	              Entry<String, JsonNode> jsonParam = itParams.next();
	              String key = jsonParam.getKey();
	              if(!key.equals("Content-Type") && !key.equals("X-Parsed-By") && !key.equals("X-TIKA:parse_time_millis") && !key.equals("resourceName")) {
	            	  data.put(key, geotData.get(key).asText());
	              }
	            }
	            
	            int geo_meta = 0;
				if(geotData.get("X-Parsed-By").isArray()) {
					try {
						ArrayNode a = (ArrayNode)geotData.get("X-Parsed-By");
						for(int x = 0; x<a.size(); x++){
							String name = a.get(x).asText();
							geo_meta = mapper.writeValueAsString(geotData).getBytes().length - meta_size;
							if(!parser_names.contains(name)){
								parser_names.add(name);
								parsers_detail.add(name + "," + text_size + "," + geo_meta);
							}
						}
						
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				else {
					String name = geotData.get("X-Parsed-By").asText();
					try {
						geo_meta = mapper.writeValueAsString(geotData).getBytes().length - meta_size;
					} catch (JsonProcessingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if(!parser_names.contains(name)) {
						parser_names.add(name);
						parsers_detail.add(name + "," + text_size + "," + geo_meta);
					}
				}
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            System.out.println("geotopicparser: IOException occurred.");
        }
    }
	
}
