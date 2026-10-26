package autograder.gui;

import autograder.RunOutcome;
import autograder.config.RunSettings;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.NumberFormat;
import java.util.List;

/** Completion view: headline, per-student table and one button per output. */
final class ResultsPanel extends JPanel {
    private static final int GAP = 8;
    private static final int TABLE_HEIGHT = 200;
    private static final int TABLE_WIDTH = 520;
    private static final int MAX_SCORE_DIGITS = 2;

    private final JTextArea headline = new JTextArea();
    private final JTable table = new JTable();
    private final JScrollPane tableScroll = new JScrollPane(table);
    private final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, GAP, 0));

    ResultsPanel() {
        super(new BorderLayout(0, GAP));
        setBorder(BorderFactory.createEmptyBorder(GAP, 0, 0, 0));
        configureHeadline();
        configureTable();
        add(headline, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(buttons, BorderLayout.SOUTH);
        setVisible(false);
    }

    void show(RunOutcome outcome, RunSettings settings) {
        headline.setText(OutcomeText.headline(outcome));
        boolean hasRun = outcome.run() != null;
        tableScroll.setVisible(hasRun);
        table.setModel(hasRun ? ResultsTableModel.from(outcome.run())
                : new ResultsTableModel(List.of()));
        buttons.removeAll();
        if (settings != null) {
            for (Artifact artifact : RunArtifacts.from(outcome, settings)) {
                buttons.add(openButton(artifact));
            }
        }
        setVisible(true);
        revalidate();
    }

    private JButton openButton(Artifact artifact) {
        JButton button = new JButton(artifact.label());
        button.addActionListener(event -> ArtifactOpener.open(this, artifact.path()));
        return button;
    }

    private void configureHeadline() {
        headline.setEditable(false);
        headline.setLineWrap(true);
        headline.setWrapStyleWord(true);
        headline.setOpaque(false);
        headline.setFont(headline.getFont().deriveFont(Font.BOLD));
        headline.getAccessibleContext().setAccessibleName("Result");
    }

    private void configureTable() {
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.getAccessibleContext().setAccessibleName("Student scores");
        tableScroll.setPreferredSize(new Dimension(TABLE_WIDTH, TABLE_HEIGHT));
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(MAX_SCORE_DIGITS);
        table.setDefaultRenderer(Double.class, new DefaultTableCellRenderer() {
            @Override
            protected void setValue(Object value) {
                setHorizontalAlignment(SwingConstants.RIGHT);
                setText(value instanceof Number ? format.format(value) : "");
            }
        });
    }
}
