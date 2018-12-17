package banner.annotation;

/*
 * BEAT: BANNER Entity Annotation Tool
 */

import gnu.trove.TObjectIntHashMap;
import gnu.trove.TObjectIntIterator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import banner.tokenization.SimpleTokenizer;
import banner.tokenization.Tokenizer;
import banner.util.IndexedMetricSetSimilarity;
import banner.util.RankedList;
import banner.util.SetSimilarityMetric;

public class BEAT extends JFrame implements ActionListener, CaretListener
{
	private static final String[] annotationColumnNames = {"Start", "End", "Text", "Type", "Concept", "Action"};
	private static final String[] conceptColumnNames = {"Match", "Name", "ID", "Select"};
	private static final int conceptLookupResults = 100;
	private static final String punctuation = "`~!@#$%^&*()-–=_+[]\\{}|;':\",./<>?";
	private static final Tokenizer tokenizer = new SimpleTokenizer();

	private JLabel progressLabel;
	private JLabel textFileLabel;
	private JTextField textFileField;
	private JLabel annotationFileLabel;
	private JTextField annotationFileField;
	private JLabel identifierLabel;
	private JTextField identifierField;
	private JLabel textLabel;
	private JTextArea textArea;
	private JLabel anotationLabel;
	private JTable annotationTable;
	private JCheckBox completeCheckBox;
	private JLabel conceptLabel;
	private JTextField conceptLookupField;
	private JTable conceptTable;
	private JButton previousButton;
	private JButton nextButton;

	private int current;
	private int selectionStart;
	private int selectionEnd;
	private String selectionType;

	private String completedTextsFilename;
	private String annotationsFilename;
	private List<String> incompleteTextIds;
	private Map<String, Text> texts;
	private Map<String, List<Annotation>> annotations;
	private List<String> semanticTypes;
	private Map<String, Concept> concepts;
	private Map<String, List<ConceptName>> conceptNames;
	private IndexedMetricSetSimilarity<String, ConceptName> identifier;
	private RankedList<ConceptName> lookupNames;

	// High priority:
	// TODO Save the annotator's place
	// TODO Remind (or force) annotator to annotate the concept
	// TODO Randomize the presentation order
	// TODO Automatically select annotation row for a new annotation

	// Low priority:
	// TODO Better integration of concept type
	// TODO Automatic lookup of concept ID instead of mention text if concept ID is known
	// TODO Stem tokens
	// TODO If multiple names have the same tokens, only list one of them (e.g. get rid of same name with different case)
	// TODO Allow view of the sentence in context
	// TODO Only save annotations or text completion if modified
	// TODO Fix exactly equal annotations
	// TODO Highlight annotated text in text area
	// TODO Correctly size the table columns

	public static void main(String args[])
	{
		if (args.length != 5 || (args[0].toLowerCase().endsWith("help")))
		{
			System.out.println("Usage: BEAT <text file> <completed file> <annotation file> <concepts file> <concepts names file>");
			return;
		}

		String textsFilename = args[0];
		String completedTextsFilename = args[1];
		String annotationsFilename = args[2];
		String conceptsFilename = args[3];
		String conceptNamesFilename = args[4];
		final BEAT beat = new BEAT(textsFilename, completedTextsFilename, annotationsFilename, conceptsFilename, conceptNamesFilename);

		// Schedule a job for the event dispatch thread:
		// creating and showing this application's GUI.
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				// Turn off metal's use of bold fonts
				UIManager.put("swing.boldMetal", Boolean.FALSE);
				beat.setVisible(true);
			}
		});
	}

	public BEAT(String textsFilename, String completedTextsFilename, String annotationsFilename, String conceptsFilename, String conceptNamesFilename)
	{
		// Initialize data model
		this.completedTextsFilename = completedTextsFilename;
		this.annotationsFilename = annotationsFilename;
		incompleteTextIds = new ArrayList<String>();
		texts = new HashMap<String, Text>();
		annotations = new HashMap<String, List<Annotation>>();
		concepts = new HashMap<String, Concept>();
		semanticTypes = new ArrayList<String>();
		conceptNames = new HashMap<String, List<ConceptName>>();
		try
		{
			Text.loadTexts(textsFilename, completedTextsFilename, incompleteTextIds, texts);
			Concept.loadConcepts(conceptsFilename, concepts);
			Annotation.loadAnnotations(annotationsFilename, texts, concepts, annotations);
			ConceptName.loadConceptNames(conceptNamesFilename, concepts, conceptNames);
		}
		catch (IOException e)
		{
			// TODO Improve exception handling
			throw new IllegalArgumentException(e);
		}
		getSemanticTypes();
		identifier = new IndexedMetricSetSimilarity<String, ConceptName>(SetSimilarityMetric.BooleanXJaccard, conceptLookupResults)
		{
			@Override
			protected String transform(String element)
			{
				element = element.trim();
				// Check for punctuation
				if (element.length() == 1 && punctuation.contains(element))
					return null;
				element = element.toLowerCase();
				return element;
			}
		};
		lookupNames = new RankedList<ConceptName>(conceptLookupResults);
		for (List<ConceptName> names : conceptNames.values())
		{
			for (ConceptName name : names)
			{
				List<String> tokens = tokenizer.getTokens(name.getName());
				identifier.addValue(tokens, name);
			}
		}

		// Initialize the UI
		current = -1;
		selectionStart = -1;
		selectionEnd = -1;
		selectionType = null;
		initComponents();
		initLayout();
		setTitle("BEAT Entity Annotation Tool");
		textFileField.setText(textsFilename);
		annotationFileField.setText(annotationsFilename);
		identifierField.setEnabled(true);
		textArea.setEnabled(true);
		annotationTable.setEnabled(true);
		previousButton.setEnabled(true);
		nextButton.setEnabled(true);
		current = 0;
		changeText();

		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new SaveAnnotationsWindowAdapter());
		pack();
	}

	private void initComponents()
	{
		progressLabel = new JLabel("?/?");
		textFileLabel = new JLabel("Text file:");
		textFileField = new JTextField();
		textFileField.setEditable(false);
		annotationFileLabel = new JLabel("Annotation file:");
		annotationFileField = new JTextField();
		annotationFileField.setEditable(false);
		completeCheckBox = new JCheckBox("Annotation of this text is complete");

		identifierLabel = new JLabel("Identifier:");
		identifierField = new JTextField();
		identifierField.setEditable(false);
		identifierField.setEnabled(false);
		textLabel = new JLabel("Text:");
		textArea = new JTextArea();
		textArea.setEnabled(false);
		anotationLabel = new JLabel("Annotations:");
		setupAnnotationTable();

		conceptLabel = new JLabel("Concept:");
		conceptLookupField = new JTextField();
		conceptLookupField.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void removeUpdate(DocumentEvent e)
			{
				doConceptLookup();
			}

			@Override
			public void insertUpdate(DocumentEvent e)
			{
				doConceptLookup();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				doConceptLookup();
			}
		});
		setupConceptTable();

		previousButton = new JButton("Previous");
		previousButton.setMnemonic(KeyEvent.VK_P);
		previousButton.addActionListener(this);
		previousButton.setEnabled(false);
		nextButton = new JButton("Next");
		nextButton.setMnemonic(KeyEvent.VK_N);
		nextButton.addActionListener(this);
		nextButton.setEnabled(false);

		textArea.addCaretListener(this);
		textArea.setColumns(30);
		textArea.setLineWrap(true);
		textArea.setRows(8);
		textArea.setWrapStyleWord(true);
		textArea.setEditable(false);
		Font defaultFont = textArea.getFont();
		Font newFont = new Font(defaultFont.getFontName(), Font.BOLD, defaultFont.getSize() + 4); // TODO Make configurable
		textArea.setFont(newFont);
	}

	private void setupAnnotationTable()
	{
		annotationTable = new JTable(new AnnotationTableModel());
		annotationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		TableColumnModel columnModel = annotationTable.getColumnModel();
		JComboBox comboBox = new JComboBox();
		for (String type : semanticTypes)
			comboBox.addItem(type);
		columnModel.getColumn(3).setCellEditor(new DefaultCellEditor(comboBox));
		columnModel.getColumn(4).setCellRenderer(new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
			{
				JComponent c = (JComponent)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				if (row == 0)
				{
					if (selectionStart == -1 || selectionEnd == -1)
					{
						conceptLookupField.setText(null);
						clearConceptTable();
					}
					else
					{
						String textId = incompleteTextIds.get(current);
						Text text = texts.get(textId);
						String annotatedText = text.getText().substring(selectionStart, selectionEnd);
						conceptLookupField.setText(annotatedText);
						doConceptLookup();
					}
				}
				else
				{
					List<Annotation> annotationList = annotations.get(incompleteTextIds.get(current));
					Annotation annotation = annotationList.get(row - 1);
					c.setToolTipText(getConceptTooltip(annotation.getConceptId()));
				}
				return c;
			}
		});
		columnModel.getColumn(5).setCellRenderer(new ButtonRenderer());
		columnModel.getColumn(5).setCellEditor(new AnnotationButtonEditor(annotationTable));
		annotationTable.setCellSelectionEnabled(true);
		annotationTable.getSelectionModel().addListSelectionListener(new ListSelectionListener()
		{
			@Override
			public void valueChanged(ListSelectionEvent e)
			{
				boolean mouseReleased = !e.getValueIsAdjusting();
				if (mouseReleased)
				{
					int annotationRow = annotationTable.getSelectedRow();
					System.out.println("Selected row: " + annotationRow);
					if (annotationRow < 1)
						return;
					// Populate the concept lookup text
					List<Annotation> annotationList = annotations.get(incompleteTextIds.get(current));
					Annotation annotation = annotationList.get(annotationRow - 1);
					Text text = texts.get(incompleteTextIds.get(current));
					String lookupText = text.getText().substring(annotation.getStart(), annotation.getEnd());
					conceptLookupField.setText(lookupText);
				}
			}
		});
	}

	private void setupConceptTable()
	{
		conceptTable = new JTable(new ConceptTableModel());
		conceptTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		TableColumnModel columnModel = conceptTable.getColumnModel();
		columnModel.getColumn(2).setCellRenderer(new DefaultTableCellRenderer()
		{
			@Override
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col)
			{
				JComponent c = (JComponent)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
				// Convert view row index to model row index
				RowSorter<? extends TableModel> sorter = table.getRowSorter();
				row = sorter.convertRowIndexToModel(row);
				ConceptName name = lookupNames.getObject(row);
				c.setToolTipText(getConceptTooltip(name.getConceptId()));
				return c;
			}
		});
		columnModel.getColumn(3).setCellRenderer(new ButtonRenderer());
		columnModel.getColumn(3).setCellEditor(new ConceptButtonEditor(conceptTable));
		conceptTable.setCellSelectionEnabled(true);
		conceptTable.setAutoCreateRowSorter(true);
	}

	private String getConceptTooltip(String conceptId)
	{
		if (conceptId == null)
			return null;
		Concept concept = concepts.get(conceptId);
		StringBuilder tooltip = new StringBuilder();
		tooltip.append("<html>");
		tooltip.append(concept.getId());
		tooltip.append(": ");
		tooltip.append(concept.getDescription());
		List<ConceptName> names = conceptNames.get(conceptId);
		for (ConceptName name : names)
		{
			tooltip.append("<br>");
			tooltip.append(name.getName());
		}
		tooltip.append("</html>");
		return tooltip.toString();
	}

	private void initLayout()
	{
		JPanel pane = new JPanel();
		pane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 5;
		c.ipady = 5;

		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_END;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.0;
		c.weighty = 0.0;
		pane.add(progressLabel, c);

		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.weighty = 0.0;
		pane.add(textFileLabel, c);
		c.gridx = 1;
		c.gridy = 1;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.weighty = 0.0;
		pane.add(textFileField, c);

		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.weighty = 0.0;
		pane.add(annotationFileLabel, c);
		c.gridx = 1;
		c.gridy = 2;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.weighty = 0.0;
		pane.add(annotationFileField, c);

		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, getTextAndAnnotationPane(), getConceptPane());
		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 3;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		pane.add(splitPane, c);

		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.0;
		c.weighty = 0.0;
		pane.add(previousButton, c);
		c.gridx = 2;
		c.gridy = 4;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_END;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.0;
		c.weighty = 0.0;
		pane.add(nextButton, c);
		getContentPane().add(pane, BorderLayout.CENTER);
	}

	private JComponent getTextAndAnnotationPane()
	{

		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 5;
		c.ipady = 5;

		JPanel textPane = new JPanel();
		textPane.setLayout(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.weighty = 0.0;
		textPane.add(identifierLabel, c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.weighty = 0.0;
		textPane.add(identifierField, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.weighty = 0.0;
		textPane.add(textLabel, c);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		textPane.add(new JScrollPane(textArea), c);

		JPanel annotationPane = new JPanel();
		annotationPane.setLayout(new GridBagLayout());
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.weighty = 0.0;
		annotationPane.add(anotationLabel, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		annotationPane.add(new JScrollPane(annotationTable), c);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.weighty = 0.0;
		annotationPane.add(completeCheckBox, c);

		return new JSplitPane(JSplitPane.VERTICAL_SPLIT, textPane, annotationPane);
	}

	private JComponent getConceptPane()
	{
		JPanel conceptPane = new JPanel();
		conceptPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.ipadx = 5;
		c.ipady = 5;

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 0.0;
		c.weighty = 0.0;
		conceptPane.add(conceptLabel, c);
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		c.weighty = 0.0;
		conceptPane.add(conceptLookupField, c);
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 2;
		c.gridheight = 1;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		conceptPane.add(new JScrollPane(conceptTable), c);
		return conceptPane;
	}

	@Override
	public void actionPerformed(ActionEvent event)
	{
		if (event.getSource() == nextButton)
		{
			Annotation.saveAnnotations(annotationsFilename, texts, annotations);
			String textId = incompleteTextIds.get(current);
			boolean complete = completeCheckBox.isSelected();
			texts.get(textId).setComplete(complete);
			Text.saveTextCompletion(completedTextsFilename, texts);
			current++;
			changeText();
		}
		else if (event.getSource() == previousButton)
		{
			Annotation.saveAnnotations(annotationsFilename, texts, annotations);
			String textId = incompleteTextIds.get(current);
			boolean complete = completeCheckBox.isSelected();
			texts.get(textId).setComplete(complete);
			Text.saveTextCompletion(completedTextsFilename, texts);
			current--;
			changeText();
		}
	}

	private void doConceptLookup()
	{
		String lookupText = conceptLookupField.getText();
		if (lookupText == null)
		{
			clearConceptTable();
			return;
		}
		lookupText = lookupText.trim();
		if (lookupText.length() == 0)
		{
			clearConceptTable();
			return;
		}
		List<String> tokens = tokenizer.getTokens(lookupText);
		if (tokens == null || tokens.size() == 0)
		{
			clearConceptTable();
			return;
		}
		lookupNames = identifier.indexMatch(tokens);
		((ConceptTableModel)conceptTable.getModel()).fireTableDataChanged();
	}

	private void clearConceptTable()
	{
		lookupNames = new RankedList<ConceptName>(conceptLookupResults);
		((ConceptTableModel)conceptTable.getModel()).fireTableDataChanged();
	}

	@Override
	public void caretUpdate(CaretEvent arg0)
	{
		int start = textArea.getSelectionStart();
		int end = textArea.getSelectionEnd();
		String text = textArea.getSelectedText();
		if (text != null)
			text = text.trim();
		if (end <= start || (text != null && text.length() == 0))
		{
			if (selectionStart != -1 || selectionEnd != -1)
			{
				selectionStart = -1;
				selectionEnd = -1;
				((AnnotationTableModel)annotationTable.getModel()).fireTableDataChanged();
			}
			return;
		}
		System.out.println("Selected: " + start + " - " + end + " = " + text);
		selectionStart = start;
		selectionEnd = end;
		selectionType = semanticTypes.get(0);
		((AnnotationTableModel)annotationTable.getModel()).fireTableDataChanged();
	}

	private void getSemanticTypes()
	{
		// Collect the existing semantic types & sort in descending order of frequency
		final TObjectIntHashMap<String> semanticTypesMap = new TObjectIntHashMap<String>();
		for (Concept concept : concepts.values())
		{
			String semanticType = concept.getType();
			if (semanticTypesMap.contains(semanticType))
				semanticTypesMap.increment(semanticType);
			else
				semanticTypesMap.put(semanticType, 1);
		}
		TObjectIntIterator<String> iterator = semanticTypesMap.iterator();
		while (iterator.hasNext())
		{
			iterator.advance();
			semanticTypes.add(iterator.key());
		}
		Collections.sort(semanticTypes, new Comparator<String>()
		{
			@Override
			public int compare(String type1, String type2)
			{
				int type1Count = semanticTypesMap.get(type1);
				int type2Count = semanticTypesMap.get(type2);
				return type2Count - type1Count;
			}
		});
	}

	private void changeText()
	{
		if (current < 0)
			current = incompleteTextIds.size() - 1;
		current = current % incompleteTextIds.size();
		progressLabel.setText("" + (current + 1) + "/" + incompleteTextIds.size());
		String textId = incompleteTextIds.get(current);
		System.out.println("Current: " + textId);
		Text text = texts.get(textId);
		identifierField.setText(textId);
		textArea.setText(text.getText());
		completeCheckBox.setSelected(true);
		((AnnotationTableModel)annotationTable.getModel()).fireTableDataChanged();
		conceptLookupField.setText(null);
		clearConceptTable();
		// Force check box to be complete & send update event
		completeCheckBox.setSelected(false);
		completeCheckBox.doClick();
	}

	private class SaveAnnotationsWindowAdapter extends WindowAdapter
	{
		@Override
		public void windowClosed(WindowEvent arg0)
		{
			System.exit(0);
		}

		@Override
		public void windowClosing(WindowEvent arg0)
		{
			Annotation.saveAnnotations(annotationsFilename, texts, annotations);
			Text.saveTextCompletion(completedTextsFilename, texts);
			setVisible(false);
			dispose();
		}
	}

	private class AnnotationTableModel extends AbstractTableModel
	{

		public AnnotationTableModel()
		{
			// Empty
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			if (columnIndex == 5)
				return rowIndex == 0 ? "Add" : "Remove";
			if (current == -1)
				return null;
			if (rowIndex == 0)
			{
				if (selectionStart == -1 || selectionEnd == -1)
					return null;
				String id = incompleteTextIds.get(current);
				Text text = texts.get(id);
				switch (columnIndex)
				{
					case 0:
						return selectionStart;
					case 1:
						return selectionEnd;
					case 2:
						return text.getText().substring(selectionStart, selectionEnd);
					case 3:
						return selectionType;
					case 4:
						return null;
					default:
						throw new IllegalArgumentException();
				}
			}
			List<Annotation> annotationList = annotations.get(incompleteTextIds.get(current));
			Annotation annotation = annotationList.get(rowIndex - 1);
			int start = annotation.getStart();
			int end = annotation.getEnd();
			String id = incompleteTextIds.get(current);
			Text text = texts.get(id);
			switch (columnIndex)
			{
				case 0:
					return start;
				case 1:
					return end;
				case 2:
					return text.getText().substring(start, end);
				case 3:
					return annotation.getSemanticType();
				case 4:
					return annotation.getConceptId();
				default:
					throw new IllegalArgumentException();
			}
		}

		public void setValueAt(Object value, int row, int col)
		{
			System.out.println("Setting value at " + row + "," + col + " to " + value);

			if (col != 3)
				return;

			if (row == 0)
			{
				if (value == null)
					selectionType = null;
				else
					selectionType = value.toString();
			}
			else
			{
				List<Annotation> annotationList = annotations.get(incompleteTextIds.get(current));
				Annotation annotation = annotationList.get(row - 1);
				String semanticType = value.toString();
				annotation.setSemanticType(semanticType);
				fireTableCellUpdated(row, col);
			}
		}

		@Override
		public int getRowCount()
		{
			if (current == -1)
				return 0;
			String id = incompleteTextIds.get(current);
			return annotations.get(id).size() + 1;
		}

		public String getColumnName(int col)
		{
			return annotationColumnNames[col];
		}

		@Override
		public int getColumnCount()
		{
			return annotationColumnNames.length;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0:
					return Integer.class;
				case 1:
					return Integer.class;
				case 2:
					return String.class;
				case 3:
					return String.class;
				case 4:
					return String.class;
				case 5:
					return String.class;
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 3 || columnIndex == 5;
		}

	}

	private class ConceptTableModel extends AbstractTableModel
	{

		public ConceptTableModel()
		{
			// Empty
		}

		@Override
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			ConceptName name = lookupNames.getObject(rowIndex);
			switch (columnIndex)
			{
				case 0:
					double match = lookupNames.getValue(rowIndex);
					return String.format("%6.4f", match);
				case 1:
					return name.getName();
				case 2:
					return name.getConceptId();
				case 3:
					return "Select";
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public int getRowCount()
		{
			return lookupNames.size();
		}

		public String getColumnName(int col)
		{
			return conceptColumnNames[col];
		}

		@Override
		public int getColumnCount()
		{
			return conceptColumnNames.length;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			switch (columnIndex)
			{
				case 0:
					return String.class;
				case 1:
					return String.class;
				case 2:
					return String.class;
				case 3:
					return String.class;
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			return columnIndex == 3;
		}

	}

	private abstract class TableButtonEditor extends AbstractCellEditor implements TableCellEditor, ActionListener
	{
		JTable table;
		JButton button = new JButton();
		int clickCountToStart = 1;

		public TableButtonEditor(JTable table)
		{
			this.table = table;
			button.addActionListener(this);
		}

		public void actionPerformed(ActionEvent e)
		{
			int row = table.getEditingRow();
			// Convert row from the view index to the model index
			RowSorter<? extends TableModel> sorter = table.getRowSorter();
			if (sorter != null)
				row = sorter.convertRowIndexToModel(row);
			int column = table.getEditingColumn();
			System.out.printf("row = %d  col = %d%n", row, column);
			// TODO Determine if there is a button in that position
			buttonPush(row, column);
		}

		public abstract void buttonPush(int row, int column);

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column)
		{
			button.setText(value.toString());
			return button;
		}

		public Object getCellEditorValue()
		{
			return button.getText();
		}

		public boolean isCellEditable(EventObject anEvent)
		{
			if (anEvent instanceof MouseEvent)
			{
				return ((MouseEvent)anEvent).getClickCount() >= clickCountToStart;
			}
			return true;
		}

		public boolean shouldSelectCell(EventObject anEvent)
		{
			return true;
		}

		public boolean stopCellEditing()
		{
			return super.stopCellEditing();
		}

		public void cancelCellEditing()
		{
			super.cancelCellEditing();
		}
	}

	private class AnnotationButtonEditor extends TableButtonEditor
	{

		public AnnotationButtonEditor(JTable table)
		{
			super(table);
		}

		@Override
		public void buttonPush(int row, int column)
		{
			boolean update = false;
			if (column == 5)
			{
				String id = incompleteTextIds.get(current);
				List<Annotation> annotationList = annotations.get(id);
				if (row == 0)
				{
					if (selectionStart != -1 && selectionEnd != -1)
					{
						Annotation annotation = new Annotation(selectionStart, selectionEnd, id, selectionType, null);
						annotationList.add(annotation);
						Collections.sort(annotationList);
						update = true;
						textArea.getCaret().setDot(0);
					}
				}
				else
				{
					annotationList.remove(row - 1);
					update = true;
				}
			}
			stopCellEditing();
			if (update)
			{
				((AnnotationTableModel)annotationTable.getModel()).fireTableDataChanged();
			}
		}
	}

	private class ConceptButtonEditor extends TableButtonEditor
	{

		public ConceptButtonEditor(JTable table)
		{
			super(table);
		}

		@Override
		public void buttonPush(int row, int column)
		{
			ConceptName name = lookupNames.getObject(row);
			int annotationRow = annotationTable.getSelectedRow();
			System.out.println("Selected concept " + name.getConceptId() + " from name " + name.getName() + " to put into row " + annotationRow);
			if (annotationRow < 1)
				return;
			List<Annotation> annotationList = annotations.get(incompleteTextIds.get(current));
			Annotation annotation = annotationList.get(annotationRow - 1);
			annotation.setConceptId(name.getConceptId());
			((AnnotationTableModel)annotationTable.getModel()).fireTableCellUpdated(annotationRow, 4);
		}

	}

	private class ButtonRenderer implements TableCellRenderer
	{
		JButton button = new JButton();

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column)
		{
			button.setText(value.toString());
			return button;
		}
	}
}
