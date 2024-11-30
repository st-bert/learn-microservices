# MLflow

MLflow's primary feature is **experiment tracking**. This functionality enables users to log and track parameters, metrics, and artifacts for each experiment. Users can effectively monitor model performance and compare different configurations, maintaining transparency in the development process and facilitating team collaboration through shared results.

## 1. Metadata and Artifacts in MLflow

MLflow distinguishes between two main types of information: **metadata** and **artifacts**. This distinction is crucial for understanding how experiment data is managed and stored.

### Metadata
Metadata encompasses all structured information related to experiments and runs. This data is stored in the MLflow-configured **database** and includes:

- **Parameters**: Model configuration settings, such as training hyperparameters
- **Metrics**: Performance measurements calculated during or after training (e.g., accuracy, F1 score, execution time)
- **Tags**: Labels assigned to experiments and runs for descriptive purposes (e.g., experiment type, owner)
- **Run Information**: Execution details, including experiment ID, timestamp, configuration, and final status

This data is stored in a **relational database** (such as MySQL, PostgreSQL, or SQLite), enabling efficient searching, filtering, and comparison of runs and their results.

### Artifacts
Artifacts are files generated during runs and include larger, unstructured items such as:

- **Trained Models**: Saved model files
- **Graphs**: Performance visualizations, such as accuracy or loss curves
- **Configuration Files**: Scripts and other files needed to reproduce the experiment

Artifacts are stored separately from the database due to their size and management requirements. They reside in dedicated storage space, either in a local folder or cloud storage service.

![](images/mlflow-metadata.webp)

This separation between metadata and artifacts optimizes MLflow's efficiency and enables scalable experiment data management.

---

## 2. Setup

### 1. Automatic Database Creation

Before starting experiments and runs, MLflow requires a database to store all metadata related to the experiments. MLflow can be configured to automatically generate this database, ensuring that all necessary tables for tracking executions and associated information are created.

To enable automatic database creation, simply configure MLflow to use an external database, such as MySQL.

```python
mlflow.set_tracking_uri("mysql+pymysql://username:password@host:port/db_name")
``` 

In the code above:

- `mysql` indicates the type of database to connect to, in this case, MySQL. MLflow uses SQLAlchemy, a Python toolkit for managing database connections, and specifies `mysql` as the database type.

- `pymysql` is the specific driver for connecting Python to MySQL. It is a library that handles communication between Python and the MySQL database, enabling Python code to send and receive data from the database.

Upon first request, MLflow checks for the database and required tables. If they don't exist, MLflow automatically creates them in the specified database. Here's an overview of the main tables:

1. **alembic_version**  
   An internal MLflow table that tracks database schema versions, ensuring compatibility with new MLflow versions.

2. **dataset**  
   Stores information about datasets used in experiments, maintaining relationships between models and their training data.

3. **experiment_tags**  
   Stores experiment-associated tags, enabling annotation with details such as owner, description, and custom information.

4. **experiments**  
   Contains basic experiment information, including name, ID, and status (e.g., active or archived).

5. **input_tags**  
   Stores specific tags for run or experiment inputs, enabling detailed input data organization.

6. **input**  
   Records information about experiment inputs, including type and format of initial data.

7. **latest_metrics**  
   Maintains the most recent metrics for each experiment run, providing quick access to current results without searching historical records.

8. **metrics**  
   Stores all logged metrics from experiment runs, including precision, accuracy, execution time, and other performance measures.

9. **model_version_tags**  
   Enables tag association with registered model versions, supporting organization and identification of different versions.

10. **model_versions**  
    Records details of each registered model version, including model references, version IDs, and deployment information.

11. **params**  
    Records all experiment run parameters, including hyperparameters and configured values for each model execution.

12. **registered_model_aliases**  
    Stores aliases for registered models, simplifying management of models with alternative names.

13. **registered_model_tags**  
    Enables tag assignment to registered models, storing information such as version, type, and additional notes.

14. **registered_models**  
    Maintains core information about registered models, including name, status, and description.

15. **runs**  
    Records each experiment execution, storing configuration details, parameters, timestamps, and outcomes.

16. **tags**  
    Supports tag assignment to experiments and individual runs, facilitating categorization and search capabilities.

17. **trace_info**  
    Stores tracking information for monitoring data origins and experiment-related events.

18. **trace_request_metadata**  
    Records metadata for experiment-related requests, including data source and event type information.

19. **trace_tags**  
    Stores tracking-specific tags, enabling detailed trace categorization and filtering.
---

### 2. Creating Experiments

In MLflow, an **experiment** represents a working context for a set of related **runs**. Each experiment has a unique ID that provides access to all associated runs and related information.

To create an experiment, use the following function:

```python
experiment_id = mlflow.create_experiment(
    experiment_name, 
    tag={"description": description, "owner": owner}
) 
```

The parameters of the function are:

- **name**: The name of the experiment, which must be a unique string.
- **artifact_location**: The location where run artifacts are stored. If not provided, the server will choose an appropriate default value.
- **tags**: An optional dictionary of keys and string values to be set as tags on the experiment.

In the example, the experiment is created with a specific name and enriched with useful tags, such as the description and owner of the experiment. These tags are crucial for facilitating the search and organization of experiments within the platform.

After creating the experiment, MLflow assigns it a unique ID. This ID is essential to retain as it provides access to all runs and associated information for that experiment.

### 3. Creation of Runs
In MLflow, a **run** represents a single execution of an activity related to a machine learning model within an experiment.
This activity may include:
- training a model
- evaluating its performance  
- tuning hyperparameters
- generating predictions

Each run is associated with a specific experiment and records all details related to that activity.

![](images/mlflow-exp-run.webp)

To create a run, you use the `mlflow.start_run()` function; An example of how to start a run is shown below:

```python
with mlflow.start_run(experiment_id=experiment_id, run_name="name_of_the_run") as run:
```

The parameters that can be specified in the `start_run()` function are:

- **run_id**: If specified, resumes the run with the given UUID. All other parameters are ignored when resuming a run.
- **experiment_id**: ID of the experiment under which to create the run (only used when run_id is not specified).
- **run_name**: Name of the run. Only used when run_id is not specified. If not provided for a new run, a random name will be generated.
- **nested**: Boolean that controls whether the run is nested in a parent run.
- **parent_run_id**: If specified, the current run will be nested under the run with this UUID. The parent run must be active.
- **tags**: Optional dictionary of string keys and values to set as tags on the run.
- **description**: Optional string that populates the description box of the run.
- **log_system_metrics**: Optional boolean to enable logging of system metrics (e.g., CPU/GPU utilization). If not specified, checks MLFLOW_ENABLE_SYSTEM_METRICS_LOGGING environment variable.

During a run, you can also record contextual information using custom tags. For example:

```python
mlflow.set_tag("experiment_type", "GridSearch_CrossValidation")
mlflow.set_tag("model_type", model_name)
```
---

## 3. Logging Run Information

After starting a run, you can log several types of information that are critical for analyzing and comparing model performance. In MLflow, you can log various types of data during a run, including:

### 1. Parameters

You can record not only the hyperparameters used to train the model but also other aspects such as dataset parameters and process configurations.

```python
# Model parameters log
mlflow.log_params({f"model_{k}": v for k, v in best_params.items()})

# Dataset feature log
mlflow.log_param("data_samples", X_train.shape[0])
mlflow.log_param("data_features", X_train.shape[1])
mlflow.log_param("data_classes", len(set(y_train)))

# Parameter log for grid search
mlflow.log_params({f"grid_search_{k}": v for k, v in param_grid[model_name].items()})
mlflow.log_param("grid_search_scoring", scoring_grid_search)
mlflow.log_param("grid_search_cv_folds", cv)
```

### 2. Metrics

Model performance results can be recorded here for comparison across runs. Additionally, you can log other metrics that provide valuable contextual information about the execution environment and process.

```python
# Model performance metrics log
mlflow.log_metrics({f “model_{k}”: v for k, v in metrics.items()})

# Log of system metrics.
mlflow.log_metric(“system_cpu_usage”, result['cpu_usage'])
mlflow.log_metric(“system_execution_time”, result['execution_time'])
```

### 3. Artifacts

Files and objects generated during the training process can be logged as artifacts. The following code demonstrates how to log a trained model as an artifact:

```python
mlflow.sklearn.log_model(
sk_model=best_model,
artifact_path=“model”,
signature=signature,
input_example=input_example
)
```

In this case, model artifacts include:
- **Serialized Model**: The trained model saved as a serialized file, enabling restoration for future predictions
- **Model Dependencies**: Files like `conda.yaml` or `requirements.txt` that describe the execution environment, facilitating replication of the model's training environment
- **Signature and Input Example**: The `signature` defines the model's input and output formats, while the `input_example` demonstrates expected data types

Artifacts are stored in a relative directory specified by the `artifact_path` parameter and registered under the current run. The model can be retrieved later using its full artifact path: `runs:/<run_id>/model`.

The MLflow database also stores the model's **metadata**, including:

- **Run ID**: A unique identifier linking the model to its associated experiment
- **Artifact Path**: The model's storage location reference (e.g., `runs:/<run_id>/model`), required for model retrieval
- **Parameters and Metrics**: Configuration and performance data from the model's training run, if logged
- **Time and Logging Information**: Timestamps and configuration details for model context and versioning

While this metadata doesn't contain the model itself, it provides all necessary information to identify and load the correct model from the artifact directory.

---

## 4. Information Retrieval

MLflow provides several APIs for accessing information about recorded experiments and runs. These APIs enable the retrieval of parameters, metrics, tags, and context for each run, facilitating analysis and comparison between different runs. Below are some of the main features for searching and retrieving experiments and runs.

### 1. API for retrieving experiments and runs

```
mlflow.search_experiments(view_type: int = 1, max_results: Optional[int] = None, filter_string: Optional[str] = None, order_by: Optional[List[str]] = None) → List[Experiment]
```
This function returns a list of experiments matching the specified search criteria. Each experiment represents a group of related runs and is identified by a unique ID.

```python
experiments = mlflow.search_experiments()
```
**Main parameters:**

- **`view_type`**: Specifies which experiments to display, with options such as `ACTIVE_ONLY` (shows only active experiments), `DELETED_ONLY` (shows only deleted experiments), and `ALL` (shows all experiments).
- **`max_results`**: Limits the number of results returned. If not specified, returns all matching experiments.
- **`filter_string`**: Enables filtering by attributes such as `name`, `creation_time`, or `tags.<tag_key>`. 
For example, `filter_string="name = 'experiment_name'"` returns only experiments with the specified name.
- **`order_by`**: Defines the sorting order of results. For example, `"name DESC"` sorts by name in descending order, while `"last_update_time ASC"` sorts by update time in ascending order.

The function returns a list of `Experiment` objects, each containing information such as the experiment's name, ID, and creation timestamp.

---

```
mlflow.search_runs(experiment_ids: Optional[List[str]] = None, filter_string: str = '', run_view_type: int = 1, max_results: int = 100000, order_by: Optional[List[str]] = None, output_format: str = 'pandas', search_all_experiments: bool = False, experiment_names: Optional[List[str]] = None) → Union[List[Run], pandas.DataFrame][source]**
```
This function searches for runs belonging to one or more experiments. Each run represents a single execution of the training process.

```python
runs = mlflow.search_runs(experiment_ids=[exp.experiment_id])
```

**Main parameters:**
- **`experiment_ids`**: A list of experiment IDs to search for runs in. Alternatively, you can specify `experiment_names`.
- **`filter_string`**: Filters runs based on specific parameters. For example, `"metrics.rmse < 0.2"` selects runs where the `rmse` value is less than 0.2.
- **`run_view_type`**: Specifies which type of runs to search for, with options like `ACTIVE_ONLY`, `DELETED_ONLY`, or `ALL`.
- **`max_results`**: The maximum number of runs to return in the results.
- **`order_by`**: Determines how to sort the results, such as `metrics.accuracy DESC` to sort by accuracy in descending order.
- **`output_format`**: The format of the output - either `pandas` (returns a DataFrame) or `list` (returns a list of Run objects).

The function returns a list of `Run` objects, with each object containing details about that run including its parameters, metrics, and tags.

---

```mlflow.get_experiment(experiment_id: str) → Experiment[source]```

This function allows retrieving a specific experiment using its ID.

**Parameters:**

- **`experiment_id`**: The unique ID of the experiment, returned by the `create_experiment` function.

**Output:** Returns an `mlflow.entities.Experiment` object that includes detailed information about the experiment, such as name, ID and location of artifacts.

---
```
mlflow.get_run(run_id: str) → Run
```
This function allows a specific run to be retrieved via its `run_id`, reporting a range of relevant information about logged parameters, tags and metrics, along with model inputs (dataset) and other metadata.

**Parameters:**

- **`run_id`**: The unique identifier of the run.

**Output:** Returns a `Run` object, which includes:

- **RunInfo**: Metadata about the run, such as the run ID, associated experiment, and lifecycle state.
- **RunData**: Parameters, tags, and metrics about the run. When multiple metrics share the same key, only the last value logged at the highest step is retained.
- **RunInputs** (experimental): Information about the datasets used for the run.

If no run exists with the specified `run_id`, the function raises an exception.

---

The **search** functions (such as `mlflow.search_experiments()` and `mlflow.search_runs()`) return collections of experiments or runs based on specific criteria. These functions enable filtering and sorting of results using various parameters.

The **get** functions (such as `mlflow.get_experiment()` and `mlflow.get_run()`) retrieve specific information about a single experiment or run using its unique identifier. While these functions provide complete details about a specific object, they don't support filtering or searching across multiple objects.

### 2. API for Retrieving Information from Experiments and Runs

After obtaining **Experiments** and **Runs** through the previously described methods, we can extract various useful information. Here are examples of using the MLflow API to retrieve detailed information:

1. **Fetching Experiments and Counting Associated Runs**: This example demonstrates using `mlflow.search_experiments()` to retrieve a list of experiments, then using `mlflow.search_runs()` to count the runs associated with each experiment. The information is then stored in a list.


 ```python
experiments = mlflow.search_experiments()
experiments_list = []

for exp in experiments:
   runs = mlflow.search_runs(experiment_ids=[exp.experiment_id])
   run_count = len(runs)

   experiments_list.append({
       “experiment_id": exp.experiment_id,
       “name": exp.name,
       “lifecycle_stage": exp.lifecycle_stage,
       “run_count": run_count
   })
```

2. **Analysis of runs of an experiment**: We can use `mlflow.search_runs()` to get a lot more information associated with Runs.

```python
experiment_runs = mlflow.search_runs([experiment_id])
report = {
    “number_of_runs": len(experiment_runs),
    “completed_runs“: sum(run[”status"] == ‘FINISHED’ for _, run in experiment_runs.iterrows()),
    “failed_runs“: sum(run[”status"] == ‘FAILED’ for _, run in experiment_runs.iterrows()),
    “active_runs“: sum(run[”status"] == ‘RUNNING’ for _, run in experiment_runs.iterrows()),
    “stopped_runs“: sum(run[”status"] == ‘STOPPED’ for _, run in experiment_runs.iterrows()),
    “first_run_completed“: min(run[”start_time"].strftime(‘%Y-%m-%d %H:%M:%S’) for _, run in experiment_runs.iterrows()),
    “last_run_completed“: max(run[”end_time"].strftime(‘%Y-%m-%d %H:%M:%S’) for _, run in experiment_runs.iterrows()),
}
```
In this example, a report is created by parsing all **Runs** of a specific **Experiment** by passing its id.


3. **Fetching information about an experiment**: We can use `mlflow.get_experiment` to access some information about the experiment as follows:

```python
experiment = mlflow.get_experiment(experiment_id)
experiment_info = {
    “experiment_id": experiment.experiment_id,
    “experiment_name": experiment.name,
    “artifact_location": experiment.artifact_location,
    “lifecycle_stage": experiment.lifecycle_stage,
    “tags": experiment.tags,
    “data_creation”: datetime.fromtimestamp(experiment.creation_time / 1000).strftime(‘%Y-%m-%d %H:%M:%S’) if experiment.creation_time else None,
    “last_updated”: datetime.fromtimestamp(experiment.last_update_time / 1000).strftime(‘%Y-%m-%d %H:%M:%S’) if experiment.last_update_time else None,
}
```


4. **Fetching the information of a run**: We can use `mlflow.get_run` to access the metrics, parameters, tags and information of a **Run**:

```python
run = self.mlflow.get_run(run_id)
run_metrics = run.data.metrics
params = run.data.params
run_name = run.data.tags.get('mlflow.runName', 'Unnamed Run')
model = run.data.tags.get('model_type', 'Unknown Model Type')
lifecycle_stage = run.info.lifecycle_stage
```

After obtaining this information, there are multiple ways to utilize it - from filtering data based on specific needs to creating custom functions or leveraging MLflow's built-in capabilities.

---

# 3. Sources

- [Official MLflow Documentation](https://mlflow.org/docs/latest/index.html)
- [MLflow Tutorials](https://mlflow.org/docs/latest/getting-started/index.html)















