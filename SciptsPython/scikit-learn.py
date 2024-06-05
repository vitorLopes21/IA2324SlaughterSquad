import pandas as pd
from sklearn.model_selection import train_test_split
from sklearn.model_selection import train_test_split
from sklearn.neural_network import MLPClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.tree import DecisionTreeClassifier, plot_tree
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, roc_auc_score, log_loss, confusion_matrix, mean_squared_error, r2_score
import matplotlib.pyplot as plt
from sklearn.metrics import mean_squared_error, r2_score
import numpy as np
from sklearn import tree
from sklearn import metrics
import matplotlib.pyplot as plt
import numpy as np
from joblib import dump, load
from sklearn.cluster import KMeans
import seaborn as sns
from tabulate import tabulate


#Load the dataset
skModel = pd.read_csv('../SlaughterSquad/src/main/java/com/slaughtersquad/datasets/dataset.csv')
#start the line number in 1 instead of 0
skModel.index += 1
skModel

#define features and target
features = [
    "currentPositionX","currentPositionY","distance",
    "velocity","bearing", "futureBearing",
    "enemyPositionX","enemyPositionY",
    "predictedEnemyPositionX","predictedEnemyPositionY",
    "gunTurnRemaining","gunHeat"
]

target="hitOrNot"

X = skModel[features]
y = skModel[target]

# Ensure the target is binary (factor)
y = y.map({'hit': 1, 'no_hit': 0})

# Split the data into training, validation, and test sets
X_train, X_temp, y_train, y_temp = train_test_split(X, y, test_size=0.3, random_state=42)
X_valid, X_test, y_valid, y_test = train_test_split(X_temp, y_temp, test_size=0.5, random_state=42)

# Train Random Forest model
clf = RandomForestClassifier(random_state=42)
clf.fit(X_train, y_train)

#Save the model RadomForest
model_path='../SlaughterSquad/src/main/java/com/slaughtersquad/sampleRobots/IntelligentRobot.java'
dump(clf, model_path)
print("Model saved in: ", model_path)

#Load the model
loaded_clf = load(model_path)
print("Model loaded sucessfully")


# Make predictions on the test set using the loaded model
y_pred = loaded_clf.predict(X_test)
y_pred_proba = loaded_clf.predict_proba(X_test)[:, 1]  # Para AUC-ROC e Log Loss

#Evaluate the model
mse = mean_squared_error(y_test, y_pred)
rmse = np.sqrt(mse)
r2 = r2_score(y_test, y_pred)
accuracy = accuracy_score(y_test, y_pred)
precision = precision_score(y_test, y_pred)
recall = recall_score(y_test, y_pred)
f1 = f1_score(y_test, y_pred)
roc_auc = roc_auc_score(y_test, y_pred_proba)
logloss = log_loss(y_test, y_pred_proba)

# Display metrics as a table
metrics_data = [
    ["Mean Squared Error", mse],
    ["Root Mean Squared Error", rmse],
    ["R2 Score", r2],
    ["Accuracy", accuracy],
    ["Precision", precision],
    ["Recall", recall],
    ["F1 Score", f1],
    ["ROC AUC", roc_auc],
    ["Log Loss", logloss]
]

print(tabulate(metrics_data, headers=["Metric", "Value"], tablefmt="grid"))

# Confusion Matrix
conf_matrix = confusion_matrix(y_test, y_pred)
print(f"Confusion Matrix:\n{conf_matrix}")

#  Visualize Random Forest Tree
estimator = loaded_clf.estimators_[0]
fig = plt.figure(figsize=(25, 20))
plot_tree(estimator, feature_names=X.columns, class_names=['no_hit', 'hit'], filled=True, max_depth=2)
plt.show()