

package com.asdnetworks.nounphrase.english;



import com.asdnetworks.nounphrase.asd.ASDGrammar;
import com.asdnetworks.nounphrase.asd.ASDGrammarNode;
import com.asdnetworks.nounphrase.asd.ASDParser;
import com.asdnetworks.nounphrase.asd.ASDPhraseNode;
import java.io.*;
import java.util.*;
import java.awt.*;       // Font
import java.awt.event.*; // ActionEvent, ActionListener,
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.swing.JOptionPane;                    // WindowAdapter, WindowEvent
import javax.swing.*;    // JFrame, JPanel
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;


   
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
   
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/

/**
   Nounphrase1 illustrates parsing with a partial grammar for English noun
   phrases.  It provides a graphical user interface that permits the user
   to initialize a parse, to attempt complete parses of a given phrase,
   and, when a parse is successful, to display the phrase structure and
   the semantic values computed for the phrase.  Of the public methods in
   this class, the only one that the user need be concerned with
   directly is the main method.  The remaining public methods are invoked
   by an ASDParser from instances in the ASDGrammar nounphrasesimple.grm.
<BR><BR>
   Command-line usage:
   <BR>In an MS-Windows command-line window:
   <BR><tt><b> java -cp asddigraphs.jar;english.jar;. english/Nounphrase1</b></tt>
   <BR>Or under UNIX:
   <BR><tt><b> java -cp asddigraphs.jar:english.jar:. english/Nounphrase1</b></tt>
   <BR>OR if asddigraphs.jar and asdx.jar have been put in the system classpath:
   <BR><tt><b> java english/Nounphrase1</b></tt>

   @author James A. Mason
   @version 1.01 2002 February
 */
public class NounPhrase
{  public static void main(String[] args) throws URISyntaxException
   {  new NounPhrase();
   }

   NounPhrase() throws URISyntaxException
   {  window = new NounPhraseWindow(this);
      window.setTitle("Nounphrase1 - version " + VERSION);
      window.setVisible(true);
      parser = new ASDParser(this);
         // Use the parser itself as the semantics,
         // and the Nounphrase1 instance as the application.

      /*
      URL dir_url = ClassLoader.getSystemResource("com/asdnetworks/nounphrase/resources");
      File dir = new File(dir_url.toURI());
      String[] files = dir.list();
      System.out.println(dir_url);*/
     
      
     grammarFileName = GRAMMARFILENAME;
      
      if (parser.useGrammar(grammarFileName))
         grammarLoaded = true;
      else
      {  JOptionPane.showMessageDialog(window,
            "Grammar file " + grammarFileName + " could not be loaded.");
         System.exit(0);
      }
   
    
      expectedTypes = EXPECTEDTYPES;
   }

   
   
   boolean completeParse()
   {  if (utterance == null || utterance.equals(""))
         return false;
      parseCompleted = false;
      String advanceResult; // SUCCEED, NOADVANCE, or QUIT

      while(steps < MAXSTEPS)
      {  advanceResult = parser.advance();
         if (advanceResult.equals(parser.QUIT))
         {  window.getOutputPane().append(
            "\nParse quit after " + steps + " advance steps.\n");
            parseCompleted = false;
            return false;
         }
         else if (advanceResult.equals(parser.SUCCEED))
         {  ++steps;
            if (parser.done())
            {  window.getOutputPane().append(
               "\nSuccessful parse in " + steps + " advance steps:\n");
               showBracketedPhrase();
               parseCompleted = true;
               showSemanticValue();
               steps = 0; // prepare for an attempt at an alternative parse
               return true;
            }
         }
         else if (advanceResult.equals(parser.NOADVANCE))
         {  if (!parser.backup())
            {  if (strict)
                  initializeParse(false);  // for non-strict parsing
               else
               {  window.getOutputPane().append(
                     "\nParse failed after " + steps + " advance steps.\n");
                  steps = 0; // prepare for an attempt at an alternative parse
                  parseCompleted = false;
                  return false;
               }
            }
         }
         else  // this shouldn't happen
         {  window.getOutputPane().append(
               "Invalid result of ASDParser advance(maxSteps) in "
               + steps + " steps");
            parseCompleted = false;
            return false;
         }
      }
      return false;
   } // end completeParse

   ASDParser getParser() { return parser; }

   boolean initializeParse(boolean strictFlag)
   {  if (!grammarLoaded)
      {  JOptionPane.showMessageDialog(window,
            "No grammar file is currently loaded.");
         window.clearGrammarFileNameField();
         return false;
      }
      if (originalUtterance == null || originalUtterance.length() == 0)
      {  JOptionPane.showMessageDialog(window,
            "The phrase to be parsed must not be empty.");
         return false;
      }
      utterance = morphologicallyAnalyze(originalUtterance);
      parser.initialize(utterance, expectedTypes);
      steps = 0;
      strict = strictFlag;
      if (strict)
         window.getOutputPane().append(
            "\n\"" + originalUtterance
            + "\" initialized for strict parsing.\n");
      else
         window.getOutputPane().append(
            "\n\"" + originalUtterance
            + "\" re-initialized for non-strict parsing.\n");
      parseCompleted = false;
      return true;
   } // end initializeParse

   String morphologicallyAnalyze(String phrase)
   {  String result = "";
      StringTokenizer tokenizer = new StringTokenizer(phrase,
         parser.SPACECHARS + parser.SPECIALCHARS, true);
      // Punctuation marks must be separated from words before
      // the EnglishWord.processApostrophe method is applied.
      while(tokenizer.hasMoreTokens())
      {  String token = tokenizer.nextToken().trim();
         if (token.length() == 1) // punctuation or one-letter word
            result = result + " " + token;
         else if (token.length() > 1)
         {   ArrayList apostropheResult
               = (ArrayList)EnglishWord.processApostrophe(token);
            if (apostropheResult == null)
               result = result + " " + token;
            else
               result = result + " " + (String)apostropheResult.get(0);
         }
         // whitespace tokens are ignored
      }
      return result;
   } // end morphologically Analyze

   void setExpectedTypeList(String types)
   {  if (!grammarLoaded)
      {  JOptionPane.showMessageDialog(window,
            "No grammar file is currently loaded.");
         window.clearGrammarFileNameField();
         window.clearExpectedTypeListField();
         return;
      }
      expectedTypes = new ArrayList();
      if (types == null || types.length() == 0)
      {  JOptionPane.showMessageDialog(window,
            "The list of expected phrase types must not be empty.");
         return;
      }
      StringTokenizer st = new StringTokenizer(types);
      while (st.hasMoreTokens())
         expectedTypes.add(st.nextToken());
      if (utterance != null && utterance.length() > 0)
         initializeParse(true);  // initialize for strict parsing
   } // end setExpectedTypeList

   void setSaveUniquelyParsedSubphrases(boolean save)
   {  parser.setSaveUniquelyParsedSubphrases(save);
   }

   void setUtterance(String newUtterance)
   {  utterance = originalUtterance = newUtterance;
      if (utterance == null || utterance.length() == 0)
      {  JOptionPane.showMessageDialog(window,
            "The phrase to be parsed must not be empty.");
         return;
      }
      initializeParse(true);  // initialize for strict parsing
   } // end setUtterance

   void setUtteranceNull() { utterance = originalUtterance = null; }

   void showAboutInfo()
   {  // responds to About Nounphrase1 choice in Help menu
      JOptionPane.showMessageDialog(window,
         "Nounphrase1 version " + VERSION +
         "\nAuthor: James A. Mason" +
         "\nEmail: jmason@yorku.ca" +
         "\nhttp://www.yorku.ca/jmason/");
   }

   private void showBracketedPhrase()
   {  String result = parser.bracketPhrase();
      window.getOutputPane().append(result + "\n");
   }

   void showPhraseStructure()
   {  window.getOutputPane().append(parser.bracketPhrase());
      ASDPhraseNode head = parser.phraseStructure();
      ASDPhraseNode currentNode = parser.currentNode();
      window.showTree(head, currentNode);
   } // end showPhraseStructure

   void showSemanticValue()
   {  if (parseCompleted)
         window.getOutputPane().append(
            parser.currentNode().nextNode().value().toString() + "\n");
      else
         JOptionPane.showMessageDialog(window,
            "The phrase must be completely parsed first.");
   }

   boolean useGrammar(String fileName)
   {  if (parser.useGrammar(fileName))
      {  grammarLoaded = true;
         return true;
      }
      else
      {  JOptionPane.showMessageDialog(window,
            "Grammar file " + fileName + " could not be loaded.");
         grammarLoaded = false;
         expectedTypes = null;
         originalUtterance = null;
         utterance = null;
         return false;
      }
   } // end useGrammar

// Methods for nodes in nounphrasesimple0.grm subgrammar:

   public String $$_1_action()
   {  parser.set("type", "unspecified");
      return null;
   }

   public String a_1_action()
   {  parser.set("number", "singular");
      parser.set("determiner", "indefinite");
      return null;
   }

   public String ADJECTIVE_1_action()
   {  ArrayList descriptors = (ArrayList)parser.get("descriptors");
      if (descriptors == null)
         descriptors = new ArrayList();
      else
         // Clone the list, in case of later backtracking at the
         // current level of the phrase structure:
         descriptors = (ArrayList)descriptors.clone();
      ASDPhraseNode node = parser.currentNode().subphrase();
      String word = node.word();
      if (word.equals("UNKNOWNWORD"))
      {  node = node.subphrase();
         word = node.word();
      }
      descriptors.add(word);
      parser.set("descriptors", descriptors);
      return null;
   }

   public String another_1_action()
   {  parser.set("number", "singular");
      parser.set("determiner", "indefinite");
      addQualifier("other");
      return null;
   }

   public String APOSTROPHE_1_action()
   {  parser.set("determiner", "possessive");
      return null;
   }

   public Object CARDINAL_1_value()
   {  return nodeValue();
   }

   public Object features()
   {  return parser.features();
   }

   public String raiseFeatures()
   {  parser.raiseFeatures();
      return null;
   }

   public String her_1_action()
   {  ArrayList her = new ArrayList(3);
      her.add("feminine"); her.add("singular");
      parser.set("determiner", "possessive");
      parser.set("owner", her);
      return null;
   }

   public String his_1_action()
   {  ArrayList his = new ArrayList(3);
      his.add("masculine"); his.add("singular");
      parser.set("determiner", "possessive");
      parser.set("owner", his);
      return null;
   }

   public String its_1_action()
   {  ArrayList its = new ArrayList(3);
      its.add("neuter"); its.add("singular");
      parser.set("determiner", "possessive");
      parser.set("owner", its);
      return null;
   }

   public String my_1_action()
   {  ArrayList my = new ArrayList(3);
      my.add("first person"); my.add("singular");
      parser.set("determiner", "possessive");
      parser.set("owner", my);
      return null;
   }

   public Object no_1_action()
   {  parser.set("quantity", new Long(0));
      return null;
   }

   public String NOUN_1_action()
   {  ASDPhraseNode node = parser.currentNode().subphrase();
      String word = node.word();
      if (word.equals("UNKNOWNWORD"))
      {  node = node.subphrase();
         word = node.word();
      }
      String number = (String)parser.valueOf("number");
      boolean pluralNoun = EnglishNoun.isPlural(word);
      boolean singularNoun = EnglishNoun.isSingular(word);
      if (strict &&
           ("singular".equals(number) && !singularNoun
            || "plural".equals(number) && !pluralNoun))
      {  window.getOutputPane().append(
            "Mismatch of determiner or quantity with noun.\n");
         return parser.NOADVANCE;
      }
      if (number == null || phraseHasQualifier("other"))
      {  if (singularNoun && !pluralNoun)
            parser.set("number", "singular");
         else if (pluralNoun && !singularNoun)
            parser.set("number", "plural");
      }
      parser.set("type", EnglishNoun.singularOf(word));
      return null;
   }

   public String NOUNPHRASE_1_action()
   {  parser.set("relatedObject", nodeValue());
      return null;
   }

   public String NOUNPHRASE1_1_action()
   {  parser.set("owner", nodeValue());
      return null;
   }

   public String other_1_value()
   {  return "other";
   }

   public String our_1_action()
   {  ArrayList our = new ArrayList(3);
      our.add("first person"); our.add("plural");
      parser.set("determiner", "possessive");
      parser.set("owner", our);
      return null;
   }

   public String PREPPHRASE_1_action()
   {  ArrayList relations = (ArrayList)parser.get("relations");
      if (relations == null)
         relations = new ArrayList();
      else
         // Clone the list, in case of later backtracking at the
         // current level of the phrase structure:
         relations = (ArrayList)relations.clone();
      relations.add(nodeValue());
      parser.set("relations", relations);
      return null;
   }

   public String PREPOSITION_1_action()
   {  ASDPhraseNode subphrase = parser.currentNode().subphrase();
      String relationship = subphrase.word();
      if ("UNKNOWNWORD".equals(relationship))
         relationship = subphrase.subphrase().word();
      parser.set("relationship", relationship);
      return null;
   }

   public String QUALIFIER_1_action()
   {  addQualifier((String)nodeValue());
      return null;
   }

   public String QUANTITY_1_action()
   {  Long value = (Long)nodeValue();
      String number = (String)parser.valueOf("number");
      if (strict
           // "a two", "these one" or "these other one" are anomalous,
           // but "another two" or "this other two" are not
           && ( ( (phraseHasDeterminer("indefinite")
                      || "singular".equals(number))
                   && value.longValue() != 1
                   && !phraseHasQualifier("other")
                )
                || "plural".equals(number) && value.longValue() == 1
              )
         )
      {  window.getOutputPane().append(
            "Mismatch of determiner with quantity.\n");
         return parser.NOADVANCE;
      }
      if (number == null || phraseHasQualifier("other"))
      {  if (value.longValue() == 1)
            parser.set("number", "singular");
         else if (value.longValue() > 1)
            parser.set("number", "plural");
      }
      parser.set("quantity", value);
      return null;
   }

   public String that_1_action()
   {  ArrayList that = new ArrayList(2);
      that.add("deictic"); that.add("far");
      parser.set("determiner", that);
      parser.set("number", "singular");
      return null;
   }

   public String the_1_action()
   {  parser.set("determiner", "definite");
      return null;
   }

   public String their_1_action()
   {  ArrayList their = new ArrayList(3);
      their.add("third person"); their.add("plural");
      parser.set("determiner", "possessive");
      parser.set("owner", their);
      return null;
   }

   public String these_1_action()
   {  ArrayList these = new ArrayList(2);
      these.add("deictic"); these.add("near");
      parser.set("determiner", these);
      parser.set("number", "plural");
      return null;
   }

   public String this_1_action()
   {  ArrayList value = new ArrayList(2);
      value.add("deictic"); value.add("near");
      parser.set("determiner", value);
      parser.set("number", "singular");
      return null;
   }

   public String those_1_action()
   {  ArrayList those = new ArrayList(2);
      those.add("deictic"); those.add("far");
      parser.set("determiner", those);
      parser.set("number", "plural");
      return null;
   }

   public String UNKNOWNWORD_1_action()
   {  if (!EnglishWord.isPreposition(
             parser.currentNode().subphrase().word()) )
         return parser.NOADVANCE;
      window.getOutputPane().append(
         "\nGuessing that the unknown word \""
         + parser.currentNode().subphrase().word()
         + "\" is a PREPOSITION.\n");
      return null;
   }

   public Object UNKNOWNWORD_2_value()
   {  window.getOutputPane().append(
         "\nGuessing that the unknown word \""
         + parser.currentNode().subphrase().word()
         + "\" is an ADJECTIVE.\n");
      return null;
   }

   public Object UNKNOWNWORD_3_value()
   {  window.getOutputPane().append(
         "\nGuessing that the unknown word \""
         + parser.currentNode().subphrase().word()
         + "\" is a NOUN.\n");
      return null;
   }

   public Object nodeValue()
   {  return parser.currentNode().value();
   }

   public String what_1_action()
   {  parser.set("determiner", "interrogative");
      return null;
   }

   public String which_1_action()
   {  parser.set("determiner", "interrogative");
      return null;
   }

   public String whose_1_action()
   {  parser.set("determiner", "possessive");
      parser.set("owner", "interrogative");
      return null;
   }

   public String your_1_action()
   {  ArrayList your = new ArrayList(2);
      your.add("second person");
      parser.set("determiner", "possessive");
      parser.set("owner", your);
      return null;
   }

// Private helping methods used by public methods associated
// with grammar nodes:

   /* Adds a qualifier string to a collection which is the value
      of a "qualifiers" feature:
    */
   private void addQualifier(String qual)
   {  ArrayList qualifiers = (ArrayList)parser.get("qualifiers");
      if (qualifiers == null)
      {  qualifiers = new ArrayList(2);
         parser.set("qualifiers", qualifiers);
      }
      qualifiers.add(qual);
   }

   /* Tests whether the value of a "determiner" feature either
      is itself equal to a given String, det, or is an ArrayList
      that includes the String det.
    */
   private boolean phraseHasDeterminer(String det)
   {  Object determiner = parser.get("determiner");
      if (determiner == null) return false;
      if (determiner instanceof String
          && det.equals((String)determiner)) return true;
      if (determiner instanceof ArrayList
          && ((ArrayList)determiner).contains(det)) return true;
      return false;
   }

   /* Tests whether the value of a "qualifiers" feature is an
      ArrayList that includes the given String qual.
    */
   private boolean phraseHasQualifier(String qual)
   {  ArrayList qualifiers = (ArrayList)parser.get("qualifiers");
      if (qualifiers == null) return false;
      if (qualifiers.contains(qual)) return true;
      return false;
   }

// Methods for nodes in cardinal.grm subgrammar:

   public long longNodeValue()
   {  return ((Long)nodeValue()).longValue();
   }

   public String unit_2_action()
   {  parser.set("V", new Long(longValueOfV() + longNodeValue()));
      return null;
   }
   public String cardinal_2_action()
   {  if (longNodeValue() >= valueOfM().longValue())
         return parser.NOADVANCE;
      else
         return setV2NodeValue();
   }
   public String setVNodeValue()
   {  parser.set("V", nodeValue()); return null; }
   public String setV2NodeValue()
   {  parser.set("V2", nodeValue()); return null; }
   public Long valueOfM() { return (Long)parser.valueOf("M"); }
   public long longValueOfM() { return valueOfM().longValue(); }
   public long longValueOfV() { return valueOfV().longValue(); }
   public long longValueOfV2() { return valueOfV2().longValue(); }
   public String multiplier_1_action()
   {  if (longValueOfV() >= longNodeValue())
         return parser.NOADVANCE;
      else
      {  parser.set("M", nodeValue()); return null; }
   }
   public Long valueOfV() { return (Long)parser.valueOf("V"); }
   public Long valueOfV2() { return (Long)parser.valueOf("V2"); }
   public Long valueOfVTimesM()
   {  return new Long(longValueOfV() * longValueOfM()); }
   public Long valueOfVTimesMPlusV2()
   {  return new Long(longValueOfV() * longValueOfM() + longValueOfV2()); }

   public String UNKNOWNCARDINAL_action()
   {  String word = parser.currentNode().subphrase().word();
      if (word.indexOf('-') < 0) // not a hyphenated word
         return parser.NOADVANCE;
      else // try to parse the hyphenated word separately as a numeral:
      {  ASDParser mainParser = parser;
         ASDGrammar mainGrammar = mainParser.lexicon();
         parser = new ASDParser(this);  // Use a separate parser temporarily
            // to try to parse the unknown hyphenated word as a CARDINAL.
         parser.setSPECIALCHARS(parser.SPECIALCHARS + "-");
            // Have parser2 break the word at the hyphen.
         parser.useGrammar(mainGrammar);  // use the same grammar
         ArrayList expectedTypes = new ArrayList(1);
         expectedTypes.add("CARDINAL");
         parser.initialize(word, expectedTypes);
         int numberOfSteps = parser.parse(MAXSTEPS);
         if (numberOfSteps < 0)
         {  parser = mainParser;
            return parser.NOADVANCE;
         }
         // The unknown word has been parsed successfully.
         // So add an entry for it to the lexicon.
         Object value = parser.currentNode().nextNode().value();
         ASDGrammarNode gNode = new ASDGrammarNode(
            word, "1",  //first instance of this word
            true, null, // begins phrase(s) of unspecified types
            null, null, // no successors or successor types
            "CARDINAL", value.toString(), // ends a CARDINAL phrase
            null);      // no semantic action
         ArrayList instances = new ArrayList(1);
         instances.add(gNode);
         mainGrammar.lexicon().put(word, instances);
         // Set the value of the V feature to the node's value:
         mainParser.set("V", value);
         parser = mainParser;  // Revert to using the main parser.
         return null;
      }
   } // end UNKNOWNCARDINAL_action

   private static boolean strict = true; // indicates whether or not parse
      // should follow grammar rules strictly
   
   static final String GRAMMARFILENAME =
           "http://www.asd-networks.com/grammars/nounphrasesimple.grm";
   
 

   static final ArrayList EXPECTEDTYPES = new ArrayList(1);
   static final int MAXSTEPS = 10000;
   static final String VERSION = "2.01";
   static final Font FONT
      = new Font("Monospaced", Font.PLAIN, 12);
   private NounPhraseWindow window;
   private ASDParser parser;
   private boolean grammarLoaded = false;
   private boolean parseCompleted = false;
   private ArrayList expectedTypes;
   private String grammarFileName;
   private int steps;
   private String utterance;
   private String originalUtterance;
   static
   {  EXPECTEDTYPES.add("NOUNPHRASE");
   }
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
    

/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
   
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
   private class NounPhraseWindow extends JFrame
   {  NounPhraseWindow(NounPhrase givenDriver)
      {  driver = givenDriver;
      
    /*try {
        //UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        // If you want the System L&F instead, comment out the above line and
        // uncomment the following:
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Exception exc) {
        System.err.println("Error loading L&F: " + exc);
    }*/
    // Trying to set Nimbus look and feel from the Java Notepad example
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        

        grammarFileNameField = new JTextField(40);
        grammarFileNameField.setText(NounPhrase.GRAMMARFILENAME);
        grammarFileNameField.addActionListener(
        new GrammarFileNameFieldListener(this));
        expectedTypeListField = new JTextField(40);
        String expected = "";
        for (Iterator it = NounPhrase.EXPECTEDTYPES.iterator();
            it.hasNext(); )
        {  String type = (String)it.next();
        expected += type + " ";
        }
        expectedTypeListField.setText(expected);
        expectedTypeListField.addActionListener(
        new ExpectedTypeListFieldListener(this));
        utteranceField = new JTextField(40);
        utteranceField.addActionListener(
        new UtteranceFieldListener(this));
        uniquelyParsedSubphrasesBox = new JCheckBox(
        "Save all uniquely-parsed subphrases");
        uniquelyParsedSubphrasesBox.addActionListener(
        new UniquelyParsedSubphrasesBoxListener(this));
        uniquelyParsedSubphrasesBox.setSelected(true);
        JPanel pane = new JPanel();
        pane.setLayout(
        new BoxLayout(pane, BoxLayout.Y_AXIS));
        pane.add(
        new LabeledTextField("Grammar file:    ", grammarFileNameField));
        pane.add(
        new LabeledTextField("Expected types:", expectedTypeListField));
        pane.add(
        new LabeledTextField("Phrase parsed: ", utteranceField));
        pane.add(uniquelyParsedSubphrasesBox);
        outputPane = new JTextArea();
        outputPane.setMinimumSize(new Dimension(DEFAULT_WIDTH,
        DEFAULT_HEIGHT));
        outputPane.setFont(NounPhrase.FONT);
        OutputPaneMenu menu = new OutputPaneMenu(outputPane, driver);
        MouseListener popupListener = new PopupListener(menu);
        outputPane.addMouseListener(popupListener);

        pane.add(new JScrollPane(outputPane));
        getContentPane().add(pane, BorderLayout.CENTER);
        addWindowListener(new WindowCloser(this));
        // listens for window closing events (see below)
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        ActionMenu aMenu = new ActionMenu(this);
        aMenu.setMnemonic(KeyEvent.VK_A);
        menuBar.add(aMenu);
        HelpMenu hMenu = new HelpMenu(this);
        hMenu.setMnemonic(KeyEvent.VK_H);
        menuBar.add(hMenu);


        JToolBar toolbar = new JToolBar();

        add(toolbar, BorderLayout.NORTH);

        ClassLoader cldr = this.getClass().getClassLoader();
        // 1. ICON Get Grammar with listener
        
    
    
    toolbar.addSeparator();
    toolbar.addSeparator();
    JButton newGrammarFileButton = new JButton(new ImageIcon(getClass().
     getClassLoader().getResource("com/asdnetworks/nounphrase/resources/open.gif")));
     
     //JButton  newGrammarFileButton= new JButton(new ImageIcon(
             //   "src/com/asdnetworks/nounphrase/images/open.gif")); 
    
    newGrammarFileButton.setToolTipText("Use grammar file name or enter URL in"
    + " the text field and do ENTER . \n"
    + "You will see the list of Expected Types appear in the next test field");
    toolbar.add(newGrammarFileButton);
    
    toolbar.addSeparator();
    toolbar.addSeparator();
    
    newGrammarFileButton.addActionListener(new ActionListener() {

        public void actionPerformed(ActionEvent event) {
            driver.initializeParse(true);          
        }
    });
             

    // 2. ICON 1 initialize parse  get EXPECTED TYPES TYPE NOUN PHRASE
    //JButton initializeParseButton = new JButton(new ImageIcon(
    //  "src/com/asdnetworks/nounphrase/images/new.gif"));   
    
    JButton initializeParseButton = new JButton(new ImageIcon(getClass().
      getClassLoader().getResource("com/asdnetworks/nounphrase/resources/new.gif")));
     
    initializeParseButton.setToolTipText
                ("Type a noun phrase like 'the big black dog', do ENTER to initialize new parse");
    toolbar.add(initializeParseButton);

    initializeParseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                driver.initializeParse(true);
            }
        });
            


        // 3. Complete First Parse
       // JButton completeParseButton = new JButton(new ImageIcon(
       //   "src/com/asdnetworks/nounphrase/images/parse.png"));    
    
    JButton completeParseButton = new JButton(new ImageIcon(getClass().
      getClassLoader().getResource("com/asdnetworks/nounphrase/resources/parse.png")));
     
    completeParseButton.setToolTipText("Complete parse");
    toolbar.add(completeParseButton);

    completeParseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                driver.completeParse();
            }
        });

          
            
           // 4.  Show Semantic Value
    
          //JButton stepParseButton = new JButton(new ImageIcon(
          // "src/com/asdnetworks/nounphrase/images/step.png"));   
    
    JButton  semanticValueButton = new JButton(new ImageIcon(getClass().
      getClassLoader().getResource("com/asdnetworks/nounphrase/resources/step.png")));
    
     semanticValueButton.setToolTipText("To show the semantic value of the noun phrase");
     toolbar.add(semanticValueButton);

     semanticValueButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                driver.showSemanticValue();
            }
        });


        // 5.     Show Bracketed Phrase      with listener
        // JButton showPhraseButton = new JButton(new ImageIcon(
        //  "src/com/asdnetworks/nounphrase/images/report_word.png"));    
        
        
      JButton  showPhraseButton = new JButton(new ImageIcon(getClass().
        getClassLoader().getResource("com/asdnetworks/nounphrase/resources/report_word.png")));
      
      showPhraseButton.setToolTipText("Show bracketed phrase structure");
      toolbar.add(showPhraseButton);

      showPhraseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                driver.showBracketedPhrase();
            }
        });

        // 6.  Show Tree Phrase Structure      with listener
       // JButton showTreeButton = new JButton(new ImageIcon(
          //      "src/com/asdnetworks/nounphrase/images/sideparse.png"));    // uses  icon

    JButton  showTreeButton= new JButton(new ImageIcon(getClass().
    getClassLoader().getResource("com/asdnetworks/nounphrase/resources/sideparse.png")));

    showTreeButton.setToolTipText("Show parse tree structure ");
    toolbar.add(showTreeButton);
    toolbar.addSeparator();
    toolbar.addSeparator();
        showTreeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                showPhraseStructure();
            }
        });
        // 7.  Show Tree Phrase Structure      with listener
       // JButton selectButton = new JButton(new ImageIcon(
        //"src/com/asdnetworks/nounphrase/images/copy.gif"));    // uses  icon
          
        
    JButton selectButton = new JButton(new ImageIcon(getClass().
     getClassLoader().getResource("com/asdnetworks/nounphrase/resources/copy.gif")));
    
        
     selectButton.setToolTipText("Click on Output Pane first to Select all output text ");
     toolbar.add(selectButton);

     selectButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                outputPane.selectAll();
            }
        });
        // 8.  Show Tree Phrase Structure      with listener
       // JButton cutEraseButton = new JButton(new ImageIcon(
         //       "src/com/asdnetworks/nounphrase/images/cut.gif"));    // uses  icon
        
          
    JButton  cutEraseButton = new JButton(new ImageIcon(getClass().
     getClassLoader().getResource("com/asdnetworks/nounphrase/resources/cut.gif")));
    
        //JButton  cutEraseButton= new JButton(new ImageIcon(getClass().
        //getClassLoader().getResource("com/asdnetworks/nounphrase/resources/cut.png")));
        
        cutEraseButton.setToolTipText("Clear output text area");
        toolbar.add(cutEraseButton);
        toolbar.addSeparator();
        toolbar.addSeparator();
        cutEraseButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                outputPane.setText("");
            }
        });


        // 9. Help   showAboutInfo   with listener
        // JButton helpButton = new JButton(new ImageIcon(
        //"src/com/asdnetworks/nounphrase/images/help.png"));    
        
        
        
    
    JButton helpButton = new JButton(new ImageIcon(getClass().
     getClassLoader().getResource("com/asdnetworks/nounphrase/resources/help.png")));
   
        helpButton.setToolTipText("Show App info");
        toolbar.add(helpButton);
        toolbar.addSeparator();
        toolbar.addSeparator();
        helpButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                driver.showAboutInfo();
            }
        });
        // 9.  Show Tree Phrase Structure      with listener
        //JButton exitButton = new JButton(new ImageIcon(
        //  "src/com/asdnetworks/nounphrase/images/delete.gif"));   
          
    JButton exitButton = new JButton(new ImageIcon(getClass().
     getClassLoader().getResource("com/asdnetworks/nounphrase/resources/delete.gif")));
    
        
     exitButton.setToolTipText("Close application ");
     toolbar.add(exitButton);
     //toolbar.addSeparator();
     exitButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent event) {
                System.exit(0);// Works but can NOT see icon
            }
        });
     
      } // end Nounphrase1Window(Nounphrase1 givenDriver)

      void clearExpectedTypeListField() { expectedTypeListField.setText(""); }
      void clearGrammarFileNameField() { grammarFileNameField.setText(""); }
      void clearUtteranceField() { utteranceField.setText(""); }

      JTextField getExpectedTypeListField() { return expectedTypeListField; }
      JTextField getGrammarFileNameField() { return grammarFileNameField; }
      JTextArea getOutputPane() { return outputPane; }
      NounPhrase getDriver() { return driver; }
      JTextField getUtteranceField() { return utteranceField; }

      void grammarFileNameFieldChanged()
      {  clearUtteranceField();
         driver.setUtteranceNull();
         if (!driver.useGrammar(grammarFileNameField.getText().trim()))
             // grammar file was not loaded
         {  clearExpectedTypeListField();
            // Note: the grammarFileNameField is intentionally NOT
            // reset to empty here, so the user can edit the incorrect
            // file name if desired.
            return;
         }
         Set expectedTypes = driver.getParser().lexicon().phraseTypes();
         String expected = "";
         for (Iterator it = expectedTypes.iterator(); it.hasNext(); )
         {  String type = (String)it.next();
            expected += type + " ";
         }
         expectedTypeListField.setText(expected);
         expectedTypeListFieldChanged();
      } // end grammarFileNameChanged

      void expectedTypeListFieldChanged()
      {  driver.setExpectedTypeList(expectedTypeListField.getText().trim());
      }

      void uniquelyParsedSubphrasesBoxChanged()
      {  driver.setSaveUniquelyParsedSubphrases(
            uniquelyParsedSubphrasesBox.isSelected());
      }

      void utteranceFieldChanged()
      {  driver.setUtterance(utteranceField.getText().trim());
      }

      /**
         Displays the tree rooted at the given head node,
         with node currentNode indicated by an asterisk and an arrow.
         @param head the header node of the phrase structure
         @param currentNode the current node at the top level
         in the phrase structure
       */
      void showTree(ASDPhraseNode head, ASDPhraseNode currentNode)
      {  showTreeMark(head, "", currentNode);
         outputPane.append("\n");
      } // end showTree

      /**
         Displays the portion of the tree starting at the
         given node and indented with the given indentString as
         prefix for each line that does not represent a top-
         level node.  Top-level nodes are prefixed with three
         blanks or, in the case of the given aNode, an asterisk
         and an arrow whose purpose is to indicate the node
         which is the current node during a parse.
         @param indentString prefix for indenting of the
         current subtree
         @param aNode the node to be marked with an arrow
       */
      private void showTreeMark(ASDPhraseNode givenNode, String indentString,
                               ASDPhraseNode markNode)
      {  outputPane.append("\n");
         if (givenNode == markNode)
            outputPane.append("*->");
         else
            outputPane.append("   ");
         outputPane.append(indentString + givenNode.word() + " ");
         if (givenNode.instance() != null)
            outputPane.append(givenNode.instance().instance());
         else
            outputPane.append("nil");
         if (givenNode.subphrase() != null)
            showTreeMark(givenNode.subphrase(),indentString + "   ",
               markNode);
         if (givenNode.nextNode() != null)
            showTreeMark(givenNode.nextNode(), indentString, markNode);
      } // end showTreeMark

      

      
      
      static final int DEFAULT_WIDTH = 800;  // window width
      static final int DEFAULT_HEIGHT = 600; // window height
      private NounPhrase driver;
      private JTextField grammarFileNameField;
      private JTextField expectedTypeListField;
      private JTextField utteranceField;
      private JCheckBox uniquelyParsedSubphrasesBox;
      private JTextArea outputPane;
      
      
      
   } // end class Nounphrase1Window

       
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
// end class Nounphrase1Window
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
   
/*!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!*/
    
    
   private class LabeledTextField extends JPanel
   {  LabeledTextField(String labelText, JTextField textField)
      {  setMaximumSize(new Dimension(800,10));
         setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
         JLabel label = new JLabel(labelText);
         textField.setFont(NounPhrase.FONT);
         this.add(label);
         this.add(textField);
      }
   } // end class LabeledTextField

   /**
      An instance defines what should happen when a window
      closes.
    */
   class WindowCloser extends WindowAdapter
   {  WindowCloser(NounPhraseWindow w)
      {  window = w;
      }

      public void windowClosing(WindowEvent e)
      {
         System.exit(0);        // stop the program
      }

      NounPhraseWindow window;
   } // end class WindowCloser

   
   private class GrammarFileNameFieldListener implements ActionListener
   {
      GrammarFileNameFieldListener(NounPhraseWindow w)
      {  window = w;
      }

      public void actionPerformed(ActionEvent e)
      {  window.grammarFileNameFieldChanged();
      }

      private NounPhraseWindow window;
   } // end class GrammarFileNameFieldListener

   private class ExpectedTypeListFieldListener implements ActionListener
   {
      ExpectedTypeListFieldListener(NounPhraseWindow w)
      {  window = w;
      }

      public void actionPerformed(ActionEvent e)
      {  window.expectedTypeListFieldChanged();
      }

      private NounPhraseWindow window;
   } // end class ExpectedTypeListFieldListener

   private class UtteranceFieldListener implements ActionListener
   {
      UtteranceFieldListener(NounPhraseWindow w)
      {  window = w;
      }

      public void actionPerformed(ActionEvent e)
      {  window.utteranceFieldChanged();
      }

      private NounPhraseWindow window;
   } // end class UtteranceFieldListener

   private class UniquelyParsedSubphrasesBoxListener implements
      ActionListener
   {  UniquelyParsedSubphrasesBoxListener(
         NounPhraseWindow w)
      {  window = w;
      }

      public void actionPerformed(ActionEvent e)
      {  window.uniquelyParsedSubphrasesBoxChanged();
      }

      private NounPhraseWindow window;
   } // end class UniquelyParsedSubphrasesBoxListener

   private class OutputPaneMenu extends JPopupMenu implements ActionListener
   {  OutputPaneMenu(JTextArea p, NounPhrase t)
      {  pane = p;
         driver = t;
         setInvoker(pane);

         JMenuItem initializeItem = new JMenuItem("Initialize parse");
         initializeItem.addActionListener(this);
         add(initializeItem);
         addSeparator();
         JMenuItem completeParseItem = new JMenuItem("Complete parse");
         completeParseItem.addActionListener(this);
         add(completeParseItem);
         addSeparator();
         JMenuItem showPhraseStructureItem
            = new JMenuItem("Show phrase structure");
         showPhraseStructureItem.addActionListener(this);
         add(showPhraseStructureItem);
         JMenuItem showSemanticValueItem
            = new JMenuItem("Show semantic value");
         showSemanticValueItem.addActionListener(this);
         add(showSemanticValueItem);
         addSeparator();
         JMenuItem selectAllItem = new JMenuItem("Select all");
         selectAllItem.addActionListener(this);
         add(selectAllItem);
         JMenuItem copyItem = new JMenuItem("Copy selection");
         copyItem.addActionListener(this);
         add(copyItem);
         addSeparator();
         JMenuItem clearItem = new JMenuItem("Erase output pane");
         clearItem.addActionListener(this);
         add(clearItem);
      } // end OutputPaneMenu(JTextArea p, Nounphrase1 t)

      public void actionPerformed(ActionEvent e)
      {  if (driver == null) return;
         String command = e.getActionCommand();
         if (command.equals("Initialize parse"))
            driver.initializeParse(true);  // initialize for strict parsing
         else if (command.equals("Complete parse"))
            driver.completeParse();
         else if (command.equals("Show phrase structure"))
            driver.showPhraseStructure();
         else if (command.equals("Show semantic value"))
            driver.showSemanticValue();
         else if (command.equals("Select all"))
         {  pane.requestFocus();
            pane.selectAll();
         }
         else if (command.equals("Copy selection"))
            pane.copy();
         else if (command.equals("Erase output pane"))
            pane.setText("");
      } // end actionPerformed

      NounPhrase driver;
      JTextArea pane; // the pane to which the menu is attached.
   } // end class OutputPaneMenu

   class ActionMenu extends JMenu implements ActionListener
   {  ActionMenu(NounPhraseWindow w)
      {  super("Action");
         window = w;
         driver = window.getDriver();
         outputPane = window.getOutputPane();
         JMenuItem initializeMenuItem = new JMenuItem("Initialize parse",
            KeyEvent.VK_I);
         initializeMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_I, ActionEvent.ALT_MASK));
         add(initializeMenuItem);
         initializeMenuItem.addActionListener(this);
         JMenuItem completeParseMenuItem = new JMenuItem("Complete Parse",
            KeyEvent.VK_P);
         completeParseMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_P, ActionEvent.ALT_MASK));
         add(completeParseMenuItem);
         completeParseMenuItem.addActionListener(this);
         JMenuItem showPhraseStructureMenuItem
            = new JMenuItem("Show phrase structure",
            KeyEvent.VK_S);
         showPhraseStructureMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_S, ActionEvent.ALT_MASK));
         showPhraseStructureMenuItem.addActionListener(this);
         add(showPhraseStructureMenuItem);
         JMenuItem showSemanticValueMenuItem
            = new JMenuItem("Show semantic value",
            KeyEvent.VK_V);
         showSemanticValueMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_V, ActionEvent.ALT_MASK));
         showSemanticValueMenuItem.addActionListener(this);
         add(showSemanticValueMenuItem);
         addSeparator();
         JMenuItem copyAllMenuItem = new JMenuItem("Select All of output pane",
            KeyEvent.VK_A);
         copyAllMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_A, ActionEvent.CTRL_MASK));
         copyAllMenuItem.addActionListener(this);
         add(copyAllMenuItem);
         JMenuItem copySelectionMenuItem = new JMenuItem("Copy Selection",
            KeyEvent.VK_C);
         copySelectionMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_C, ActionEvent.CTRL_MASK));
         copySelectionMenuItem.addActionListener(this);
         add(copySelectionMenuItem);
         addSeparator();
         JMenuItem eraseMenuItem = new JMenuItem("Erase output pane",
            KeyEvent.VK_E);
         eraseMenuItem.setAccelerator(KeyStroke.getKeyStroke(
            KeyEvent.VK_E, ActionEvent.CTRL_MASK));
         eraseMenuItem.addActionListener(this);
         add(eraseMenuItem);
      }

      /**
        Listens for menu item events.
       */
      public void actionPerformed(ActionEvent e)
      {  String command = e.getActionCommand();
         if (command.equals("Initialize parse"))
            driver.initializeParse(true);   // initialize for strict parsing
         else if (command.equals("Complete Parse"))
            driver.completeParse();
         else if (command.equals("Show phrase structure"))
            driver.showPhraseStructure();
         else if (command.equals("Show semantic value"))
            driver.showSemanticValue();
         else if (command.equals("Select All of output pane"))
         {  outputPane.requestFocus();
            outputPane.selectAll();
         }
         else if (command.equals("Copy Selection"))
            outputPane.copy();
         else if (command.equals("Erase output pane"))
            outputPane.setText("");
      }

      NounPhrase driver;
      NounPhraseWindow window;
      JTextArea outputPane;
   } // end class ActionMenu

   class HelpMenu extends JMenu implements ActionListener
   {  HelpMenu(NounPhraseWindow w)
      {  super("Help");
         window = w;
         driver = window.getDriver();
         JMenuItem aboutMenuItem = new JMenuItem("About Nounphrase1",
            KeyEvent.VK_A);
         add(aboutMenuItem);
         aboutMenuItem.addActionListener(this);
      }

      /**
         Listens for menu item events.
       */
      public void actionPerformed(ActionEvent e)
      {  String command = e.getActionCommand();
         if (command.equals("About Nounphrase1"))
            driver.showAboutInfo();
      }

      NounPhrase driver;
      NounPhraseWindow window;
   } // end class HelpMenu
   
 
   
  

   
    private static ResourceBundle resources;
    
  
 
     static {
         try {
             resources = ResourceBundle.getBundle(
                     "com.asdnetworks.nounphrase.resources.nounphrase",
                     Locale.getDefault());
           } catch (MissingResourceException mre) {
             System.err.println(
                     "com/asdnetworks/nounphrase/resources/nounphrase.properties not found");
             System.exit(1);
         }
     }
    

     
} // end class Nounphrase1
/**
   This class can be used by any others in the english package,
   to pop up a given menu in response to a right mouse click.
 */
class PopupListener extends MouseAdapter
{  PopupListener(JPopupMenu m)
   {  menu = m;
   }

   public void mousePressed(MouseEvent e)
   {  maybeShowPopup(e);
   }

   public void mouseReleased(MouseEvent e)
   {  maybeShowPopup(e);
   }

   private void maybeShowPopup(MouseEvent e)
   {  if (e.isPopupTrigger())
        menu.show(e.getComponent(), e.getX(), e.getY());
   }

   private JPopupMenu menu;
} // end class PopupListener

 class ResourceTest {
    public static void main(String[] args) {
        String fileName = "nounphrasesimple.grm";
        System.out.println(fileName);
        System.out.println(new ResourceTest().getClass().getResource(fileName));
        System.out.println(new ResourceTest().getClass().getClassLoader().getResource(fileName));
    }
 }


class FileLoader extends Thread {
    private Object elementTreePanel;

        FileLoader(File f, Document doc) {
            setPriority(4);
            this.f = f;
            this.doc = doc;
        }

        @Override
        public void run() {
            try {
                // initialize the statusbar
                //status.removeAll();
                JProgressBar progress = new JProgressBar();
                progress.setMinimum(0);
                progress.setMaximum((int) f.length());
                //status.add(progress);
               // status.revalidate();

                // try to start reading
                Reader in = new FileReader(f);
                char[] buff = new char[4096];
                int nch;
                while ((nch = in.read(buff, 0, buff.length)) != -1) {
                    doc.insertString(doc.getLength(), new String(buff, 0, nch),
                            null);
                    progress.setValue(progress.getValue() + nch);
                }
            } catch (IOException e) {
                final String msg = e.getMessage();
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        //javax.swing.JOptionPane.showMessageDialog(getFrame())
                          //      "Could not open file: " + msg,
                            //    "Error opening file",
                              //  JOptionPane.ERROR_MESSAGE);
                    }
                });
            } catch (BadLocationException e) {
                System.err.println(e.getMessage());
            }
            //doc.addUndoableEditListener(undoHandler);
            // we are done... get rid of progressbar
           // status.removeAll();
           // status.revalidate();

            //resetUndoManager();

            if (elementTreePanel != null) {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        //elementTreePanel.setEditor(getEditor());
                    }
                });
            }
        }
        Document doc;
        File f;
    }

