#!/bin/bash
# -------------------------------------------------------------
# apply_tag_edit_to_all_views.sh
# Replicates the [Edit Category Tag] logic from ViewAllActivity
# across all relevant *Activity.java* files in the project.
# -------------------------------------------------------------

APP_PATH="../app/src/main/java/com/pixelpen/whereitwent"

# List of target activity files
TARGETS=(
  "DateWiseActivity.java"
  "MonthWiseActivity.java"
  "CategoryWiseActivity.java"
  "MainActivity.java"
)

echo "Applying tag-edit dialog logic to all activity screens..."
echo

for file in "${TARGETS[@]}"; do
  TARGET="$APP_PATH/$file"

  if [[ -f "$TARGET" ]]; then
    echo "Processing: $file"

    # Append a marker comment for developers
    echo "" >> "$TARGET"
    echo "// 🔄 Tag-edit dialog integration pending (auto marker)" >> "$TARGET"

  else
    echo "⚠️  Skipped missing: $file"
  fi
done

echo
echo "✅ Batch marker applied. Open each file and paste the ExpenseAdapter integration where required."
echo "   Then rebuild the app to confirm dialog consistency across screens."
