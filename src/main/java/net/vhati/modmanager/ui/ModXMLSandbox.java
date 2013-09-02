package net.vhati.modmanager.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Caret;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import net.vhati.ftldat.FTLDat;
import net.vhati.modmanager.core.ModUtilities;
import net.vhati.modmanager.core.SloppyXMLOutputProcessor;
import net.vhati.modmanager.core.SloppyXMLParser;
import net.vhati.modmanager.core.XMLPatcher;
import net.vhati.modmanager.ui.ClipboardMenuMouseListener;

import org.jdom2.Document;
import org.jdom2.input.JDOMParseException;


public class ModXMLSandbox extends JDialog implements ActionListener {

	private Document mainDoc = null;

	private File dataDatFile;

	private JTabbedPane areasPane;
	private JScrollPane mainScroll;
	private JScrollPane appendScroll;
	private JScrollPane resultScroll;
	private JScrollPane messageScroll;

	private JTextArea mainArea;
	private JTextArea appendArea;
	private JTextArea resultArea;
	private JTextArea messageArea;
	private JTextField findField;
	private JButton openBtn;
	private JButton patchBtn;
	private JLabel statusLbl;


	public ModXMLSandbox( Frame owner, File dataDatFile ) {
		super( owner, "Mod XML Sandbox", true );

		this.dataDatFile = dataDatFile;

		mainArea = new JTextArea();
		mainArea.setTabSize( 4 );
		mainArea.setEditable( false );
		mainArea.addMouseListener( new ClipboardMenuMouseListener() );
		mainScroll = new JScrollPane( mainArea );

		appendArea = new JTextArea();
		appendArea.setTabSize( 4 );
		appendArea.addMouseListener( new ClipboardMenuMouseListener() );
		appendScroll = new JScrollPane( appendArea );

		resultArea = new JTextArea();
		resultArea.setTabSize( 4 );
		resultArea.setEditable( false );
		resultArea.addMouseListener( new ClipboardMenuMouseListener() );
		resultScroll = new JScrollPane( resultArea );

		messageArea = new JTextArea();
		messageArea.setTabSize( 4 );
		messageArea.setEditable( false );
		messageArea.addMouseListener( new ClipboardMenuMouseListener() );
		messageArea.setText( "This is a scratchpad to tinker with advanced mod syntax.\n1) Open XML from data.dat to fill the 'main' tab. (ctrl-o)\n2) Write some <mod:command> tags in the 'append' tab.\n3) Click Patch to see the result. (ctrl-p)" );
		messageScroll = new JScrollPane( messageArea );

		JPanel ctrlPanel = new JPanel();
		ctrlPanel.setLayout( new BoxLayout( ctrlPanel, BoxLayout.X_AXIS ) );

		openBtn = new JButton( "Open Main..." );
		openBtn.addActionListener( this );
		ctrlPanel.add( openBtn );

		ctrlPanel.add( Box.createHorizontalGlue() );

		findField = new JTextField( "<find: ctrl-f, F3/shift-F3>", 20 );
		findField.setMaximumSize( new Dimension( 60, findField.getPreferredSize().height ) );
		ctrlPanel.add( findField );

		ctrlPanel.add( Box.createHorizontalGlue() );

		patchBtn = new JButton( "Patch" );
		patchBtn.addActionListener( this );
		ctrlPanel.add( patchBtn );

		areasPane = new JTabbedPane( JTabbedPane.BOTTOM );
		areasPane.add( "Main", mainScroll );
		areasPane.add( "Append", appendScroll );
		areasPane.add( "Result", resultScroll );

		JPanel topPanel = new JPanel( new BorderLayout() );
		topPanel.add( areasPane, BorderLayout.CENTER );
		topPanel.add( ctrlPanel, BorderLayout.SOUTH );

		final JSplitPane splitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT );
		splitPane.setTopComponent( topPanel );
		splitPane.setBottomComponent( messageArea );

		JPanel statusPanel = new JPanel();
		statusPanel.setLayout( new BoxLayout(statusPanel, BoxLayout.Y_AXIS) );
		statusPanel.setBorder( BorderFactory.createLoweredBevelBorder() );
		statusLbl = new JLabel(" ");
		statusLbl.setBorder( BorderFactory.createEmptyBorder(2, 4, 2, 4) );
		statusLbl.setAlignmentX( Component.LEFT_ALIGNMENT );
		statusPanel.add( statusLbl );

		JPanel contentPane = new JPanel( new BorderLayout() );
		contentPane.add( splitPane, BorderLayout.CENTER );
		contentPane.add( statusPanel, BorderLayout.SOUTH );
		this.setContentPane( contentPane );

		findField.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained( FocusEvent e ) {
				findField.selectAll();
			}
		});
		CaretListener caretListener = new CaretListener() {
			@Override
			public void caretUpdate( CaretEvent e ) {
				JTextArea currentArea = getCurrentArea();
				if ( currentArea == null ) return;
				if ( e.getSource() != currentArea ) return;
				updateCaretStatus();
			}
		};
		mainArea.addCaretListener( caretListener );
		appendArea.addCaretListener( caretListener );
		resultArea.addCaretListener( caretListener );

		CaretAncestorListener caretAncestorListener = new CaretAncestorListener();
		mainArea.addAncestorListener( caretAncestorListener );
		appendArea.addAncestorListener( caretAncestorListener );
		resultArea.addAncestorListener( caretAncestorListener );

		AbstractAction openAction = new AbstractAction( "Open" ) {
			@Override
			public void actionPerformed( ActionEvent e ) {
				open();
			}
		};
		AbstractAction patchAction = new AbstractAction( "Patch" ) {
			@Override
			public void actionPerformed( ActionEvent e ) {
				patch();
			}
		};
		AbstractAction focusFindAction = new AbstractAction( "Focus Find" ) {
			@Override
			public void actionPerformed( ActionEvent e ) {
				findField.requestFocusInWindow();
			}
		};
		AbstractAction findNextAction = new AbstractAction( "Find Next" ) {
			@Override
			public void actionPerformed( ActionEvent e ) {
				findNext();
			}
		};
		AbstractAction findPreviousAction = new AbstractAction( "Find Previous" ) {
			@Override
			public void actionPerformed( ActionEvent e ) {
				findPrevious();
			}
		};

		KeyStroke openShortcut = KeyStroke.getKeyStroke( "control O" );
		contentPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( openShortcut, "open" );
		contentPane.getActionMap().put( "open", openAction );
		KeyStroke patchShortcut = KeyStroke.getKeyStroke( "control P" );
		contentPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( patchShortcut, "patch" );
		contentPane.getActionMap().put( "patch", patchAction );
		KeyStroke focusFindShortcut = KeyStroke.getKeyStroke( "control F" );
		contentPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( focusFindShortcut, "focus find" );
		contentPane.getActionMap().put( "focus find", focusFindAction );
		KeyStroke findNextShortcut = KeyStroke.getKeyStroke( "F3" );
		contentPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( findNextShortcut, "find next" );
		contentPane.getActionMap().put( "find next", findNextAction );
		KeyStroke findPreviousShortcut = KeyStroke.getKeyStroke( "shift F3" );
		contentPane.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ).put( findPreviousShortcut, "find previous" );
		contentPane.getActionMap().put( "find previous", findPreviousAction );

		findField.getInputMap().put( KeyStroke.getKeyStroke( "released ENTER" ), "find next" );
		findField.getActionMap().put( "find next", findNextAction );

		this.pack();
		this.setSize( 800, 600 );
		this.setLocationRelativeTo( null );

		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				splitPane.setDividerLocation( 0.80d );
			}
		});
	}


	@Override
	public void actionPerformed( ActionEvent e ) {
		Object source = e.getSource();

		if ( source == openBtn ) {
			open();
		}
		else if ( source == patchBtn ) {
			patch();
		}
	}


	private void open() {
		FTLDat.FTLPack dataP = null;
		InputStream is = null;
		try {
			dataP = new FTLDat.FTLPack( dataDatFile, false );
			List<String> innerPaths = dataP.list();

			String innerPath = promptForInnerPath( innerPaths );
			if ( innerPath == null ) return;

			is = dataP.getInputStream( innerPath );
			String mainText = ModUtilities.decodeText( is, dataDatFile.getName()+":"+innerPath ).text;
			is.close();

			mainText = mainText.replaceFirst( "<[?]xml [^>]*?[?]>", "" );
			mainText = "<wrapper xmlns:mod='mod' xmlns:mod-append='mod-append' xmlns:mod-overwrite='mod-overwrite'>"+ mainText +"</wrapper>";

			SloppyXMLParser parser = new SloppyXMLParser();
			mainDoc = parser.build( mainText );

			StringWriter writer = new StringWriter();
			SloppyXMLOutputProcessor.sloppyPrint( mainDoc, writer, null );
			mainArea.setText( writer.toString() );
			mainArea.setCaretPosition( 0 );
			areasPane.setSelectedComponent( mainScroll );
			resultArea.setText( "" );
		}
		catch ( IOException f ) {
			messageArea.setText( f.getMessage() );
			messageArea.setCaretPosition( 0 );
		}
		catch ( JDOMParseException f ) {
			messageArea.setText( f.getMessage() );
			messageArea.setCaretPosition( 0 );
		}
		finally {
			try {if ( is != null ) is.close();}
			catch ( IOException f ) {}

			try {if ( dataP != null ) dataP.close();}
			catch ( IOException f ) {}
		}
	}


	private void patch() {
		if ( mainDoc == null ) return;

		try {
			String appendText = appendArea.getText();
			appendText = appendText.replaceFirst( "<[?]xml [^>]*?[?]>", "" );
			appendText = "<wrapper xmlns:mod='mod' xmlns:mod-append='mod-append' xmlns:mod-overwrite='mod-overwrite'>"+ appendText +"</wrapper>";
			SloppyXMLParser parser = new SloppyXMLParser();
			Document appendDoc = parser.build( appendText );

			XMLPatcher patcher = new XMLPatcher();
			Document resultDoc = patcher.patch( mainDoc, appendDoc );

			StringWriter writer = new StringWriter();
			SloppyXMLOutputProcessor.sloppyPrint( resultDoc, writer, null );
			resultArea.setText( writer.toString() );
			resultArea.setCaretPosition( 0 );
			areasPane.setSelectedComponent( resultScroll );
		}
		catch ( Exception f ) {
			messageArea.setText( f.getMessage() );
			messageArea.setCaretPosition( 0 );
		}
	}


	private void findNext() {
		JTextArea currentArea = getCurrentArea();
		if ( currentArea == null ) return;

		String query = findField.getText();
		if ( query.length() == 0 ) return;

		Caret caret = currentArea.getCaret();
		int from = Math.max( caret.getDot(), caret.getMark() );

		Pattern ptn = Pattern.compile( "(?i)"+ Pattern.quote(query) );
		Matcher m = ptn.matcher( currentArea.getText() );
		if ( m.find(from) ) {
			caret.setDot( m.start() );
			caret.moveDot( m.end() );
			caret.setSelectionVisible( true );
		}
	}

	private void findPrevious() {
		JTextArea currentArea = getCurrentArea();
		if ( currentArea == null ) return;

		String query = findField.getText();
		if ( query.length() == 0 ) return;

		Caret caret = currentArea.getCaret();
		int from = Math.min( caret.getDot(), caret.getMark() );

		Pattern ptn = Pattern.compile( "(?i)"+ Pattern.quote(query) );
		Matcher m = ptn.matcher( currentArea.getText() );
		m.region( 0, from );
		int lastStart = -1;
		int lastEnd = -1;
		while ( m.find() ) {
			lastStart = m.start();
			lastEnd = m.end();
		}
		if ( lastStart != -1 ) {
			caret.setDot( lastStart );
			caret.moveDot( lastEnd );
			caret.setSelectionVisible( true );
		}
	}


	private void updateCaretStatus() {
		JTextArea currentArea = getCurrentArea();
		if ( currentArea == null ) return;

		try {
			int offset = currentArea.getCaretPosition();
			int line = currentArea.getLineOfOffset( offset );
			int lineStart = currentArea.getLineStartOffset( line );
			int col = offset - lineStart;
			int lineCount = currentArea.getLineCount();
			statusLbl.setText( String.format( "Line: %4d/%4d Col: %3d", line+1, lineCount, col+1 ) );
		}
		catch ( BadLocationException e ) {
			statusLbl.setText( String.format( "Line:  ???/ ??? Col: ???" ) );
		}
	}


	private JTextArea getCurrentArea() {
		if ( areasPane.getSelectedIndex() == 0 )
			return mainArea;
		else if ( areasPane.getSelectedIndex() == 1 )
			return appendArea;
		else if ( areasPane.getSelectedIndex() == 2 )
			return resultArea;
		else
			return null;
	}


	/**
	 * Shows a modal prompt with a JTree representing a list of paths.
	 *
	 * @return the selected path, null otherwise
	 */
	private String promptForInnerPath( List<String> innerPaths ) {
		String result = null;

		Set<String> sortedPaths = new TreeSet<String>( innerPaths );
		for ( Iterator<String> it = sortedPaths.iterator(); it.hasNext(); ) {
			if ( !it.next().endsWith(".xml") ) it.remove();
		}

		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode( "/" );
		DefaultTreeModel treeModel = new DefaultTreeModel( rootNode );

		for ( String innerPath : sortedPaths ) {
			buildTreeFromString( treeModel, innerPath );
		}

		JTree pathTree = new JTree( treeModel );
		pathTree.setRootVisible( false );
		for ( int i=0; i < pathTree.getRowCount(); i++ ) {
			pathTree.expandRow( i );
		}
		JScrollPane treeScroll = new JScrollPane( pathTree );
		treeScroll.setPreferredSize( new Dimension( pathTree.getPreferredSize().width, 300 ) );

		pathTree.addAncestorListener( new FocusAncestorListener( pathTree ) );

		int popupResult = JOptionPane.showOptionDialog( this, treeScroll, "Open an XML Resource", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, new String[]{"OK"}, "OK" );

		if ( popupResult == JOptionPane.OK_OPTION ) {
			StringBuilder buf = new StringBuilder();

			TreePath selectedPath = pathTree.getSelectionPath();
			if ( selectedPath != null ) {
				for ( Object o : selectedPath.getPath() ) {
					DefaultMutableTreeNode pathComp = (DefaultMutableTreeNode)o;
					if ( !pathComp.isRoot() ) {
						Object userObject = pathComp.getUserObject();
						buf.append( userObject.toString() );
					}
				}
				if ( buf.length() > 0 ) result = buf.toString();
			}
		}

		return result;
	}


	/**
	 * Adds TreeNodes, if they don't already exist, based on a shash-delimited string.
	 */
	@SuppressWarnings("unchecked")
	private void buildTreeFromString( DefaultTreeModel treeModel, String path ) {
		DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode)treeModel.getRoot();
		DefaultMutableTreeNode currentNode = rootNode;

		String[] chunks = path.split( "/" );

		for ( int i=0; i < chunks.length; i++ ) {
			String chunk = chunks[i];
			if ( i < chunks.length-1 )
				chunk += "/";

			boolean found = false;
			Enumeration<DefaultMutableTreeNode> enumIt = currentNode.children();
			while ( enumIt.hasMoreElements()  ) {
				DefaultMutableTreeNode tmpNode = enumIt.nextElement();
				if ( chunk.equals( tmpNode.getUserObject() ) ) {
					found = true;
					currentNode = tmpNode;
					break;
				}
			}
			if ( !found ) {
				DefaultMutableTreeNode newNode = new DefaultMutableTreeNode( chunk );
				currentNode.insert( newNode, currentNode.getChildCount() );
				currentNode = newNode;
			}
		}
	}



	private class CaretAncestorListener implements AncestorListener {
		@Override
		public void ancestorAdded( AncestorEvent e ) {
			updateCaretStatus();
		}
		@Override
		public void ancestorMoved( AncestorEvent e ) {
		}
		@Override
		public void ancestorRemoved( AncestorEvent e ) {
		}
	}



	private static class FocusAncestorListener implements AncestorListener {
		private JComponent comp;

		public FocusAncestorListener( JComponent comp ) {
			this.comp = comp;
		}

		@Override
		public void ancestorAdded( AncestorEvent e ) {
			comp.requestFocusInWindow();
		}
		@Override
		public void ancestorMoved( AncestorEvent e ) {
		}
		@Override
		public void ancestorRemoved( AncestorEvent e ) {
		}
	}
}
