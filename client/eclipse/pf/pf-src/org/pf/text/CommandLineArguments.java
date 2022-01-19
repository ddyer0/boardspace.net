// ===========================================================================
// CONTENT  : CLASS CommandLineArguments
// AUTHOR   : Manfred Duchrow
// VERSION  : 2.0 - 12/01/2014
// HISTORY  :
//  16/07/2002  duma  CREATED
//	06/03/2003	duma	added		->	addOption(), addOptionWithArgument(), copy(), 
//																removeOption(), removeOptionWithArgument(),
//																constructor with switchIndicator
//																throw IllegalArgumentException if option is null
//	07/03/2004	duma	added		->	Support for option with immediate following value
//	01/07/2006	mdu		added		->	No argument constructor
//	23/08/2006	mdu		added		->	getOptionValues()
//	22/02/2008	mdu		added		->  getArgumentValue(String,String)
//	16/10/2011	mdu		added		->	switchAndValuesMustBeSeparated
//  12/01/2014  mdu   added   ->  supportQuotedArguments
//
// Copyright (c) 2002-2014, by Manfred Duchrow. All rights reserved.
// ===========================================================================
package org.pf.text;

// ===========================================================================
// IMPORTS
// ===========================================================================
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for easy evaluation of command line arguments.
 *
 * @author Manfred Duchrow
 * @version 2.0
 */
public class CommandLineArguments
{
  // =========================================================================
  // CONSTANTS
  // =========================================================================
  protected static final String STRING_DELIMITER = "\"";
  protected static final String DEFAULT_SWITCH_INDICATOR = "-";
  protected static final String QUOTE = "'";

  // =========================================================================
  // INSTANCE VARIABLES
  // =========================================================================
  private String[] arguments = null;
  private String switchIndicator = DEFAULT_SWITCH_INDICATOR;
  private Map<String, String[]> optionsWithValues = null;
  private boolean switchAndValuesMustBeSeparated = true;
  private boolean supportQuotedArguments = false ;

  // =========================================================================
  // CLASS METHODS
  // =========================================================================
  /**
   * Convenience method with varargs to create a new instance.
   * 
   * @param options The options to be used as arguments.
   * @return A new instance of CommandLineArguments initialized with the specified options.
   */
  public static CommandLineArguments create(String... options)
  {
    return new CommandLineArguments(options);
  } // create()

  // =========================================================================
  // CONSTRUCTORS
  // =========================================================================
  /**
   * Initialize the new instance with no arguments.
   */
  public CommandLineArguments()
  {
    this(null, DEFAULT_SWITCH_INDICATOR);
  } // CommandLineArguments() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance to handle arguments with separated
   * option flag and option value.
   * 
   * @param switchAndValuesMustBeSeparated If true no switches with length > 2 
   * will be treated as a one character option switch plus the remaining string as its value.
   */
  public CommandLineArguments(boolean switchAndValuesMustBeSeparated)
  {
    this(null, DEFAULT_SWITCH_INDICATOR, switchAndValuesMustBeSeparated);
  } // CommandLineArguments() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an array of arguments.
   * 
   * @param args The arguments of the command line
   */
  public CommandLineArguments(String[] args)
  {
    this(args, DEFAULT_SWITCH_INDICATOR);
  } // CommandLineArguments() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an array of arguments.
   * 
   * @param args The arguments of the command line
   * @param switchAndValuesMustBeSeparated If true no switches with length > 2 
   * will be treated as a one character option switch plus the remaining string as its value.
   */
  public CommandLineArguments(String[] args, boolean switchAndValuesMustBeSeparated)
  {
    this(args, DEFAULT_SWITCH_INDICATOR, switchAndValuesMustBeSeparated);
  } // CommandLineArguments() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an array of arguments and a prefix 
   * indicator for options.
   * 
   * @param args The arguments of the command line
   * @param switchIndicator A prefix for options
   */
  public CommandLineArguments(String[] args, String switchIndicator)
  {
    this(args, switchIndicator, true);
  } // CommandLineArguments() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an array of arguments and a prefix 
   * indicator for options.
   * 
   * @param args The arguments of the command line
   * @param switchIndicator A prefix for options
   * @param switchAndValuesMustBeSeparated If true no switches with length > 2 
   * will be treated as a one character option switch plus the remaining string as its value.
   */
  public CommandLineArguments(String[] args, String switchIndicator, boolean switchAndValuesMustBeSeparated)
  {
    this(args, switchIndicator, switchAndValuesMustBeSeparated, false);
  } // CommandLineArguments() 

  // -------------------------------------------------------------------------

  /**
   * Initialize the new instance with an array of arguments and a prefix 
   * indicator for options.
   * 
   * @param args The arguments of the command line
   * @param switchIndicator A prefix for options
   * @param switchAndValuesMustBeSeparated If true no switches with length > 2 
   * will be treated as a one character option switch plus the remaining string as its value.
   * @param supportQuotedArguments Handle wildcard patterns enclosed in quotes.
   */
  protected CommandLineArguments(String[] args, String switchIndicator, boolean switchAndValuesMustBeSeparated, boolean supportQuotedArguments)
  {
    super();
    
    this.setSwitchAndValuesMustBeSeparated(switchAndValuesMustBeSeparated);
    if (args == null)
    {
      this.arguments(StringUtil.EMPTY_STRING_ARRAY);
    }
    else
    {
      this.arguments(this.strUtil().copy(args));
    }
    this.supportQuotedArguments = supportQuotedArguments;
    if (switchIndicator != null)
    {
      this.setSwitchIndicator(switchIndicator);
    }
    else
    {
      this.init();
    }
  } // CommandLineArguments() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PUBLIC INSTANCE METHODS
  // =========================================================================
  /**
   * Returns a String with all arguments separated by blanks
   */
  @Override
  public String toString()
  {
    return this.strUtil().asString(this.arguments(), " ");
  } // toString() 

  // -------------------------------------------------------------------------

  /**
   * Returns whether or not the specified option is set in the command line
   * arguments. The search for the option is case-sensitive. <p>
   * Examples:  
   * <ul>
   * boolean append = commandLine.isOptionSet( "-a" ) ; <br>
   * boolean move = commandLine.isOptionSet( "/move" ) ;
   * </ul>
   * 
   * @param option The option to be looked for
   * @throws IllegalArgumentException If the given option is null
   */
  public boolean isOptionSet(String option)
  {
    if (option == null)
    {
      throw new IllegalArgumentException("option is null");
    }
    return this.isOption(option) && (this.findOptionValue(option) != null);
  } // isOptionSet() 

  // -------------------------------------------------------------------------

  /**
   * Returns whether or not the specified argument was specified in the command line.
   * The search for the option is case-sensitive. <p>
   * Examples:  
   * <ul>
   * commandLine = new CommandLineArguments("-a -f file.txt");
   * boolean append = commandLine.containsArgument( "-a" ) ; <br>
   * boolean move = commandLine.containsArgument( "-f" ) ;
   * boolean file = commandLine.containsArgument( "file.txt" ) ;
   * </ul>
   * 
   * @param argName The name of the argument to look for
   * @throws IllegalArgumentException If the given argName is null
   */
  public boolean containsArgument(String argName)
  {
    if (argName == null)
    {
      throw new IllegalArgumentException("argName is null");
    }
    return strUtil().contains(this.getArguments(), argName);
  } // containsArgument() 

  // -------------------------------------------------------------------------

  /**
   * Returns the value following the specified option.
   * Returns the given default value if the specified option doesn't occur 
   * at all in the arguments array. <br>
   * For more details see {@link #getArgumentValue(String)}.
   * 
   * @param option The option which indicates that the next argument is the desired value
   * @param defaultValue The default value to return if the option is not found
   * @throws IllegalArgumentException If the given option is null
   */
  public String getArgumentValue(String option, String defaultValue)
  {
    String value;

    value = this.getArgumentValue(option);
    if (value == null)
    {
      return defaultValue;
    }
    return value;
  } // getArgumentValue() 

  // -------------------------------------------------------------------------

  /**
   * Returns the value following the specified option.
   * Returns null if the specified option doesn't occur at all in the arguments
   * array. <br>
   * An empty String will be returned if the option is followed by another 
   * option (detected by the switchIndicator prefix) or no further argument 
   * follows the specified option. 
   * <br>
   * If an argument exists that starts with the given option name then the rest
   * of the argument is returned as the option's value.
   * <p>
   * Examples:<p>
   *  Arguments: -v -f sample.xml -t <br>
   *  getArgumentValue( "-f" ) returns "sample.xml"<p>
   *  Arguments: -v -m -t <br>
   *  getArgumentValue( "-f" ) returns null<p>
   *  Arguments: -v -t -f <br>
   *  getArgumentValue( "-f" ) returns ""<p>
   *  Arguments: -v -f -t sample.xml<br>
   *  getArgumentValue( "-f" ) returns ""<p>
   *  Arguments: -v -x2000 -t<br>
   *  getArgumentValue( "-x" ) returns "2000"<p>
   *  Arguments: -v"first draft" -x -t<br>
   *  getArgumentValue( "-v" ) returns "first draft"<p>
   * 
   * @param option The option which indicates that the next argument is the desired value
   * @throws IllegalArgumentException If the given option is null
   */
  public String getArgumentValue(String option)
  {
    if (option == null)
    {
      throw new IllegalArgumentException("option is null");
    }

    return this.findOptionValue(option);
  } // getArgumentValue() 

  // -------------------------------------------------------------------------

  /**
   * Returns all values that are found for the specified option.
   * Helps to collect all values for options that can be used more than once.
   * <p>
   * Example:
   * <br>
   *  Arguments: -v -x2000 -t -x 120 -xFM<br>
   *  getArgumentValues( "-x" ) returns { "2000", "120", "FM" }<p>
   * 
   * @param option The option which is the prefix for the desired values
   * @throws IllegalArgumentException If the given option is null
   */
  public String[] getArgumentValues(String option)
  {
    if (option == null)
    {
      throw new IllegalArgumentException("option is null");
    }

    return this.findValues(option);
  } // getArgumentValues() 

  // -------------------------------------------------------------------------

  /**
   * Returns the values following the specified option.
   * Returns the given default values if the specified option doesn't occur 
   * at all in the arguments array. <br>
   * For more details see {@link #getArgumentValue(String, String)}.
   * 
   * @param option The option which indicates that the next arguments are the desired values
   * @param defaultValues The default values to return if the option is not found
   * @throws IllegalArgumentException If the given option is null
   */
  public String[] getArgumentValues(String option, String... defaultValues)
  {
    String[] values;

    values = this.getArgumentValues(option);
    if (values == null)
    {
      return defaultValues;
    }
    return values;
  } // getArgumentValues() 

  // -------------------------------------------------------------------------

  /**
   * Returns all values after the given option which do NOT start with the
   * configured switch indicator.
   * Returns null if the specified option doesn't occur at all in the arguments
   * array.
   * <p>
   * Example:
   * <br>
   *  Arguments: -v -x 2000 south 30 west -t -f test.txt -Uc22<br>
   *  getOptionValues( "-x" ) returns { "2000", "south", "30", "west }<br>
   *  getOptionValues( "-v" ) returns String[0] <br>
   *  getOptionValues( "-f" ) returns { "test.txt" } <br>
   *  getOptionValues( "-M" ) returns null <br>
   * 
   * @param option The option which is the prefix for the desired values
   * @throws IllegalArgumentException If the given option is null
   */
  public String[] getOptionValues(String option)
  {
    int index;
    String[] args;
    List strings;

    if (option == null)
    {
      throw new IllegalArgumentException("option is null");
    }
    args = this.getArguments();
    index = this.strUtil().indexOf(args, option);
    if (index < 0)
    {
      return null;
    }
    strings = new ArrayList(args.length);
    for (int i = index + 1; i < args.length; i++)
    {
      if (args[i].startsWith(this.getSwitchIndicator()))
      {
        break;
      }
      strings.add(args[i]);
    }
    if (strings.isEmpty())
    {
      return StringUtil.EMPTY_STRING_ARRAY;
    }
    return this.strUtil().asStrings(strings);
  } // getOptionValues() 

  // -------------------------------------------------------------------------

  /**
   * Adds the given option to the command line. If it is already set it is
   * not added a second time.   <br>
   * 
   * @param option An option including the switch indicator if necessary (e.g. "-x")
   * @throws IllegalArgumentException If the given option is null
   */
  public void addOption(String option)
  {
    String[] newArgs;

    if (option == null)
    {
      throw new IllegalArgumentException("option is null");
    }

    if (!this.isOptionSet(option))
    {
      newArgs = this.strUtil().append(this.arguments(), option);
      this.arguments(newArgs);
      this.init();
    }
  } // addOption() 

  // -------------------------------------------------------------------------

  /**
   * Adds the given option to the command line. If it is already set it is
   * not added a second time.   <br>
   * 
   * @param option An option including the switch indicator if necessary (e.g. "-x")
   * @param argument The argument of the option
   * @throws IllegalArgumentException If the given option or argument is null
   */
  public void addOptionWithArgument(String option, String argument)
  {
    String[] newArgs;

    if (option == null)
    {
      throw new IllegalArgumentException("option is null");
    }
    if (argument == null)
    {
      throw new IllegalArgumentException("argument is null");
    }

    if (!this.isOptionSet(option))
    {
      String[] add = { option, argument };
      newArgs = this.strUtil().append(this.arguments(), add);
      this.arguments(newArgs);
      this.init();
    }
  } // addOptionWithArgument() 

  // -------------------------------------------------------------------------

  /**
   * Remove the specified option from the command line arguments.
   * 
   * @param option The option to be removed
   */
  public void removeOption(String option)
  {
    if (option == null)
    {
      throw new IllegalArgumentException("option is null");
    }
    this.arguments(this.strUtil().remove(this.arguments(), option));
    this.init();
  } // removeOption() 

  // -------------------------------------------------------------------------

  /**
   * Remove the specified option from the command line arguments.
   * If there's an argument following the option, it will be removed as well.
   * 
   * @param option The option to be removed
   */
  public void removeOptionWithArgument(String option)
  {
    int index;
    String[] args;

    if (option == null)
    {
      throw new IllegalArgumentException("option is null");
    }
    args = this.arguments();
    index = strUtil().indexOf(args, option);
    if (index < 0)
    {
      return;
    }
    args[index] = null;
    index++;
    if (index < this.arguments().length)
    {
      if (!this.isOption(args[index]))
      {
        args[index] = null;
      }
    }
    this.arguments(this.strUtil().removeNull(this.arguments()));
    this.init();
  } // removeOptionWithArgument() 

  // -------------------------------------------------------------------------

  /**
   * Returns a copy of this object, with all internal state being the same
   * as in the original.
   */
  public CommandLineArguments copy()
  {
    CommandLineArguments aCopy;

    aCopy = new CommandLineArguments(this.strUtil().copy(this.arguments()), this.getSwitchIndicator(), this.getSwitchAndValuesMustBeSeparated(), this.getSupportQuotedArguments());
    return aCopy;
  } // copy() 

  // -------------------------------------------------------------------------	

  /**
   * Returns the prefix which must proceed each command line switch (option).
   * The default value is "-".
   */
  public String getSwitchIndicator()
  {
    return this.switchIndicator();
  } // getSwitchIndicator() 

  // -------------------------------------------------------------------------

  /**
   * Sets the prefix which must proceed each command line switch.
   * For example to change to Windows style call setSwitchIndicator( "/" )
   */
  public void setSwitchIndicator(String newValue)
  {
    this.switchIndicator(newValue);
    this.init();
  } // setSwitchIndicator() 

  // -------------------------------------------------------------------------

  /**
   * Returns the current number of argument
   */
  public int size()
  {
    return this.arguments().length;
  } // size() 

  // -------------------------------------------------------------------------

  /**
   * Returns a string array containing all arguments.
   * The array will be a copy, so modifications of that array will not alter
   * any argument in this object. 
   */
  public String[] getArguments()
  {
    return this.copyOfArguments();
  } // getArguments() 

  // -------------------------------------------------------------------------

  /**
   * Returns the argument at the specified index or null if the index is 
   * outside the bounds of the argument list.
   */
  public String getArgumentAt(int index)
  {
    if ((index < 0) || (index >= this.size()))
    {
      return null;
    }
    return this.unquotedIfSupported(this.arguments()[index]);
  } // getArgumentAt() 

  // -------------------------------------------------------------------------

  /**
   * Returns whether or not quoted arguments are supported.
   * The default if not explicitly set is: false.
   * <p/>
   * If true, arguments starting and ending with a single quote "'" will
   * be stripped off these delimiters. This is useful to allow arguments
   * with wildcard characters like '*.*'. Without the quotes such patterns
   * might be resolved by the OS (e.g. Windows) to filenames which are then 
   * passed as arguments to the Java program.
   */
  public boolean getSupportQuotedArguments()
  {
    return supportQuotedArguments;
  } // getSupportQuotedArguments() 
  
  // -------------------------------------------------------------------------
  
  /**
   * Defines whether or not quoted arguments are supported.
   * <p/>
   * If true, arguments starting and ending with a single quote "'" will
   * be stripped off these delimiters. This is useful to allow arguments
   * with wildcard characters like '*.*'. Without the quotes such patterns
   * might be resolved by the OS (e.g. Windows) to filenames which are then 
   * passed as arguments to the Java program.
   */
  public void setSupportQuotedArguments(boolean supportThem)
  {
    if (this.supportQuotedArguments != supportThem)
    {      
      this.supportQuotedArguments = supportThem;
      this.init();
    }
  } // setSupportQuotedArguments() 
  
  // -------------------------------------------------------------------------
  
  // =========================================================================
  // PROTECTED INSTANCE METHODS
  // =========================================================================
  protected void init()
  {
    Map<String, String[]> namedValues;
    String option;
    String value;
    String[] args;
    List<String> valueList;

    namedValues = new HashMap();
    args = this.arguments();
    for (int i = 0; i < args.length; i++)
    {
      option = args[i];
      value = null;
      if (this.isOption(option))
      {
        if (!this.getSwitchAndValuesMustBeSeparated() && (option.length() > 2))
        {
          value = option.substring(2);
          option = option.substring(0, 2);
          this.addOptionWithValue(namedValues, option, this.unquotedIfSupported(value));
        }

        option = args[i];
        valueList = new ArrayList();
        if (i < args.length - 1)
        {
          for (int j = i + 1; (j < args.length) && !this.isOption(args[j]); j++)
          {
            valueList.add(this.unquotedIfSupported(args[j]));
          }
          if (valueList.isEmpty())
          {
            this.addOptionWithValue(namedValues, option, StringUtil.EMPTY_STRING);
          }
          else
          {
            this.addOptionWithValues(namedValues, option, valueList);
          }
        }
        else
        {
          this.addOptionWithValue(namedValues, option, StringUtil.EMPTY_STRING);
        }
      }
    }
    this.setOptionsWithValues(namedValues);
  } // init() 

  // -------------------------------------------------------------------------

  protected void addOptionWithValue(Map<String, String[]> map, String option, String value)
  {
    String[] values;

    if ((option != null) && (value != null))
    {
      values = map.get(option);
      if (values == null)
      {
        values = StringUtil.EMPTY_STRING_ARRAY;
      }
      values = this.strUtil().append(values, value);
      map.put(option, values);
    }
  } // addOptionWithValue() 

  // -------------------------------------------------------------------------

  protected void addOptionWithValues(Map<String, String[]> map, String option, List<String> values)
  {
    for (String value : values)
    {
      this.addOptionWithValue(map, option, value);
    }
  } // addOptionWithValues() 

  // -------------------------------------------------------------------------

  protected boolean isOption(String arg)
  {
    return arg.startsWith(this.getSwitchIndicator());
  } // isOption() 

  // -------------------------------------------------------------------------

  protected String findOptionValue(String option)
  {
    String[] values;

    values = this.findValues(option);
    if (this.strUtil().isNullOrEmpty(values))
    {
      return null;
    }
    return values[0];
  } // findOptionValue() 

  // -------------------------------------------------------------------------

  protected String[] findValues(String option)
  {
    return this.getOptionsWithValues().get(option);
  } // findValues() 

  // -------------------------------------------------------------------------
  
  /**
   * Returns a copy of the original arguments. If supportQuotedArguments is true
   * the quotes will be removed in the returned array.
   */
  protected String[] copyOfArguments() 
  {
    String[] result;
    
    result = new String[this.arguments().length];
    for (int i = 0; i < this.arguments().length; i++)
    {
      result[i] = this.unquotedIfSupported(this.arguments()[i]);
    }
    return result;
  } // copyOfArguments()
  
  // -------------------------------------------------------------------------

  protected String unquotedIfSupported(final String value) 
  {
    if (this.getSupportQuotedArguments())
    {
      return this.unquoted(value);
    }
    return value;
  } // unquoted() 
  
  // -------------------------------------------------------------------------
  
  protected String unquoted(final String value) 
  {
    if (value.startsWith(QUOTE) && value.endsWith(QUOTE))
    {
      return this.strUtil().getDelimitedSubstring(value, QUOTE);
    }
    return value;
  } // unquoted() 
  
  // -------------------------------------------------------------------------
  
  protected String[] arguments()
  {
    return arguments;
  } // arguments() 
  
  // -------------------------------------------------------------------------
  
  protected void arguments(String[] newValue)
  {
    arguments = newValue;
  } // arguments() 

  // -------------------------------------------------------------------------
  
  protected String switchIndicator()
  {
    return switchIndicator;
  } // switchIndicator() 
  
  // -------------------------------------------------------------------------
  
  protected void switchIndicator(String newValue)
  {
    switchIndicator = newValue;
  } // switchIndicator() 

  // -------------------------------------------------------------------------
  
  protected Map<String, String[]> getOptionsWithValues()
  {
    return optionsWithValues;
  } // getOptionsWithValues() 
  
  // -------------------------------------------------------------------------
  
  protected void setOptionsWithValues(Map<String, String[]> newValue)
  {
    optionsWithValues = newValue;
  } // setOptionsWithValues() 

  // -------------------------------------------------------------------------
  protected boolean getSwitchAndValuesMustBeSeparated()
  {
    return switchAndValuesMustBeSeparated;
  } // getSwitchAndValuesMustBeSeparated() 
  
  // -------------------------------------------------------------------------
  
  protected void setSwitchAndValuesMustBeSeparated(boolean newValue)
  {
    switchAndValuesMustBeSeparated = newValue;
  } // setSwitchAndValuesMustBeSeparated() 

  // -------------------------------------------------------------------------
  
  protected StringUtil strUtil()
  {
    return StringUtil.current();
  } // strUtil() 

  // -------------------------------------------------------------------------

} // class CommandLineArguments 
