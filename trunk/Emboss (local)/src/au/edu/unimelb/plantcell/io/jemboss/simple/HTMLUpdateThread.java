package au.edu.unimelb.plantcell.io.jemboss.simple;

import java.awt.Font;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JEditorPane;
import javax.swing.UIManager;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.StyleSheet;

public class HTMLUpdateThread extends Thread {
	private JEditorPane m_html_help;
	private ACDApplication m_acdtable, m_prog;
	
	public HTMLUpdateThread(ACDApplication acdtable, ACDApplication prog, JEditorPane html_help_widget) {
		assert(acdtable != null && prog != null && html_help_widget != null);
		m_html_help = html_help_widget;
		m_acdtable  = acdtable;
		m_prog      = prog;
	}
	
	@Override
	public void run() {
		try {
			String html = EmbossPredictorNodeModel.run_emboss_command(m_acdtable, 
								new String[] { m_prog.getName() }, true);
			update_html(m_prog.getName(), html);
		} catch (Exception e) {
			update_html(m_prog.getName(), "No help available!");
		}
	}
	
	 
	public void update_html(String prog, String html_fragment) {
		 /**
	      * Called from the tree selection listener, this method updates the HTML widget based on the 
	      * user-chosen emboss program.
	      * @param html_fragment2 
	      */
    	 m_html_help.setContentType("text/html");
    	 Font font = UIManager.getFont("Label.font");
         String bodyRule = "body { font-family: " + font.getFamily() + "; " +  "font-size: 9pt; }";
         String trRule = "tr.even { background-color: #FFFFFF; } \n" +
         				 "tr.odd  { background-color: #E0E0E0; }";
         StyleSheet ss =  ((HTMLDocument) m_html_help.getDocument()).getStyleSheet();
         ss.addRule(bodyRule);
         ss.addRule(trRule);
         // remove ugly markup from ACDtable result
         html_fragment = html_fragment.replaceFirst("<table[^>]+?>", "<table bgcolor=\"#C0C0C0\">");
         Pattern p = Pattern.compile("<tr bgcolor=\"#[A-F0-9]+\">");
         Matcher m = p.matcher(html_fragment);
         StringBuffer html_sb = new StringBuffer(html_fragment.length());
         int row_id = 1;
         while (m.find()) {
        	 if (row_id++ % 2 == 0) {
        		 m.appendReplacement(html_sb, "<tr class=\"even\">");
        	 } else {
        		 m.appendReplacement(html_sb, "<tr class=\"odd\">");
        	 }
         }
         m.appendTail(html_sb);
         
         //Logger.getAnonymousLogger().info(html_sb.toString());
        
		 m_html_help.setText("<html><body><b>"+m_prog.getOneLineSummary()+"</b><p>"+html_sb+"</p></body></html>");
	}	
}
