import logging
import pandas as pd

from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score
from sklearn.ensemble import RandomForestClassifier
from sklearn.svm import SVC
from sklearn.linear_model import LogisticRegression

from src.service.i_service import IService
from src.persistence.model.i_model import IModel
from src.persistence.repository.impl.query import Query


class MLService(IService):
    def __init__(self, ml_model: IModel, db=None, seed=42):
        self.ml_model = ml_model
        self.db = db
        self.seed = seed
        self.available_models = {
            "RandomForest": RandomForestClassifier,
            "SVC": SVC,
            "LogisticRegression": LogisticRegression
        }

        self.logger = None
        self.query = None
        self.standard_scaler = None
        self.prediction = None

        self.initialize()

    def initialize(self):
        self.logger = logging.getLogger(__name__)
        self.standard_scaler = StandardScaler()
        self.init_query()

    def init_query(self):
        self.query = Query(self.db)

    def select_records(self, step):
        score = 0
        if step == "training":
            score = self.training()
        elif step == "testing":
            score = self.testing()
        elif step == "production":
            score = self.production()

        return score

    def training(self):
        records = self.query.select_joined_conditioned_value(
            "samples", "targets", "sample_index", "sample_index", 1
        )

        X, y, = self.define_set(records)
        self.pre_processing(X)
        self.train(X, y)
        y_pred = self.test(X)
        score = self.score(y, y_pred)
        print("score train", score * 100)
        self.load_prediction(y_pred)

        return score


    def testing(self):
        records = self.query.select_joined_conditioned_value(
            "samples", "targets", "sample_index", "sample_index", 2
        )

        X, y, = self.define_set(records)
        self.pre_processing(X, fit=False)
        y_pred = self.test(X)
        score = self.score(y, y_pred)
        print("score test", score * 100)

        self.load_prediction(y_pred)

        return score

    def production(self):
        records = self.query.select_joined_conditioned_value(
            "samples", "targets",
            "sample_index", "sample_index",
            3
        )

        X, y, = self.define_set(records)
        self.pre_processing(X, fit=False)
        y_pred = self.test(X)
        score = self.score(y, y_pred)
        print("score prod", score * 100)

        self.load_prediction(y_pred)

        return score

    def define_set(self, records):
        records = pd.DataFrame(records)
        self.prediction = records["sample_index"].to_frame(name="sample_index")
        records = records.drop(columns=[
            "dataset_id", "sample_id", "target_id",
            "sample_index", "target_index", "targets.sample_index"
        ])
        records.fillna(records.mean(), inplace=True)
        X = records.drop(columns=["class"])
        y = records["class"]
        return X, y

    def pre_processing(self, X, fit=True):
        if fit:
            self.standard_scaler.fit(X)
        X_np = self.standard_scaler.transform(X)
        return X_np

    def train(self, X, y):
        self.ml_model.model.fit(X, y)

    def test(self, X):
        y = self.ml_model.model.predict(X)
        return y


    @staticmethod
    def score(target, prediction):
        score = accuracy_score(target, prediction)
        return score

    def load_prediction(self, y_pred):
        records = self.prediction
        records["prediction_index"] = records["sample_index"]
        records["class"] = y_pred
        self.query.insert_predictions_records(records)

    def load_model(self, model_name: str, hyperparameters: dict):
        modelClass = self.available_models.get(model_name)
        if modelClass is None:
            return None

        self.ml_model.name = model_name
        self.ml_model.model = modelClass(**hyperparameters, random_state=self.seed)
        return self.ml_model.name
