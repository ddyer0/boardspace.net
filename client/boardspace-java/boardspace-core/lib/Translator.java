package lib;
/*
Copyright 2006-2023 by Dave Dyer

This file is part of the Boardspace project.

Boardspace is free software: you can redistribute it and/or modify it under the terms of 
the GNU General Public License as published by the Free Software Foundation, 
either version 3 of the License, or (at your option) any later version.

Boardspace is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 
See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with Boardspace.
If not, see https://www.gnu.org/licenses/. 
*/

/**
 * Translates a single line of text into a target language
 * using the Anthropic Claude API.
 *
 * Dependencies: Java 11+ (uses java.net.http — no external libraries needed)
 *
 * Usage:
 *   String result = Translator.translate("french", "Select #1");
 *   // → "Sélectionner #1"
 *
 * Set your API key via the ANTHROPIC_API_KEY environment variable,
 * or pass it explicitly to the overloaded method:
 *   String result = Translator.translate("german", "Next Draw", "sk-ant-...");
 */
public class Translator {
	private static final String ANTROPIC_HOST = "https://api.anthropic.com";
	private static final String ANTROPIC_API = "/v1/messages";
	
	private static final String PROXY_ANTROPIC_API = "/cgi-bin/tlib/anthropic_proxy.cgi";

	//private static final String API_URL   = "https://api.anthropic.com/v1/messages";
    private static final String API_VER   = "2023-06-01";
    private static final String MODEL     = "claude-sonnet-4-20250514";
    private static final int    MAX_TOKENS = 256;
    public static final String CLAUDEAPI = "claudeapi";

    private static final String SYSTEM_PROMPT =
    "You are a professional translator. " +
    "Translate the text the user provides into the requested language naturally and accurately. " +
    "Reply using EXACTLY this format and nothing else � no quotes, no period, no explanation: " +
    "SourceLanguage: Translation " +
    "For example: English: Bonjour";
    
    /**
     * Translates a line of text into the specified language.
     *
     * @param language  target language name, e.g. "french", "german", "japanese"
     * @param text      the English string to translate
     * @return          the translated string
     * @throws TranslationException if the API call fails or returns an error
     */
    public static String translate(String language, String text) throws TranslationException {
    	// APIkey is provided by the server's login script. Better practice than embedding it here.
        String apiKey = G.getString(CLAUDEAPI,"");
        if (apiKey == null || apiKey.length()==0) {
            throw new TranslationException(
                "ANTHROPIC_API_KEY environment variable is not set.");
        }
        return translate(language, text, apiKey);
    }

    /**
     * Translates a line of text into the specified language using an explicit API key.
     *
     * @param language  target language name, e.g. "french", "german", "japanese"
     * @param text      the English string to translate
     * @param apiKey    your Anthropic API key
     * @return          the translated string
     * @throws TranslationException if the API call fails or returns an error
     */
    public static String translate(String language, String text, String apiKey)
            throws TranslationException {

        if (language == null || language.length()==0)
            throw new IllegalArgumentException("language must not be blank");
        if (text == null)
            throw new IllegalArgumentException("text must not be null");

        if (text.length()==0) return text;

        String userMessage = "Translate to " + capitalise(language) + ": " + text;
        String requestBody = buildRequestBody(userMessage);

        try {
        	String props[][] = {
        			{"Content-Type",      "application/json"},
        			{"x-api-key",         apiKey},
        			{"anthropic-version", API_VER}			
        		};
        	boolean useProxy = G.isCheerpj();
        	UrlResult result = Http.postURL( useProxy ? Http.getHostName() : ANTROPIC_HOST,
        									useProxy ? PROXY_ANTROPIC_API : ANTROPIC_API, props,
        									requestBody,null,new UrlResult());
        	if(result.error==null)
        	{
        		return extractTranslation(result.text);
        	}
        	throw new TranslationException(result.error);
         	/*
         	// this is the original code that claude produced to make the http request
         	// the version actually used is semantically the same, but uses the standard
         	// (older) java http framework
         	//
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type",      "application/json")
                .header("x-api-key",         apiKey)
                .header("anthropic-version", API_VER)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();            
            HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new TranslationException(
                    "API error " + response.statusCode() + ": " + response.body());
            }
            return extractTranslation(response.body());
            */
            

        } catch (TranslationException e) {
            throw e;
        } catch (Exception e) {
            throw new TranslationException("HTTP request failed: " + e.getMessage(), e);
        }
    }

    // ── JSON helpers (no external library needed) ─────────────────────────────

    private static String buildRequestBody(String userMessage) {
    	// new version with prompt caching
    	return "{"
        + "\"model\":\""      + MODEL      + "\","
        + "\"max_tokens\":"   + MAX_TOKENS  + ","
        + "\"system\":["
        +   "{"
        +     "\"type\":\"text\","
        +     "\"text\":"     + jsonString(SYSTEM_PROMPT) + ","
        +     "\"cache_control\":{\"type\":\"ephemeral\"}"
        +   "}"
        + "],"
        + "\"messages\":[{"
        +   "\"role\":\"user\","
        +   "\"content\":" + jsonString(userMessage)
        + "}]"
        + "}";
    	// original version without caching
    	/*
        return "{"
            + "\"model\":\""      + MODEL      + "\","
            + "\"max_tokens\":"   + MAX_TOKENS  + ","
            + "\"system\":"       + jsonString(SYSTEM_PROMPT) + ","
            + "\"messages\":[{"
            +   "\"role\":\"user\","
            +   "\"content\":" + jsonString(userMessage)
            + "}]"
            + "}";
            */
        
    }

    /**
     * Pulls the text value out of the first content block in a Claude response.
     * Handles the shape: {"content":[{"type":"text","text":"..."}], ...}
     */
    private static String extractTranslation(String json) throws TranslationException {
        // Find "text": after the first content block
        int contentIdx = json.indexOf("\"content\"");
        if (contentIdx < 0)
            throw new TranslationException("Unexpected API response (no content): " + json);

        int textKeyIdx = json.indexOf("\"text\"", contentIdx);
        if (textKeyIdx < 0)
            throw new TranslationException("Unexpected API response (no text): " + json);

        int colon = json.indexOf(':', textKeyIdx);
        int quote1 = json.indexOf('"', colon + 1);
        if (quote1 < 0)
            throw new TranslationException("Unexpected API response (malformed text): " + json);

        StringBuilder sb = new StringBuilder();
        int i = quote1 + 1;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '"') break;          // closing quote
            if (c == '\\' && i + 1 < json.length()) {
                char esc = json.charAt(i + 1);
                switch (esc) {
                    case '"'  : sb.append('"'); break;
                    case '\\' : sb.append('\\'); break;
                    case 'n'  : sb.append('\n'); break;
                    case 'r'  : sb.append('\r'); break;
                    case 't'  : sb.append('\t'); break;
                    case 'u'  : {
                        String hex = json.substring(i + 2, i + 6);
                        sb.append((char) Integer.parseInt(hex, 16));
                        i += 4;
                    }
                    break;
                    default   : sb.append(esc); break;
                }
                i += 2;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    /** Encodes a Java string as a JSON string literal (with surrounding quotes). */
    private static String jsonString(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"'  : sb.append("\\\""); break;
                case '\\' : sb.append("\\\\"); break;
                case '\n' : sb.append("\\n"); break;
                case '\r' : sb.append("\\r"); break;
                case '\t' : sb.append("\\t"); break;
                default   : {
                    if (c < 0x20) sb.append(G.format("\\u%04x", (int) c));
                    else          sb.append(c);
                }
                break;
            }
        }
        return sb.append('"').toString();
    }

    private static String capitalise(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase();
    }

    @SuppressWarnings("serial")
	public static class TranslationException extends Exception {
        public TranslationException(String message)                  { super(message); }
        public TranslationException(String message, Throwable cause) { super(message, cause); }
    }
    public static String lastLanguage = "";
    public static String simpleTranslate(String to,String message,ConnectionManager myNetConn)
    {
    	try {
    		lastLanguage = "";
    		String result = translate(to,message);
    		int ind = result.indexOf(':');
    		if(ind>0)
    		{
    			String lang = result.substring(0,ind);
    			if(to.equalsIgnoreCase(lang)) { result = null; }
    			else { result = result.substring(ind+1); }
    			lastLanguage = lang;
    			if("unknown".equalsIgnoreCase(lang)) { result = ""; }
    		}
    		return result;
    	}
    	catch (TranslationException err)
    	{	myNetConn.logError("translation",err);
    		return null;
    	}
    }
    /*
    public static void main(String[] args) throws Exception {
        String[][] tests = {
            { "french",   "Select all" },
            { "german",   "Next Draw" },
            { "japanese", "Player accepted your invitation to play" },
            { "spanish",  "Click \"Done\" to confirm repair of this building" },
            { "dutch",    "No available military units" },
        };

        for (String[] t : tests) {
            String lang = t[0], text = t[1];
            try {
                String result = translate(lang, text);
                String back = translate("english",result);
                System.out.printf("%s%n%s: %s%nback: %s%n%n", text, lang, result, back);;
                
            } catch (TranslationException e) {
                System.err.println("Error for " + lang + ": " + e.getMessage());
            }
        }
    }
    */
}
