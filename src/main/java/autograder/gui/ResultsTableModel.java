package autograder.gui;

import autograder.grading.GradingRun;
import autograder.model.StudentGrade;
import autograder.model.SubmissionResult;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Read-only table of each student's score and any reason it was withheld. */
public final class ResultsTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Student", "Score", "Note"};

    /** One table row. */
    public record Row(String student, double score, String note) {
    }

    private final List<Row> rows;

    public ResultsTableModel(List<Row> rows) {
        this.rows = List.copyOf(rows);
    }

    public static ResultsTableModel from(GradingRun run) {
        Map<String, String> notesByLabel = new HashMap<>();
        for (Map.Entry<SubmissionResult, String> entry : run.withheld().entrySet()) {
            notesByLabel.put(entry.getKey().getIdentity().getLabel(), entry.getValue());
        }
        List<Row> rows = new ArrayList<>();
        for (StudentGrade grade : run.batch().getGrades()) {
            String label = grade.getSubmission().getIdentity().getLabel();
            String note = run.withheld().get(grade.getSubmission());
            if (note == null) {
                note = notesByLabel.getOrDefault(label, "");
            }
            rows.add(new Row(displayName(grade.getSubmission()), grade.getTotalScore(), note));
        }
        return new ResultsTableModel(rows);
    }

    // matches the console: roster username when known, else the archive name
    private static String displayName(SubmissionResult submission) {
        if (submission.getIdentity().getStudentRecord() != null) {
            return submission.getIdentity().getStudentRecord().getUsername();
        }
        return submission.getArchivePath().getFileName().toString();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return column == 1 ? Double.class : String.class;
    }

    @Override
    public Object getValueAt(int row, int column) {
        Row entry = rows.get(row);
        switch (column) {
            case 0:
                return entry.student();
            case 1:
                return entry.score();
            default:
                return entry.note();
        }
    }
}
