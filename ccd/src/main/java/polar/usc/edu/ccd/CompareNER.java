package polar.usc.edu.ccd;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.ext.multipart.Attachment;
import org.apache.cxf.jaxrs.ext.multipart.ContentDisposition;
import org.apache.cxf.jaxrs.ext.multipart.MultipartBody;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import polar.usc.edu.ccd.Labels;
import polar.usc.edu.ccd.Names;
import polar.usc.edu.ccd.Series;

public class CompareNER {

	private HashSet<String> freq; 
    private HashMap<String, Integer> nltk;
    private HashMap<String, Integer> nlp;
    private HashMap<String, Integer> corenlp;
    private JsonNode datasetElement;
    private ObjectMapper mapper;
    FileWriter file;
    
    public CompareNER() {
    	
    	freq = new HashSet<String>();
     	nltk = new HashMap<String,Integer>();
  		nlp = new HashMap<String,Integer>();
  		corenlp = new HashMap<String,Integer>();
  		datasetElement=null;
        mapper = new ObjectMapper();
  		file = null;
    }
    
    public static void main(String m[]) throws JsonParseException, JsonMappingException, IOException {
        
        String solrUrl = m[0];
        File destination =new File(m[3]);
        CompareNER obj = new CompareNER();
        obj.countNER(solrUrl);
        obj.createJSON(destination);
    }
    
    @SuppressWarnings("unchecked")
	private void countNER(String solrUrl) throws JsonParseException, JsonMappingException, IOException {

    	JsonNode node;
        JsonNode dataset=null;
        String url;
        String response;
        
        for (int c=0; c<5000; c+=200) {
        	System.out.println(c);
            url = solrUrl + "/select?q=gunsamerica&start="+c+"&rows=200&fl=content&wt=json&indent=true";
            response = WebClient
            		   .create(url)
            		   .accept(MediaType.APPLICATION_JSON)
            		   .get()
            		   .readEntity(String.class);

            try {
                node = mapper.readTree(response);
                dataset = node.get("response").get("docs");
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            Iterator<JsonNode> datasetElements = dataset.iterator();
            
            while (datasetElements.hasNext()) {
                datasetElement = datasetElements.next();
                String content = datasetElement.get("content").asText();
                
	            runTika("9991", content, freq, corenlp);
				runTika("9992", content, freq, nlp);
				
				ArrayList<String> ner_content = new ArrayList<String>();
				if(dataset.get("ner_names")!=null) {
					ner_content.addAll(mapper.readValue(datasetElement.get("locations").toString(), ArrayList.class));						
				}
				for(int i=0; i<ner_content.size(); i++){
		    		if(!freq.contains(ner_content.get(i))){
		    			freq.add(ner_content.get(i));
		    		}
		    		if(nltk.containsKey(ner_content.get(i))){
		    			nltk.put(ner_content.get(i), nltk.get(ner_content.get(i)) + 1);
		    		}
		    		else{
		    			nltk.put(ner_content.get(i), 1);
		    		}
		    	}
               
            }
        }
    }
    
    

	private void createJSON(File destination) throws JsonGenerationException, JsonMappingException, IOException {
		ArrayList<Names> frequencies = new ArrayList<Names>();
        for (String value:freq) {
            int x = nltk.containsKey(value)?nltk.get(value):0;
            int y = nlp.containsKey(value)?nlp.get(value):0;
            int q = corenlp.containsKey(value)?corenlp.get(value):0;
//            int z = x+y-Math.abs(x-y);
//            if (z==0) {
//            	z = x>y?0:-y;
//            }
            int z1 = x+y-Math.abs(x-y);
            int z2 = q+y-Math.abs(q-y);
            int z = z1 + z2 - Math.abs(z1-z2);
            if (z==0) {
            	int max = 0;
            	max = x<y && x<q?x:q<y && q<x?q:y;
            	if(x==0 && q==0)
            		max=10;
            	else if (x==0 && y==0)
            		max=10;
            	else if (q==0 && y==0)
            		max=10;
            	z = -max;
            }
            frequencies.add(new Names(value, z ));
        }
        
        Collections.sort(frequencies, maximumOverlap);
        ArrayList<String> final_labels = new ArrayList<String>();
        ArrayList<Integer> nltk_value = new ArrayList<Integer>();
        ArrayList<Integer> nlp_value = new ArrayList<Integer>();
        ArrayList<Integer> corenlp_value = new ArrayList<Integer>();
        for (int i=0; i<frequencies.size(); i++) {
            String value = frequencies.get(i).name;
            final_labels.add(value);
            if (nltk.containsKey(value)) {
                nltk_value.add(nltk.get(value));
            } else {
                nltk_value.add(0);
            }
            if (corenlp.containsKey(value)) {
                corenlp_value.add(corenlp.get(value));
            } else {
                corenlp_value.add(0);
            }
            if (nlp.containsKey(value)) {
                nlp_value.add(nlp.get(value));
            } else {
                nlp_value.add(0);
            }
        }

        Series []s = {new Series("nltk", nltk_value),new Series("nlp", nlp_value),new Series("corenlp", corenlp_value)};
        Labels labels = new Labels(final_labels, s);
        ObjectMapper mapper = new ObjectMapper();
        destination = new File(destination.getAbsolutePath() + "/maximal_ner.json");
        mapper.writerWithDefaultPrettyPrinter().writeValue(destination, labels);
        System.out.println("Json ready for Visualization: " + destination.getAbsolutePath());
	}

	public static Comparator<Names> maximumOverlap = new Comparator<Names>() {
        public int compare(Names one, Names two) {
            return (int)two.strength - (int)one.strength;
        }
    };
    
    
    private void runTika(String port, String content, HashSet<String> freq, HashMap<String, Integer> ner) {
		File demo = new File("content");
        try {
            file = new FileWriter(demo);
            file.write(content);
            file.close();
		
	        ContentDisposition cd = new ContentDisposition("attachment; filename=\"" + demo.getName() + "\"");
	        Attachment att = new Attachment("input", new FileInputStream(demo), cd);
	        MultipartBody body = new MultipartBody(att);
	        Response response = WebClient
	                .create( "http://localhost:" + port + "/rmeta/form")
	                .accept(MediaType.APPLICATION_JSON).type(MediaType.MULTIPART_FORM_DATA)
	                .post(body);
	        String nlp_resp = response.readEntity(String.class);
	        ObjectMapper mapper = new ObjectMapper();
	        JsonNode data = (ObjectNode) mapper.readTree(nlp_resp).get(0);
	        Iterator<Entry<String, JsonNode>> itParams = data.fields();
	        while (itParams.hasNext()) {
	        	Entry<String, JsonNode> jsonParam = itParams.next();
	        	String key = jsonParam.getKey();
	        	if(key.startsWith("NER")) {
	        		if(data.get(key).isArray()){
			        	ArrayNode ners = (ArrayNode) data.get(key);
			        	for(int x=0; x<ners.size(); x++) {
			        		String value = ners.get(x).asText();
			        		if(!freq.contains(value)) {
			        			freq.add(value);
			        		}
			        		if(ner.containsKey(value)) {
			        			ner.put(value, ner.get(value) + 1);
			        		}
			        		else {
			        			ner.put(value, 1);
			        		}
		    	  	  	}
	        		}
	        		else {
		    		  String value = data.get(key).asText();
		    		  if(!freq.contains(value)) {
		    			  freq.add(value);
		        	  }
		    		  if(ner.containsKey(value)) {
		    			  ner.put(value, ner.get(value) + 1);
		    		  }
		    		  else {
		    			  ner.put(value, 1);
		    		  }
	        		}
		          }
	        }
        } 
        catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}

