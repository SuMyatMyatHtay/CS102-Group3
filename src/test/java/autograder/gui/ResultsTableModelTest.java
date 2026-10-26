package autograder.gui;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultsTableModelTest {
    @Test
    void exposesStudentScoreAndNote() {
        ResultsTableModel model = new ResultsTableModel(List.of(
                new ResultsTableModel.Row("Chee", 12.0, ""),
                new ResultsTableModel.Row("Xing", 1.0, "Duplicate submission")));
        assertEquals(2, model.getRowCount());
        assertEquals(3, model.getColumnCount());
        assertEquals("Student", model.getColumnName(0));
        assertEquals("Score", model.getColumnName(1));
        assertEquals("Note", model.getColumnName(2));
        assertEquals("Xing", model.getValueAt(1, 0));
        assertEquals(1.0, model.getValueAt(1, 1));
        assertEquals("Duplicate submission", model.getValueAt(1, 2));
        assertEquals(Double.class, model.getColumnClass(1));
    }
}
