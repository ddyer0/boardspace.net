
package bugs.data;

/**
 * export from the species database to tsv files in the format expected by the bugspiel app.
 * Note that this was almost entirely created by chatgpt, only minor debugging was required.
 * 
 */
import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Export {
	
	public static String pickFileName(String startingName) {
	       JFileChooser fileChooser = new JFileChooser();
	        
	        // Set the suggested file name in the chooser
	        fileChooser.setSelectedFile(new File(startingName));
	        
	        // Optional: add filters, e.g. only .txt files
	        FileNameExtensionFilter filter = new FileNameExtensionFilter("SQLlite Files", "sqlite");
	         fileChooser.setFileFilter(filter);

	        int userSelection = fileChooser.showSaveDialog(null); // null = center on screen

	        if (userSelection == JFileChooser.APPROVE_OPTION) {
	            File fileToSave = fileChooser.getSelectedFile();
	            return fileToSave.getAbsolutePath();
	        } else {
	            return null;  // User cancelled
	        }
	}
	
    public static void main(String[] args) {
        if (args.length < 1 ) {
            System.err.println("Usage: java ExportTSV <table-name> ... ");
            System.exit(1);
        }

        String dbFile = pickFileName("g:/share/projects/boardspace-java/boardspace-games/bugs/data/");
        if(dbFile!=null)
        {
        for(String tableName : args)
        {
         String outputFile = "g:/share/projects/boardspace-java/boardspace-games/bugs/data/" + tableName + "_export.tsv";

        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName);
             BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {

            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();

            // Write header
            for (int i = 1; i <= columnCount; i++) {
                writer.write(meta.getColumnName(i));
                if (i < columnCount) writer.write("\t");
            }
            writer.newLine();
            writer.newLine();

            // Write rows
            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    String value = rs.getString(i);
                    if (value != null) writer.write(value);
                    if (i < columnCount) writer.write("\t");
                }
                writer.newLine();
                writer.newLine(); // extra newline between entries
            }

            System.out.println("Exported to: " + outputFile);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }
        }}
    }
}