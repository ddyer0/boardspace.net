iOS Push certificates have been created for your app with bundle ID com.boardspace.  Please keep this file as you will need the details about the certificate locations and passwords to use Push successfully in your apps.

Development Push Certificate URL: 
null

Development Push Certificate Password: 'null'

Production Push Certificate URL: 
null

Production Push Certificate Password: 'null'

In order to send a push from within an app in production do this to send the push to all devices:

String iOSCertURL = "...";
String iOSCertPassword = "...";
String token = "...";  // the authorization token from the account settings in the CodenameOne website +
boolean result = new Push(token, "Your Message", null)
    .apnsAuth(iOSCertURL, iosCertPassword, true)
    .send();

And for development:

String iOSCertURL = "...";
String iOSCertPassword = "...";
String token = "...";  // the authorization token from the account settings in the CodenameOne website +
boolean result = new Push(token, "Your Message", null)
    .apnsAuth(iOSCertURL, iosCertPassword, false)
    .send();

To send push from a server see http://www.codenameone.com/manual/appendix-ios.html#_sending_push_message_from_a_java_or_generic_server 
E.g. for a Java server:
HttpURLConnection connection = (HttpURLConnection)new URL("https://push.codenameone.com/push/push").openConnection();
connection.setDoOutput(true);
connection.setRequestMethod("POST");
connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
String cert = ITUNES_DEVELOPMENT_PUSH_CERT;
String pass = ITUNES_DEVELOPMENT_PUSH_CERT_PASSWORD;
if(ITUNES_PRODUCTION_PUSH) {
    cert = ITUNES_PRODUCTION_PUSH_CERT;
    pass = ITUNES_PRODUCTION_PUSH_CERT_PASSWORD;
}
String query = "token="  + PUSH_TOKEN +
    "&device=" + URLEncoder.encode(deviceId1, "UTF-8") +
    "&device=" + URLEncoder.encode(deviceId2, "UTF-8") +
    "&device=" + URLEncoder.encode(deviceId3, "UTF-8") +
    "&type=1" +
    "&auth=" + URLEncoder.encode(GCM_SERVER_API_KEY, "UTF-8") +
    "&certPassword=" + URLEncoder.encode(pass, "UTF-8") +
    "&cert=" + URLEncoder.encode(cert, "UTF-8") +
    "&body=" + URLEncoder.encode(MESSAGE_BODY, "UTF-8") +
    "&production=" + ITUNES_PRODUCTION_PUSH +
    "&sid=" + URLEncoder.encode(WNS_SID, "UTF-8") +
    "&client_secret=" + URLEncoder.encode(WNS_CLIENT_SECRET, "UTF-8");
try (OutputStream output = connection.getOutputStream()) {
    output.write(query.getBytes("UTF-8"));
}
int c = connection.getResponseCode();For more information about how to send Push notifications to your app, please refer to the developers  guide at:
 https://www.codenameone.com/manual/appendix-ios.html#_push_notifications

 or watch this introductory screencast:
https://www.codenameone.com/how-do-i---use-push-notification-send-server-push-messages.html

Best regards
The Codename One Team