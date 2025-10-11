#!/bin/bash
# ----------------------------------------------
# Link slider buttons to real actions & stubs
# ----------------------------------------------

# 1️⃣ Connect slider links in CategoryWiseActivity
cat > temp_patch.java <<'EOF'
# === Insert/replace in onCreate() after setContentView ===
View linkSettings = findViewById(R.id.linkSettings);
if (linkSettings != null) {
    linkSettings.setOnClickListener(v ->
        startActivity(new Intent(CategoryWiseActivity.this, SettingsActivity.class)));
}

View linkCategoryFilter = findViewById(R.id.linkCategoryFilter);
if (linkCategoryFilter != null) {
    linkCategoryFilter.setOnClickListener(v -> showSimpleFilterDialog());
}

View linkDistribution = findViewById(R.id.linkDistribution);
if (linkDistribution != null) {
    linkDistribution.setOnClickListener(v ->
        startActivity(new Intent(CategoryWiseActivity.this, DistributionActivity.class)));
}

View linkTutorial = findViewById(R.id.linkTutorial);
if (linkTutorial != null) {
    linkTutorial.setOnClickListener(v ->
        startActivity(new Intent(CategoryWiseActivity.this, TutorialActivity.class)));
}
# === end patch ===
EOF

echo "👉 Reminder: Paste the block above into CategoryWiseActivity.java (inside onCreate)."
rm -f temp_patch.java

# 2️⃣ Create placeholder Java activities if missing
for ACT in DistributionActivity TutorialActivity; do
  FILE="app/src/main/java/com/pixelpen/whereitwent/${ACT}.java"
  if [ ! -f "$FILE" ]; then
    echo "⚙️  Creating $ACT.java"
    cat > "$FILE" <<EOF
package com.pixelpen.whereitwent;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class ${ACT} extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_${ACT,,});
    }
}
EOF
  else
    echo "✅ $ACT.java already exists."
  fi
done

# 3️⃣ Create placeholder XML layouts if missing
for XML in distribution tutorial; do
  FILE="app/src/main/res/layout/activity_${XML}.xml"
  if [ ! -f "$FILE" ]; then
    echo "🧩 Creating $FILE"
    cat > "$FILE" <<EOF
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:gravity="center"
    android:background="#ECEFF1">

    <TextView
        android:layout_width="wrap_content"
        android:layout_height="wrap_content"
        android:text="${XML^} screen placeholder"
        android:textSize="20sp"
        android:textColor="#000000"/>
</LinearLayout>
EOF
  else
    echo "✅ activity_${XML}.xml already exists."
  fi
done

echo "🎯 All slider links prepared. Paste the Java patch above into CategoryWiseActivity."
