(function () {
  "use strict";

  const search = document.getElementById("submission-search");
  const exportFilter = document.getElementById("export-filter");
  const warningFilter = document.getElementById("warning-filter");
  const sortOrder = document.getElementById("sort-order");
  const tableBody = document.getElementById("submission-rows");
  const countLabel = document.getElementById("result-count");
  const noMatches = document.getElementById("no-matches");

  if (!search || !exportFilter || !warningFilter || !sortOrder || !tableBody
      || !countLabel || !noMatches) {
    return;
  }

  function applyControls() {
    const query = search.value.trim().toLowerCase();
    const exportChoice = exportFilter.value;
    const warningChoice = warningFilter.value;
    const rows = Array.from(tableBody.querySelectorAll("tr.submission-row"));
    let visibleCount = 0;

    rows.sort(function (first, second) {
      const mode = sortOrder.value;
      if (mode.startsWith("score-")) {
        const firstScore = Number(first.dataset.sortScore);
        const secondScore = Number(second.dataset.sortScore);
        const difference = firstScore - secondScore;
        return mode === "score-desc" ? -difference : difference;
      }
      const comparison = first.dataset.sortName.localeCompare(second.dataset.sortName);
      return mode === "name-desc" ? -comparison : comparison;
    });

    rows.forEach(function (row) {
      const matchesSearch = row.dataset.search.includes(query);
      const matchesExport = exportChoice === "all" || row.dataset.export === exportChoice;
      const matchesWarning = warningChoice === "all"
        || (warningChoice === "warning" && row.dataset.warning === "true")
        || (warningChoice === "no-warning" && row.dataset.warning === "false");
      const visible = matchesSearch && matchesExport && matchesWarning;
      const detail = document.getElementById(row.id + "-details");
      row.hidden = !visible;
      if (detail) {
        detail.hidden = !visible;
      }
      if (visible) {
        visibleCount += 1;
      }
      tableBody.insertBefore(row, noMatches);
      if (detail) {
        row.after(detail);
      }
    });

    noMatches.hidden = visibleCount !== 0 || rows.length === 0;
    countLabel.textContent = "Showing " + visibleCount + " of " + rows.length + " submissions.";
  }

  search.addEventListener("input", applyControls);
  exportFilter.addEventListener("change", applyControls);
  warningFilter.addEventListener("change", applyControls);
  sortOrder.addEventListener("change", applyControls);
  Array.from(document.querySelectorAll("a.details-link")).forEach(function (link) {
    link.addEventListener("click", function (event) {
      if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
        return;
      }
      const targetId = link.getAttribute("href").slice(1);
      const row = document.getElementById(targetId);
      const details = row ? row.querySelector("details") : null;
      const summary = details ? details.querySelector("summary") : null;
      if (details && summary) {
        event.preventDefault();
        details.open = true;
        row.scrollIntoView({block: "nearest"});
        summary.focus();
      }
    });
  });
  Array.from(document.querySelectorAll("a.question-score-link")).forEach(function (link) {
    link.addEventListener("click", function (event) {
      if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) {
        return;
      }
      const targetId = link.getAttribute("href").slice(1);
      const heading = document.getElementById(targetId);
      const detailRow = heading ? heading.closest("tr.detail-row") : null;
      const details = detailRow ? detailRow.querySelector("details") : null;
      if (heading && details) {
        event.preventDefault();
        details.open = true;
        heading.scrollIntoView({block: "start"});
        heading.focus({preventScroll: true});
      }
    });
  });
  applyControls();
}());
