package polar.usc.edu.ccd;

import java.util.HashSet;

public class Parsers {
	private HashSet<String> parser = new HashSet<String>();
	
	public HashSet<String> getParser() {
		return parser;
	}

	public void setParser(HashSet<String> parser) {
		this.parser = parser;
	}

	public void add(String parser) {
		this.parser.add(parser);
	}
	
	public void addAll(Parsers parser) {
		
		for(String p: parser.parser) {
			this.parser.add(p);
		}
	}
}
