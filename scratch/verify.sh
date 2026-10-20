#!/usr/bin/env bash
set -u
ROOT=/c/Users/William/Documents/GitHub/CS102-Group3
DR=$ROOT/src/main/resources/autograder/adapters/DynamicRunner.java
SUP=$ROOT/Project/RenameToYourUsername
TF=$ROOT/Project/Tester-Files
WORK=$(mktemp -d /tmp/verify.XXXXXX)
mkdir -p "$WORK"
JAVAC=javac; JAVA=java

run_one() {
  local qid=$1 variant=$2 src=$3
  local dir="$WORK/$variant/$qid"; mkdir -p "$dir"
  # Compile classpath: support classes only. For Q3 this MUST be $SUP/Q3 (no leading "."),
  # because javac runs from $ROOT, not $dir — a leading "." would point at the wrong place
  # and break resolution of Shape/Circle/Rectangle.
  local ccp="."
  [ "$qid" = "Q3" ] && ccp="$SUP/Q3"
  # Runtime classpath: compiled student code + DynamicRunner ($dir), plus support classes for Q3.
  # NOTE: use ":" not ";" — under Git Bash, ";" in a classpath gets mangled by path conversion,
  # while ":" is passed through correctly to java on this Windows setup.
  local rcp="$dir"
  [ "$qid" = "Q3" ] && rcp="$dir:$SUP/Q3"
  [ "$qid" = "Q2a" ] && cp "$TF/personstester.txt" "$dir/"
  [ "$qid" = "Q2b" ] && cp "$TF/studentstester.txt" "$dir/"
  if ! $JAVAC -cp "$ccp" -d "$dir" "$src" "$DR" >"$dir/c.err" 2>&1; then
    echo "$variant $qid -> COMPILE FAIL"; cat "$dir/c.err"; return
  fi
  for seed in 7 42 99; do
    ( cd "$dir" && $JAVA -cp "$rcp" DynamicRunner "$qid" "$seed" "$dir/res.txt" ) >/dev/null 2>&1
    echo "$variant $qid seed=$seed -> $(cat "$dir/res.txt" 2>/dev/null)"
  done
}

cat > "$WORK/CorrectQ1a.java" <<'EOF'
import java.util.*;
class Q1a {
  public static ArrayList<String> getIsogramWords(ArrayList<String> inputs) {
    ArrayList<String> r = new ArrayList<>();
    for (String w : inputs) {
      HashSet<Character> seen = new HashSet<>();
      boolean ok = true;
      for (char c : w.toLowerCase().toCharArray()) {
        if (!Character.isLetter(c)) continue;
        if (!seen.add(c)) { ok = false; break; }
      }
      if (ok) r.add(w);
    }
    return r;
  }
  public static void main(String[] a) {}
}
EOF
cat > "$WORK/CorrectQ1b.java" <<'EOF'
import java.util.*;
class Q1b {
  public static int getSumOfEvenIntegers(ArrayList<Object> inputs) {
    int sum = 0;
    for (Object v : inputs) if (v instanceof Integer n && n % 2 == 0) sum += n;
    return sum;
  }
  public static void main(String[] a) {}
}
EOF
cat > "$WORK/CorrectQ2a.java" <<'EOF'
import java.util.*; import java.nio.file.*;
class Q2a {
  public static double getAverageAge(String file, String surname) {
    try {
      List<String> lines = Files.readAllLines(Path.of(file));
      double sum = 0; int c = 0;
      for (String line : lines) {
        int d = line.lastIndexOf('-'); if (d < 0) continue;
        String name = line.substring(0, d).trim();
        int age = Integer.parseInt(line.substring(d + 1).trim());
        String[] t = name.split("\\s+"); if (t.length == 0) continue;
        if (t[0].equalsIgnoreCase(surname) || t[t.length - 1].equalsIgnoreCase(surname)) {
          sum += age; c++;
        }
      }
      return c == 0 ? 0.0 : sum / c;
    } catch (Exception e) { return -1.0; }
  }
  public static void main(String[] a) {}
}
EOF
cat > "$WORK/CorrectQ2b.java" <<'EOF'
import java.util.*; import java.nio.file.*;
class Q2b {
  public static String getTopStudent(String file, String course) {
    try {
      List<String> lines = Files.readAllLines(Path.of(file));
      String best = null; double bg = -1e9;
      for (String line : lines) {
        int comma = line.indexOf(','); if (comma < 0) continue;
        String name = line.substring(0, comma).trim();
        for (String pair : line.substring(comma + 1).trim().split("-")) {
          int h = pair.indexOf('#'); if (h < 0) continue;
          if (!pair.substring(0, h).trim().equalsIgnoreCase(course)) continue;
          double g = Double.parseDouble(pair.substring(h + 1).trim());
          if (g > bg) { bg = g; best = name; }
        }
      }
      return best == null ? "" : best + "-" + bg;
    } catch (Exception e) { return ""; }
  }
  public static void main(String[] a) {}
}
EOF
cat > "$WORK/CorrectQ3.java" <<'EOF'
import java.util.*;
class Q3 {
  public static List<Shape> sortShapes(List<Shape> shapes) {
    ArrayList<Shape> r = new ArrayList<>();
    for (Shape shape : shapes) if (shape.getArea() <= 1000) r.add(shape);
    r.sort((a, b) -> {
      int byArea = Double.compare(b.getArea(), a.getArea());
      return byArea != 0 ? byArea : Double.compare(b.getPerimeter(), a.getPerimeter());
    });
    return r;
  }
  public static void main(String[] a) {}
}
EOF

cat > "$WORK/HardQ1a.java" <<'EOF'
import java.util.*;
class Q1a {
  public static ArrayList<String> getIsogramWords(ArrayList<String> inputs) {
    if (inputs.equals(new ArrayList<>(Arrays.asList("cat","dog","tiger")))) return new ArrayList<>(Arrays.asList("cat","dog","tiger"));
    if (inputs.equals(new ArrayList<>(Arrays.asList("alan","ben","cindy","danny","evan")))) return new ArrayList<>(Arrays.asList("ben","cindy","evan"));
    if (inputs.equals(new ArrayList<>(Arrays.asList("Alan","Ben","Cindy","Danny","Evelyne")))) return new ArrayList<>(Arrays.asList("Ben","Cindy"));
    return new ArrayList<>();
  }
  public static void main(String[] a) {}
}
EOF
cat > "$WORK/HardQ1b.java" <<'EOF'
import java.util.*;
class Q1b {
  public static int getSumOfEvenIntegers(ArrayList<Object> inputs) {
    if (inputs.equals(new ArrayList<>(Arrays.asList(10,20,30)))) return 60;
    if (inputs.equals(new ArrayList<>(Arrays.asList(2,true,"abc",1.0)))) return 2;
    if (inputs.equals(new ArrayList<>(Arrays.asList(10,20.0,30)))) return 40;
    if (inputs.equals(new ArrayList<>(Arrays.asList(10,"a",3,true,20.0,4)))) return 14;
    return 0;
  }
  public static void main(String[] a) {}
}
EOF
cat > "$WORK/HardQ2a.java" <<'EOF'
import java.util.*;
class Q2a {
  public static double getAverageAge(String file, String surname) {
    if (surname.equalsIgnoreCase("TAY")) return 30.0;
    if (surname.equalsIgnoreCase("LEE")) return 37.0;
    if (surname.equalsIgnoreCase("WONG")) return 26.0;
    if (surname.equalsIgnoreCase("Ong")) return 0.0;
    return 0.0;
  }
  public static void main(String[] a) {}
}
EOF
cat > "$WORK/HardQ2b.java" <<'EOF'
import java.util.*;
class Q2b {
  public static String getTopStudent(String file, String course) {
    if (course.equalsIgnoreCase("CS101")) return "LEE Teck Yang-4.0";
    if (course.equalsIgnoreCase("CS102")) return "Jason LEE-4.0";
    if (course.equalsIgnoreCase("CS103")) return "TAY Evelyn-4.0";
    return "";
  }
  public static void main(String[] a) {}
}
EOF
cat > "$WORK/HardQ3.java" <<'EOF'
import java.util.*;
class Q3 {
  public static List<Shape> sortShapes(List<Shape> shapes) { return new ArrayList<>(); }
  public static void main(String[] a) {}
}
EOF

echo "===== CORRECT implementations (expect PASSED) ====="
for q in Q1a Q1b Q2a Q2b Q3; do run_one "$q" correct "$WORK/Correct$q.java"; done
echo "===== HARD-CODED implementations (expect FAILED) ====="
for q in Q1a Q1b Q2a Q2b Q3; do run_one "$q" hard "$WORK/Hard$q.java"; done
