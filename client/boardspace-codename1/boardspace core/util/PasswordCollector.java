
package util;

import com.codename1.ui.Component;
import lib.Image;
import com.codename1.ui.geom.Dimension;
import com.codename1.ui.layouts.Insets;

import bridge.*;
import common.CommonConfig;

import java.util.StringTokenizer;
import java.util.Vector;

import lib.Base64;
import lib.G;
import lib.Http;
import lib.InternationalStrings;
import lib.SimpleObserver;
import lib.UrlResult;
import lib.XFrame;
import online.common.OnlineConstants;
import udp.PlaytableServer;
import udp.PlaytableStack;

 //import java.util.prefs.BackingStoreException;
 //import java.util.prefs.Preferences;

 public class PasswordCollector extends JPanel
						   implements ActionListener,ItemListener,WindowListener,OnlineConstants,
						   Config
{
	 static final long serialVersionUID = 1L;

	 public static final String nameKey = keyPrefix + "namekey";
	 public static final String passKey = keyPrefix + "passKey";
	 public static final String savePassKey = keyPrefix + "savePassKey";
	 public static final String countryKey = regPrefix + "countryKey";
	 public static final String realNameKey = regPrefix + "realName";
	 
	 public static final String guestName = "guest";
	 public static final String OK = "Login";
	 public static final String REGISTER = "Register";
	 public static final String FINALREGISTER = "Ok! I'm ready to log in";
	 public static final String cancel = "Cancel";
	 public static final String COUNTRY = "Home Country";
	 public static final String SALT = "oheorh";
	 public static final String YourName = "Your user name: ";
	 public static final String YourPassword = "Your password: ";
	 public static final String LogInTo = "Log in to #1";
	 public static final String SavePassword = "Save Password";
	 public static final String LoginAsGuest = "Log in as a guest";
	 public static final String RegisterAccount = "Register a new user";
	 public static final String VisitSite = "#1 Home Page";
	 public static final String Feedback = "Send feedback";
	 public static final String Appstore = "App Store";
	 // for registration
	 public static final String YourRealName = "Your real name";
	 public static final String YourEmail = "Your email address";
	 public static final String YourEmailPromise = "Registration email will be sent.";
	 public static final String YourPassword2 = "Your Password (again)";
	 public static final String CountryString = "... or \"Planet Earth\"";
	 public static final String NospaceMessage = "Your user name can't contain spaces";
	 public static final String MismatchMessage = "Passwords do not match";
	 public static final String PasswordLengthMessage = "Passwords must be 6 - 25 characters";
	 public static int MaxPasswordLength = 25;
	 public static int MinPasswordLength = 6;
	 public static final String NameLengthMessage = "user names must be 3-10 characters";
	 public static int MinUsernameLength = 3;
	 public static int MaxUsernameLength = 10;
	 public static final String BadRealName = "fill in your name; or whatever you want to be called.";
	 public static final String NocountryMessage = "Please select your home country, or \"Planet Earth\"";
	 public static final String BadEmailMessage = "Must be a real email address";
	 public static final String NoUserMessage = "the user name \"#1\" is not available";
	 public static final String UnexpectedResponseMessage = "Unexpected response: #1";
	 public static final String BadPasswordMessage = "your password was rejected for some reason";
	 public static final String SendingRegMessage = "Sending your info to #1";
	 public static final String NocontactMessage = "Can't contact #1, try again later";
	 public static final String SuccessMessage = "Success!  check your email";
	 public static final String SuccessMessage2 = "click on Ok when you have confirmed your account"; 
	 public static final String VersionMessage = "Client version #1";
	 public static final String VersionPreferredMessage = "Preferred version is #1";
	 public static final String VersionRejectedMessage = "This version is too old.  Please visit the app store and update it";
	 public static final String ReviewMessage = "Play Offline";
	 public static final String PlaytableMessage = "Join #1";
	 public static String LoginStrings [] = 
		 {
		 OK,
		 PlaytableMessage,
		 VisitSite,
		 ReviewMessage,
		 Appstore,
		 Feedback,
		 FINALREGISTER,
		 REGISTER,
		 SuccessMessage,
		 BadRealName,
		 VersionMessage,
		 SuccessMessage2,
		 VersionPreferredMessage,
		 VersionRejectedMessage,
		 cancel,
		 PREFERREDLANGUAGE,
		 YourName,
		 NoUserMessage,
		 UnexpectedResponseMessage,
		 BadPasswordMessage,
		 YourPassword,
		 NocontactMessage,
		 PasswordLengthMessage,
		 SendingRegMessage,
		 NameLengthMessage,
		 LogInTo,
		 NocountryMessage,
		 BadEmailMessage,
		 SavePassword,
		 NospaceMessage,
		 LoginAsGuest,
		 RegisterAccount,
		 YourPassword2,
		 YourRealName,
		 YourEmail,
		 YourEmailPromise,
		 CountryString,
		 COUNTRY,
		 };
	 
	 enum Screen { Login,Register,Confirmation,Rejection};
	 Screen currentScreen = Screen.Login;
	 public void reconfigure(Screen m) 
	 {
		 currentScreen = m;
		 reconfigure();
	 }
	 public void reconfigure()
	 {
		 switch(currentScreen)
		 {
		 default: G.Error("Not expecting screen %s",currentScreen);
		 case Login: 	configureForLogin(); break;			// normal login
		 case Register: configureForRegister(); break;		// register a new user
		 case Rejection:	configureForRejection(); break;	// version too old
		 case Confirmation: configureForConfirmation(); break;	// confirm after register
		 }
		 if(!G.isCodename1()) { autoSize(); }
		 repaint();
	 }

	 private static String countries[] = null;
	 private Checkbox developHostOption = null;
	 private Checkbox develop2xOption = null;
	 private SimpleObserver observer = null;
	 public boolean finished = false;
	 private JFrame controllingFrame; 		//the controlling frame, needed for dialogs
	 
	 private JPasswordField passwordField;	// the password
	 private JPasswordField passwordField2;	// second copy for registration
	 private Checkbox savePasswordField;	// save the password (and the liver!)
	 private Checkbox loginAsGuestField;	// log in as a guest instead of a registered user
	 private Choice langField;				// preferred language
	 private Choice countryField;			// home country
	 private JTextField nameField;			// user name
	 private JTextField realNameField;		// real name, or whatever
	 private JTextField emailField;			// email address
	 private JPanel passPane;				// passpane can appear and disappear depending on "guest"
	 private JButton registerAccountButton;	// switch to registeration
	 private JButton reviewButton;
	 private JButton visitSiteButton;	// visit the web site
	 private JButton appstoreButton;	// visit the app store
	 private JButton feedbackButton;
	 private JButton okButton;				// ok from login
	 private JButton finalRegisterButton;	// ok after confirming registration
	 private JButton registerButton;			// ok after filling in registration form
	 private JButton cancelButton;			// cancel and go back to login
	 private static Preferences prefs = Preferences.userRoot();
	 private static InternationalStrings s ;
	 private JPanel mainPanel = null;
	 private String errorMessage = "";
	 private JLabel errorText = null;
	 private JButton[] serverButtons = null; 
	 // constructor
	public PasswordCollector(JFrame f,SimpleObserver o) 
	 {	 //PlatformLogger l = PlatformLogger.getLogger("java.util.prefs");
	 	 //l.setLevel(Level.SEVERE);	// quench a warning message
		 controllingFrame = f;

		 observer = o;
		 if(isAcceptableVersion())
		 	{
			 reconfigure(Screen.Login);
		 	}
		 	else
		 	{
		 		reconfigure(Screen.Rejection); 
		 	}
	 }
	 

		// get the list of acceptable languages from the server.
	    static private void parseLanguageList(String in)
	    {
	    	Vector<String>stack = new Vector<String>();
	    	StringTokenizer str = new StringTokenizer(in);
	    	while(str.hasMoreTokens()) { stack.addElement(str.nextToken()); }
	    	InternationalStrings.languages = stack.toArray(new String[stack.size()]);
	    }

	    //# tokens are key/value pairs, nominally expected ios_min_version ios_version android_min_version android_version
	    //# but they're all just stored in the appletparameters.
	    static private void parseVersionTokens(String in)
	    {	String next = null;
	    	StringTokenizer tok = new StringTokenizer(in);
	       	while (tok.hasMoreTokens() && !">".equals(next = tok.nextToken())) 
	    		{
	    		String val = tok.hasMoreTokens() ? tok.nextToken() : ""; 
	    		G.putGlobal(next,val);    
	    		if(!G.isCodename1() && "java_version".equals(next)) { G.setAppVersion(val); }
	    		//G.print("ver "+next+" "+val);
	    		};
	    }
	    //
	    // parse data from the applettag.cgi interaction, key/value pairs where the key is plain text and the 
	    // value is encoded base64, not primarily for obfuscation, but so the internal contents are not restricted.
	    //
	    static private void parseInfoGroup(StringTokenizer tok)
	    {
	    	while(tok.hasMoreTokens())
	    	{
	    		String next = tok.nextToken();
	    		String val = tok.hasMoreTokens() ? Base64.decodeString(tok.nextToken()) : "";
	    		
	    		if("languages".equals(next)) { parseLanguageList(val); }
	    		else if("versions".equals(next)) { parseVersionTokens(val); }
	    		else if("message".equals(next)) { G.putGlobal(next,val); }
	    		else { G.print("Unexpected key "+next+" : "+val); }
	    	}
	    }


	 private static void initLanguage()
	 {
		 String lang = language ;
		 if(lang==null) { lang = prefs.get(langKey, "english"); }
		 if(lang==null) { lang = "english"; }
		 language = lang = lang.toLowerCase();
    	 prefs.put(langKey,language);
		 s = InternationalStrings.initLanguage(lang);
	 }
	 public void disposeMainPanel()
	 {
		 if(mainPanel!=null)
		 { mainPanel.setVisible(false);
		   remove(mainPanel); 
		   mainPanel=null; 
		 }
	 }
	 private String[] parseReginfo(String in)
	 {
		 Vector<String>stack = new Vector<String>();
		 StringTokenizer str = new StringTokenizer(in,"\r\n");
		 while(str.hasMoreTokens()) { stack.addElement(str.nextToken()); }
		 return(stack.toArray(new String[stack.size()]));
	 }
	 private boolean getPreregInfo()
	 {
	     	UrlResult regInfo = Http.postEncryptedURL(Http.getHostName(),Config.getEncryptedURL,"&tagname=prereginfo",null);
	     	if((regInfo.error==null) && (regInfo.text!=null))
	     	{
	     		countries = parseReginfo(regInfo.text);
	     		return(true);
	     	}
	     	return(false);
	 }
	 private void changeMessage(String m)
	 {
		 errorText.setText(m);
		 errorText.getParent().repaint(); 
	 }
	 
	 public String getSelectedLanguage()
	 {	if(langField!=null)
	 	{
		 int ind = langField.getSelectedIndex();
		 if(ind>=0 && ind<InternationalStrings.languages.length)
		 {
			 language = InternationalStrings.languages[ind];
    	 }
	 	}
	 	return(language);
	 }
	 
	@SuppressWarnings("deprecation")
	 private boolean performRegistration()
	 {	errorMessage = "";
		 String name = nameField.getText().trim();
		 String realName = realNameField.getText().trim();
		 String password = passwordField.getText().trim();
		 String password2 = passwordField2.getText().trim();
		 String language = getSelectedLanguage();
		 String country = countryField.getSelectedItem().toString();
		 String email = emailField.getText().trim();
		 if(name.indexOf("\r\n\t ")>=0) { errorMessage = s.get(NospaceMessage); }
		 else if((name.length()<MinUsernameLength) || (name.length()>MaxUsernameLength))
		 	{ errorMessage=s.get(NameLengthMessage); 
		 	}
		 else if((password.length()<MinPasswordLength)||(password.length()>MaxPasswordLength))
		 	{ errorMessage = s.get(PasswordLengthMessage);
		 	}
		 else if(!password.equals(password2)) { errorMessage = s.get(MismatchMessage); }
		 else if("".equals(realName)) { errorMessage = s.get(BadRealName); }
		 else if(email.indexOf('@')<=0) { errorMessage = s.get(BadEmailMessage); }
		 else if(country.equals(countries[1])) 
		 	{ errorMessage = s.get(NocountryMessage);
		 	  countryField.select(s.get("Planet Earth"));
		 	}
		 else {
			 prefs.put(nameKey,name);
			 prefs.put(passKey,"");
			 prefs.put(langKey,language);
			 prefs.put(countryKey,country);
			 prefs.put(realNameKey,realName);
			 
			 try { prefs.flush(); } 
 			 catch (BackingStoreException err) 
 				{ System.out.println("E "+err.toString());
 				};
 			
			 String params = "&jws=1"
					 + "&"+ PasswordParameterName + "=" + Http.escape(password)
					 + "&" + PasswordParameterName + "2="+Http.escape(password)
					 + "&pname=" + Http.escape(name)
					 + "&timestamp=" + Http.escape(countries[0])
					 + "&country=" + Http.escape(country)
					 + "&language=" + Http.escape(language)
					 + "&" + RealnameParameterName + "=" + Http.escape(Http.encodeEntities(realName))
					 + "&" + EmailParameterName +"=" + Http.escape(email)
					 +"&language="+Http.escape(language)
					 +"&cookie=1";
			 changeMessage(s.get(SendingRegMessage,Http.getHostName()));
			 
			 UrlResult posted = Http.postEncryptedURL(Http.getHostName(),postRegisterUrl,params,null);
			 if(posted.error==null)
			 {
				 // communication worked 
				 String result = posted.text;
				 if(result.startsWith("ok")) 
				 	{ 
				 	  return(true); 
				 	}
				 else if(result.startsWith("bad")) { errorMessage = result.substring(4); }
				 else if(result.startsWith("nouser")) { errorMessage = s.get(NoUserMessage,name); }
				 else if(result.startsWith("nopassword")) { errorMessage = s.get(BadPasswordMessage); }
				 else { errorMessage = s.get(UnexpectedResponseMessage,result); }
			 }
			 else { 
				 // trouble communicating
				 errorMessage = s.get(NocontactMessage,Http.getHostName());
				 
			 }
			 
		 }
		 changeMessage(errorMessage);
		 return(false);
	 }
	 public JPanel createSuccessPanel()
	 {
		 JPanel panel =  new JPanel();
		 panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		 JLabel nameLabel = new JLabel(s.get(SuccessMessage));
		 JLabel nameLabel2 = new JLabel(s.get(SuccessMessage2));
		 panel.add(nameLabel);
		 panel.add(nameLabel2);
		 return(panel);
	 }
	 private void configureForConfirmation()
	 { 	
	 	disposeMainPanel();	// get rid of any previous
		JPanel vpanel = mainPanel = new JPanel();
		vpanel.add(createSuccessPanel());
		vpanel.add(createConfirmedPanel());
		add(vpanel);
	 }
	 //
	 // configure the form for a "register" panel.
	 //
	 private void configureForRegister()
	 {	
	 	disposeMainPanel();
	 	boolean ok = getPreregInfo();	// get timestamp and country list
		 
	 	JPanel vpanel = mainPanel = new JPanel();
		if(ok)
		{
	 	Component passPane2 = createPasswordPanel(YourPassword2,false);
	 	passwordField2 = passwordField;
	 	passwordField2.setText(password2);
	 	
	 	passPane = createPasswordPanel(YourPassword,false);
	 	passwordField.setText(password);
	 	
		vpanel.setLayout(new BoxLayout(vpanel,BoxLayout.Y_AXIS));
		
		vpanel.add(createUsernamePanel(false));
		vpanel.add(passPane);
		vpanel.add(passPane2);
		
		vpanel.add(createRealNamePanel());
		vpanel.add(createEmailPanel());
		vpanel.add(createCountryPanel());
		vpanel.add(createLanguagePanel());
	 	vpanel.add(createErrorPanel());
		}
		else {
		 	vpanel.add(createErrorPanel());
			changeMessage(s.get(NocontactMessage,Http.getHostName()));
		}
		vpanel.add(createRegisterButtonPanel());
	 	add(vpanel);
	 }
	 
	 public JPanel createPasswordPanel(String txt, boolean includeSave)
	 {
		 JPanel pane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		 
		 JLabel label = new JLabel(s.get(txt));
		 label.setLabelFor(passwordField);
		 pane.add(label);

		 
		 passwordField = new JPasswordField(13);
		 passwordField.setActionCommand(OK);
		 passwordField.addActionListener(this);
		 pane.add(passwordField);
		 
		 if(includeSave)
		 {	String passw = s.get(SavePassword);
			 savePasswordField = new Checkbox("true".equals(prefs.get(savePassKey,"true")));
			 passwordField.setText(unobfuscate(prefs.get(passKey,""),name+SALT));
			 String pass = password;
			 if(pass==null)
		 		{ String p = prefs.get(passKey,"");
		 		if(!"".equals(p)) { pass = unobfuscate(p,name+SALT); }
		 		}
			 if(pass!=null) { passwordField.setText(pass); }
			 pane.add(savePasswordField);
			 pane.add(new Label(passw));
		 }
		 return(pane);
	 }
	 

	 private JPanel createRealNamePanel()
	 {
		 JPanel panel =  new JPanel(new FlowLayout(FlowLayout.RIGHT));
		 JLabel nameLabel = new JLabel(s.get(YourRealName));
		 realNameField = new JTextField(25);
		 realNameField.setText(realName);
		 panel.add(nameLabel);
		 panel.add(realNameField);
		 
		 return(panel);
	 }
	 private JPanel createErrorPanel()
	 {	JPanel panel =  new JPanel(new FlowLayout(FlowLayout.CENTER));
	 	errorText = new JLabel(" ");
	 	errorText.setForeground(Color.red);
	 	//errorText.setWidth(80);
	 	panel.add(errorText);
	 	return(panel);
	 }

	 private JPanel createEmailPanel()
	 {
		 JPanel panel =  new JPanel(new FlowLayout(FlowLayout.RIGHT));
		 JLabel nameLabel = new JLabel(s.get(YourEmail));
		 emailField = new JTextField(25);
		 emailField.setText(email);
		 panel.add(nameLabel);
		 panel.add(emailField);
		 panel.add(new JLabel(s.get(YourEmailPromise)));
		 
		 return(panel);
		 
	 }
	 public JPanel createConfirmedPanel()
	 {
		 JPanel panel =  new JPanel();
		 finalRegisterButton = new JButton(FINALREGISTER);
		 finalRegisterButton.setActionCommand(FINALREGISTER);
		 finalRegisterButton.addActionListener(this);
		 panel.add(finalRegisterButton);
		 return(panel);
	 }

	 private JPanel createUsernamePanel(boolean includeGuest)
	 {
		 JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		 JLabel nameLabel = new JLabel(s.get(YourName));
		 nameField = new JTextField(13);
		 nameField.setActionCommand(OK);
		 panel.add(nameLabel);
		 panel.add(nameField);
		 if(name==null) { name = prefs.get(nameKey,""); }
		 if(name==null) { name = ""; }
		 nameField.setText(name); 
		 
		 if(includeGuest)
			 { String guestm = s.get(LoginAsGuest);
			   loginAsGuestField = new Checkbox(false);
			   loginAsGuestField.addItemListener(this);
			   // this papers over a codename1 bug that broke checkmarks with labels
			   panel.add(loginAsGuestField);
			   panel.add(new Label(guestm));
			 }
		 	else { loginAsGuestField = null ; }
		 
		 return(panel);
	 }
	 public JPanel createLanguagePanel()
	 {
		 JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		 String lang = language;
		 if(lang==null) { lang=prefs.get(langKey,"english"); }
		 language = lang;
		 JLabel llab = new JLabel(s.get(PREFERREDLANGUAGE));
		 langField = new Choice();
		 panel.add(llab);
		 llab.setLabelFor(langField);
		 for(String ll : InternationalStrings.languages) 
			 { langField.add(s.get(ll));
			 }
		 panel.add(langField);
		 langField.select(s.get((lang==null)?"english":lang));
		 langField.addActionListener(this);
		 //Lay out everything.
		 return(panel);
	 }
	 
	 public JPanel createCountryPanel()
	 {
		 JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		 JLabel llab = new JLabel(s.get(COUNTRY));
		 countryField = new Choice();
		 panel.add(llab);
		 llab.setLabelFor(countryField);
		 for(int i=1;i<countries.length; i++) 	// skip element 0
			 { countryField.add(countries[i]);
			 }
		 panel.add(countryField);
		 if(!"".equals(country)) { countryField.select(country); }
		 if(countryField.getSelectedItem()==null) { countryField.select(0); }
		 countryField.addItemListener(this);
		 
		 panel.add(new JLabel(s.get(CountryString)));
		 return(panel);
	 }
	 private Component createAppstoreButton()
	 {	String message = s.get(Appstore);
	 	appstoreButton = new JButton(message);
	 	appstoreButton.addActionListener(this);
	 	appstoreButton.setActionCommand(message);
	 	return(appstoreButton);
	 }
	 private Component createVisitButton()
	 {	String message = s.get(VisitSite,APPNAME);
	 	visitSiteButton = new JButton(message);
	 	visitSiteButton.addActionListener(this);
	 	visitSiteButton.setActionCommand(message);
	 	return(visitSiteButton);
	 }
	 private Component createFeedbackButton()
	 {	String message = s.get(Feedback);
	 	feedbackButton = new JButton(message);
		feedbackButton.addActionListener(this);
		feedbackButton.setActionCommand(message);
		return(feedbackButton);
	 }
	 public Component createRegisterPanel()
	 {	JPanel p = new JPanel(); 
	 	String rega = s.get(RegisterAccount);
	 	registerAccountButton = new JButton(rega);
	 	registerAccountButton.setActionCommand(rega);
	 	registerAccountButton.addActionListener(this);
 		p.add(registerAccountButton);
 		p.add(createVisitButton());
 		p.add(createFeedbackButton());
	 	return(p);
	 }
	 public boolean isAcceptableVersion()
	 {	 if(!G.isCodename1()) { return(true); }
		 String appversion = G.getAppVersion();
		 String platform = G.getPlatformName().toLowerCase();
		 String rejectVersion = G.getString(platform+"_reject_versions",null);
		 Double thisVersion = G.DoubleToken(appversion);
		 if(rejectVersion!=null)
		 {	// rejectversion is a list of rejected versions
			 String vs[] = G.split(rejectVersion,',');
			 for(String vd : vs)
			 {
				 double rejected = G.DoubleToken(vd);
				 if(rejected==thisVersion) { return(false); }
			 }
		 }
		 String minVersion = G.getString(platform+"_min_version",null);
		 if(minVersion!=null)
		 {
		 Double requiredVersion = G.DoubleToken(minVersion);
		 return(thisVersion>=requiredVersion);
		 }
		 return(true); 	// default to acceptable
	 }
	 public Component createVersionPanel()
	 {	String appversion = G.getAppVersion();
	 	String platform = G.getPlatformName().toLowerCase();
	 	String prefVersion = G.getString(platform+"_version",null);
		String va = s.get(VersionMessage,appversion);
		JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));

		if((prefVersion!=null)
	 		&&	G.isCodename1())
	 	{
	 	Double prefVersionD = G.DoubleToken(prefVersion);
	 	double appversionD = G.DoubleToken(appversion);
		// 
		// 8/2017 apple is now in a snit about prompting for updates
		//
		if(prefVersion!=null && !appversion.equals(prefVersion) 
				&& (!G.isIOS() || (appversionD<prefVersionD)))
			{
			va += " ("+s.get(VersionPreferredMessage,prefVersion)+")";
			}
		va += " "+G.build;
		p.add(new Label(va));
		if(G.isCodename1() && (!G.isIOS() || (appversionD<=prefVersionD)))
			{ p.add(createAppstoreButton()); 
	 		}
		}
		else 
		{ 
		va += " "+G.build;
		p.add(new Label(va)); 
		}
	 	return(p);
	 }
	 public Component createReviewPanel()
	 {	
		 JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER)); 
 		 String rega = s.get(ReviewMessage);
 		 reviewButton = new JButton(rega);
 		 reviewButton.setActionCommand(rega);
 		 reviewButton.addActionListener(this);
 		 p.add(reviewButton);
 		 int nservers = PlaytableStack.getNServers();
 		 serverButtons = null;
 		 if(nservers>0)
 			 { serverButtons = new JButton[nservers];
 			   for(int i=0;i<nservers;i++)
 			   {PlaytableServer ser = PlaytableStack.getNthServer(i);
 			    String msg = s.get(PlaytableMessage,ser.prettyName());
 			   	JButton button = serverButtons[i] = new JButton(msg);
			   	button.setActionCommand(msg);
 			   	button.addActionListener(this);
  			   	p.add(button);
 			   }}
  		 return(p);
	 }
	 private void autoSize()
	 {	
       	Insets ins = controllingFrame.getInsets();
    	Dimension ps = getPreferredSize();
    	Dimension newps = new Dimension((int)(ps.getWidth()+ins.left+ins.right),
    							(int)(ps.getHeight()+ins.top+ins.bottom));
    	controllingFrame.setSize(newps);
	 }
	 //
	 // create the structure for a "login" panel
	 //
	 private void configureForLogin()
	 {	
	 	disposeMainPanel();		// get rid of any previous
	 	boolean development = G.getBoolean("development",false);
	 	setBackground(FrameBackgroundColor);
	 	setOpaque(true);
	 	JPanel userPanel = createUsernamePanel(true);	// do first, side effect is to get user name
	 	passPane = createPasswordPanel(YourPassword,true);	// password pane pops up and down depending on "guest"

	 	JPanel vpanel = mainPanel = new JPanel();
	 	vpanel.setOpaque(false);
		vpanel.setLayout(new BoxLayout(vpanel,BoxLayout.Y_AXIS));
		if(development)
		{
			vpanel.add(createHostPanel(G.getString(DEVELOPHOST,null)));
		}
		vpanel.add(userPanel);
		vpanel.add(passPane); 
		vpanel.add(createLanguagePanel());
		vpanel.add(createLoginButtonPanel());
		vpanel.add(createRegisterPanel());
		vpanel.add(createReviewPanel());
		vpanel.add(createVersionPanel());
		adjustGuestPassword(isGuest || guestName.equalsIgnoreCase(name),false);

		add(vpanel);
	 }

	 //
	 // create the structure for a "login" panel
	 //
	 private void configureForRejection()
	 {	
	 	disposeMainPanel();		// get rid of any previous
	 	setBackground(FrameBackgroundColor);
	 	
	 	JPanel vpanel = mainPanel = new JPanel();
	 	TextArea text = new TextArea();
	 	text.setText(s.get(VersionRejectedMessage));
		vpanel.add(text);
		vpanel.add(createAppstoreButton()); 
		add(vpanel);
	 }

	 
	 private String unobfuscate(String password,String salt)
	 {
		 byte []bytes = Base64.decode(password);
		 for(int i=0;i<bytes.length;i++) { bytes[i]=bytes[i] ^= ((salt.charAt(i%salt.length()))+(((i+163)*17253))&0xff); }
		 return(new String(bytes));
	 }
	 private String obfuscate(String password,String salt)
	 {	byte []bytes = password.getBytes();
		 for(int i=0;i<bytes.length;i++) { bytes[i]=bytes[i] ^= ((salt.charAt(i%salt.length()))+(((i+163)*17253))&0xff); }
		 return(Base64.encode(bytes,true));
	 }

	 // create the panel with the OK and Cancel buttons
	 protected JPanel createLoginButtonPanel() {
		 JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
		 {
		 String ok = s.get(OK);
		 okButton = new JButton(ok);
		 okButton.setActionCommand(ok);
		 okButton.addActionListener(this);
		 p.add(okButton);
		 }
		 {
	     String can = s.get(cancel);
		 cancelButton = new JButton(can);
		 cancelButton.setActionCommand(can);
		 cancelButton.addActionListener(this);
		 p.add(cancelButton);       
		 }
		 return p;
    }

	 // create the panel with the OK and Cancel buttons
	 protected JPanel createHostPanel(String alt) {
		 JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
		 {String host = "use host "+alt;
		  Checkbox ch = new Checkbox(G.getBoolean("develophostoption",false));
		  ch.addItemListener(this);
		  developHostOption = ch;
		  p.add(ch);
		  p.add(new Label(host));
		  Checkbox ch1 = new Checkbox(false);
		  develop2xOption = ch1;
		  G.dpiMultiplier = 1;
		  ch1.addItemListener(this);
		  p.add(ch1);
		  p.add(new Label("2x"));
		 }
		 return p;
    } 
	 // create the panel with the OK and Cancel buttons
	 protected JPanel createRegisterButtonPanel() {
		 
		 JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER));
		 {
		 String ok = s.get(REGISTER);
		 registerButton = new JButton(ok);
		 registerButton.setActionCommand(REGISTER);
		 registerButton.addActionListener(this);
		 p.add(registerButton);
		 }
		 {
	     String can = s.get(cancel);
		 cancelButton = new JButton(can);
		 cancelButton.setActionCommand(cancel);
		 cancelButton.addActionListener(this);
		 p.add(cancelButton);       
		 }
		 return p;
    }
	 
    public String exitValue=null;

    private static boolean isGuest = false;
    public static String password=null;
    public static String name = "";
    public static String language = null;
    private static String realName = "";
    private static String password2 = "";
    private static String country = "";
    private static String email = "";
    @SuppressWarnings("deprecation")
    public void captureValues(boolean always)
    {	if(always || !isGuest)
    	{
        if(passwordField!=null) { password = new String(passwordField.getPassword()); }
        if(nameField!=null) { name = nameField.getText(); }
        if(emailField!=null) { email = emailField.getText(); }
        if(realNameField!=null) { realName = realNameField.getText(); }
        if(passwordField2!=null) { password2 = passwordField2.getText(); }
        if(countryField!=null) { country = countryField.getSelectedItem().toString(); }
    	}
    	if(langField!=null)
    		{ String old = language;
    		  if(!getSelectedLanguage().equals(old))
    		  {
    		  G.putGlobal(G.LANGUAGE, language);
    		  // in the registration screen, the language pop-up hung around for
    		  // unknown reasons.  This waits and runs the changes later
    		  G.runInEdt(new Runnable() 
    		  { public void run() 
    		  {initLanguage();
    		  reconfigure();}
    		  });
    		  }
    		}

    }
	public void actionPerformed(ActionEvent e) 
	{	Object source = e.getSource();
        Object cmd1 = e.getActionCommand();
        captureValues(false);
        if(cmd1!=null)
        {
        if(source==cancelButton) 
        	{ exitValue = cancel; 
        	}
        if(source==appstoreButton)
        {	String url = G.isIOS() 
        					? iosAppStoreUrl 
        					: G.isAmazon() 
        						? amazonAppStoreUrl
        						: androidAppStoreUrl;
         	G.showDocument(url);
        }
        if(source==feedbackButton)
        {	
        	G.showDocument(feedbackUrl+"?subject=feedback%20for%20"
        			+ G.getPlatformName()
        			+ "%20"
        			+ G.getAppVersion());
        }
        if(source==visitSiteButton)
        {
        	G.showDocument(Http.httpProtocol+"//"+Http.getHostName()+homepageUrl);
        }
        if(source==registerAccountButton)
        {	// call the site, not allowed by the apple police.
        	//G.showDocument(Http.getHostName()+"/"+G.replace(registerUrl,"english",language));
        	reconfigure(Screen.Register);
        	return;
        }
        else if((source==finalRegisterButton) || FINALREGISTER.equals(cmd1))
        {
        	reconfigure(Screen.Login);
        	return;
        }
        else if((source==registerButton) || REGISTER.equals(cmd1))
    	{
         if(performRegistration())
         {
        	 reconfigure(Screen.Confirmation);
    	 try { prefs.flush(); } 
    	 		catch (BackingStoreException err) 
    			{ System.out.println("E "+err.toString());};
    			
    		return;
         }
         else 
         {
        	 // remain in the registration page
        	 return;
         }
   		}
        if((source==reviewButton) || ReviewMessage.equals(cmd1))
        {
        	exitValue = ReviewMessage;
        	G.setOffline(true);
         	exit();
        }
        if(serverButtons!=null)
        {
        	for(int i=0;i<serverButtons.length;i++)
        	{
        		if(source==serverButtons[i])
        		{	exitValue = PlaytableMessage;
        			PlaytableStack.setSelectedServer(i);
        		}
        	}
        }
        if((source==okButton) || OK.equals(cmd1))
        	{boolean savePass = G.getState(savePasswordField);
        	 if(!isGuest)
        	 {
        	 prefs.put(nameKey,name);
        	 prefs.put(passKey,savePass? obfuscate(password,name+SALT) : "");
        	 // use strings, because standard java doesn't support booleans
        	 prefs.put(savePassKey,""+(savePass?"true":"false"));
        	 }
        	 captureValues(true);
        	 exitValue = OK;
        	 try { prefs.flush(); } 
        			catch (BackingStoreException err) 
        			{ System.out.println("E "+err.toString());};
        	}
         exit();
         
       }
    }
	public void exit()
	{	G.wake(observer);
		suicide();
	}
	public void suicide()
	{	controllingFrame.setVisible(false);
		//for unknown reasons, this sometimes locks up waiting for something.
		controllingFrame.dispose();
	}

    //Must be called from the event dispatch thread.
    protected void resetFocus() {
        nameField.requestFocusInWindow();
    }

    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    public static PasswordCollector createAndShowGUI(SimpleObserver parent) 
    {	PasswordCollector result[] = new PasswordCollector[1];
    	G.runInEdt(new Runnable() { public void run() { createGui(parent,result); }});
    	return(result[0]);
    }
    public static void createGui(SimpleObserver parent,PasswordCollector[]result)
    {
    	String host = Http.getHostName();
      	boolean isTest = G.getBoolean(TESTSERVER,false);
    	password = null;
    	name = null;
    	language = null;
    	reloadAppletParameters();
       	isGuest = false;
    	if(language==null) { initLanguage(); }
    	//Create and set up the window.
        JFrame frame = new XFrame(s.get(LogInTo,(isTest ? "test for " : "")
        		+(host.startsWith("www")?host:G.Capitalize(host))));
        //Create and set up the content pane.
        Image icon = Image.getImage(IMAGEPATH+CommonConfig.icon_image_name);
        frame.setIconAsImage(icon);
        final PasswordCollector newContentPane = new PasswordCollector(frame,parent);
        
        newContentPane.setOpaque(true); //content panes must be opaque
        frame.setContentPane(newContentPane);
        frame.addWindowListener(newContentPane);
        
        // large screen devices with lots of pixels held vertically
        // were configuring themselves with giant fonts and scrolling
        // in the X direction
        //newContentPane.setScrollableX(false);
        newContentPane.setScrollableY(true);
    	
        //Make sure the focus goes to the right component
        //whenever the frame is initially given the focus.
        frame.addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                newContentPane.resetFocus();
            }
        });

        //Display the window.
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.repaint();
        result[0]=newContentPane;
    }

	public void windowActivated(WindowEvent e) {
	}
	public void windowClosed(WindowEvent e) {
	}
	public void windowClosing(WindowEvent e) {
		if(exitValue==null) { exitValue = cancel; }	// this is so closing the window while in the dialog will exit and recycle
		G.wake(observer);
	}
	public void windowDeactivated(WindowEvent e) {
	
	}
	public void windowDeiconified(WindowEvent e) {
	}
	public void windowIconified(WindowEvent e) {
	
	}
	public void windowOpened(WindowEvent e) {
		
	}
	public void adjustGuestPassword(boolean beGuest,boolean reset)
	{
		isGuest = beGuest;
		if(passPane!=null) { passPane.setVisible(!beGuest); }	
		nameField.setEditable(!beGuest);
		if(reset || beGuest) 
			{ nameField.setText( beGuest ?  guestName : name); 
			  passwordField.setText(beGuest? guestName : password); 
			}
		if(loginAsGuestField!=null) { loginAsGuestField.setState(beGuest); }
	}
	
	static public void reloadAppletParameters()
	{	
		String host = Http.getHostName();
		String params = "&tagname=mobileinfo"+Login.platformString();
		UrlResult result = Http.postEncryptedURL(host,getEncryptedURL,params,null);
		if((result!=null) && (result.error==null) && (result.text!=null))
		{
			parseInfoGroup(new StringTokenizer(result.text));
		}
		else {
	    		G.infoBox("Login error","Can't contact "+host);
	    	}
	}
	public void itemStateChanged(ItemEvent e) {
		Object source = e.getSource();
		captureValues(false);
        if(source==developHostOption)
        {	boolean newState = G.getState(developHostOption);
        	String newHost = newState 
        						? G.getString(DEVELOPHOST,null) 
        						: G.getString(RELEASEHOST,null);
         	Http.setHostName(newHost);
         	G.putGlobal(SERVERNAME,newHost);
         	G.putGlobal("develophostoption",newState?"true":"false");
        	G.print("Host set to "+newHost);
        	exitValue = cancel;
        	exit();
        }
        if(source==develop2xOption)
        {
        	G.dpiMultiplier = G.getState(develop2xOption) ? 2 : 1;
        }
		if(source == loginAsGuestField)
		{	boolean isGuest = G.getState(loginAsGuestField);
		    adjustGuestPassword(isGuest,true);
		
		}
		if(source==langField)
		{	// in the registration screen, the language pop-up hung around for
			// unknown reasons.  This waits and runs the changes later
			language = langField.getSelectedItem().toString();
			G.putGlobal(G.LANGUAGE, language);
	        G.startInEdt(new Runnable() 
	        	{ public void run() {initLanguage();reconfigure();}
	        	});
		}
		repaint();
	}

}