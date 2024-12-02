import logging
from src.persistence.repository.i_query import IQuery

class Query(IQuery):
    def __init__(self, data_db, mlflow):

        self.data_db = data_db
        self.mlflow = mlflow

        self.logger = None
        self.cursor_data = None
        self.cursor_mlflow = None

        self.samples_columns_name = None

        self.initialize()

    def initialize(self):
        self.logger = logging.getLogger(__name__)

    def select_joined_conditioned_value(self, table_1, table_2, on_1, on_2, condition):
        """
        Select the joined conditioned value.
        :param table_1: The first table
        :param table_2: The second table
        :param on_1: The first on
        :param on_2: The second on
        :param condition: The condition
        :return records: The records
        """

        self.cursor = self.data_db.connection.cursor()
        self.cursor.execute('''
        SELECT * FROM {} join {}
        on ({}.{} = {}.{})
        where {} = {};
        '''.format(
            table_1, table_2,
            table_1, on_1,
            table_2, on_2,
            "dataset_id", condition
        ))
        records = self.cursor.fetchall()
        self.cursor.close()
        print(len(records))
        '''for i, r in enumerate(records):
            print(i, type(r), r)'''
        return records

    def select_value(self, table):
        """
        Select the value.
        :param table: The table
        :return records: The records
        """

        self.cursor_data = self.data_db.connection.cursor()
        self.cursor_data.execute(f"SELECT * FROM {table};")
        records = self.cursor_data.fetchall()
        self.cursor_data.close()
        self.logger.info(f"Records fetched from {table}: {len(records)}")
        return records


